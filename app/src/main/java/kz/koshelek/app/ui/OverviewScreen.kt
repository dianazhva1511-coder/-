package kz.koshelek.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.koshelek.app.data.Transaction
import java.time.YearMonth

@Composable
fun OverviewScreen(all: List<Transaction>, modifier: Modifier, onOpenSettings: () -> Unit) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    val inMonth = all.filter { YearMonth.from(it.timestamp.toLocalDate()) == month }
    val income = inMonth.filter { it.type == "INCOME" }.sumOf { it.amount }
    val expenses = inMonth.filter { it.type == "EXPENSE" }
    val expense = expenses.sumOf { it.amount }
    val hasAccess = rememberNotificationAccess()

    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!hasAccess) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Автоучёт выключен", fontWeight = FontWeight.Bold)
                    Text("Разрешите приложению читать уведомления, чтобы операции Kaspi, Halyk, Forte и Samsung Pay добавлялись сами.")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onOpenSettings) { Text("Включить") }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { month = month.minusMonths(1) }) { Text("‹", fontSize = 24.sp) }
            Text(
                month.format(monthFormat).replaceFirstChar { it.titlecase(Ru) },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { month = month.plusMonths(1) }, enabled = month < YearMonth.now()) {
                Text("›", fontSize = 24.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("Поступления", income, IncomeColor, Modifier.weight(1f))
            SummaryCard("Расходы", expense, ExpenseColor, Modifier.weight(1f))
        }
        SummaryCard("Итог за месяц", income - expense, MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth())

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Расходы по дням", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                DailyChart(month, expenses)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Куда уходят деньги", style = MaterialTheme.typography.titleMedium)
                val byCat = expenses.groupBy { it.category }
                    .mapValues { (_, v) -> v.sumOf { it.amount } }
                    .toList().sortedByDescending { it.second }
                if (byCat.isEmpty()) Text("Пока нет расходов за этот месяц")
                byCat.forEachIndexed { i, (cat, sum) ->
                    val share = if (expense > 0) (sum / expense).toFloat() else 0f
                    val color = ChartColors[i % ChartColors.size]
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(color, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(cat, Modifier.weight(1f))
                            Text("${money(sum)} · ${(share * 100).toInt()}%")
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { share },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("По банкам", style = MaterialTheme.typography.titleMedium)
                val byBank = inMonth.groupBy { it.bank.substringBefore(" · ") }
                if (byBank.isEmpty()) Text("Нет операций")
                byBank.forEach { (bank, list) ->
                    val inc = list.filter { it.type == "INCOME" }.sumOf { it.amount }
                    val exp = list.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    Row {
                        Text(bank, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Text("+${money(inc)}", color = IncomeColor)
                        Spacer(Modifier.width(10.dp))
                        Text("−${money(exp)}", color = ExpenseColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: Double, color: Color, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(money(value), color = color, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DailyChart(month: YearMonth, expenses: List<Transaction>) {
    val days = month.lengthOfMonth()
    val perDay = DoubleArray(days)
    expenses.forEach { perDay[it.timestamp.toLocalDate().dayOfMonth - 1] += it.amount }
    val max = perDay.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val barColor = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant

    Column {
        Text("Максимум за день: ${money(perDay.maxOrNull() ?: 0.0)}", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val slot = size.width / days
            val barW = slot * 0.65f
            for (d in 0 until days) {
                val x = d * slot + (slot - barW) / 2
                drawRoundRect(track, Offset(x, 0f), Size(barW, size.height), CornerRadius(barW / 2))
                val h = (perDay[d] / max).toFloat() * size.height
                if (h > 0f) {
                    drawRoundRect(barColor, Offset(x, size.height - h), Size(barW, h), CornerRadius(barW / 2))
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text("1", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.weight(1f))
            Text("15", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.weight(1f))
            Text("$days", style = MaterialTheme.typography.bodySmall)
        }
    }
}
