package kz.koshelek.app.ui

import kz.koshelek.app.data.AppSettings
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.koshelek.app.data.Transaction
import kz.koshelek.app.data.counts
import kz.koshelek.app.data.isIncome
import java.time.YearMonth

@Composable
fun ReportScreen(all: List<Transaction>) {
    val p = LocalPalette.current
    var period by remember { mutableIntStateOf(1) }
    val monthsCount = listOf(3, 6, 12)[period]
    val now = AppSettings.currentPeriod()
    val months = (monthsCount - 1 downTo 0).map { now.minusMonths(it.toLong()) }
    val counted = all.filter { it.counts }
    val incomes = months.map { m -> counted.inMonth(m).filter { it.isIncome }.sumOf { it.amount } }
    val expenses = months.map { m -> counted.inMonth(m).filter { !it.isIncome }.sumOf { it.amount } }
    val labels = months.mapIndexed { i, m ->
        if (monthsCount <= 6 || i % 2 == 0) m.format(monthShort).trimEnd('.') else ""
    }
    val totalIn = incomes.sum()
    val totalOut = expenses.sum()

    val thisMonth = counted.inMonth(now).filter { !it.isIncome }
    val byCat = thisMonth.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amount } }.toList().sortedByDescending { it.second }
    val monthTotal = byCat.sumOf { it.second }
    val prev = counted.inMonth(now.minusMonths(1)).filter { !it.isIncome }.sumOf { it.amount }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Отчёт", color = p.text, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 16.dp))
        Segmented(listOf("3 месяца", "6 месяцев", "Год"), period) { period = it }

        PCard {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Legend("Доходы", p.income)
                Legend("Расходы", p.expense)
            }
            Spacer(Modifier.height(10.dp))
            LineChart(listOf(incomes to p.income, expenses to p.expense), labels, p.divider, p.text2)
        }

        PCard {
            Row {
                Total("Доходы", moneyShort(totalIn), p.income, Modifier.weight(1f))
                Total("Расходы", moneyShort(totalOut), p.expense, Modifier.weight(1f))
                Total("Сэкономлено", moneyShort(totalIn - totalOut), p.text, Modifier.weight(1f))
            }
        }

        PCard {
            Text("Расходы за ${now.format(monthFormat)}", color = p.text, fontWeight = FontWeight.Bold)
            if (prev > 0) {
                val diff = ((monthTotal - prev) / prev * 100).toInt()
                Text(
                    if (diff >= 0) "+$diff% к прошлому месяцу" else "$diff% к прошлому месяцу",
                    color = if (diff > 0) p.expense else p.income, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Donut(byCat.map { it.second to p.colorOf(it.first) }, 130.dp, 26.dp, p.chip)
                Spacer(Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (byCat.isEmpty()) Text("Нет расходов", color = p.text2)
                    byCat.take(6).forEach { (cat, sum) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(p.colorOf(cat)))
                            Spacer(Modifier.width(8.dp))
                            Text(shortName(cat), color = p.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text("${if (monthTotal > 0) (sum / monthTotal * 100).toInt() else 0}%", color = p.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Legend(label: String, color: androidx.compose.ui.graphics.Color) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = p.text2, fontSize = 13.sp)
    }
}

@Composable
private fun Total(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    val p = LocalPalette.current
    Column(modifier) {
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = p.text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}
