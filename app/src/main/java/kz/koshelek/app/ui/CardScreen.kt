package kz.koshelek.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.koshelek.app.data.Balance
import kz.koshelek.app.data.Transaction
import kz.koshelek.app.data.counts
import kz.koshelek.app.data.isIncome
import java.time.YearMonth

/** Экран одной карты (или наличных): остаток, доход и расход за месяц, операции. */
@Composable
fun CardScreen(
    bank: String,
    balance: Balance?,
    all: List<Transaction>,
    onBack: () -> Unit,
    onOpen: (Transaction) -> Unit,
    onEditBalance: () -> Unit,
) {
    val p = LocalPalette.current
    var month by remember { mutableStateOf(YearMonth.now()) }
    val mine = all.filter { it.bank.startsWith(bank, ignoreCase = true) }
    val inMonth = mine.inMonth(month)
    val counted = inMonth.filter { it.counts }
    val income = counted.filter { it.isIncome }.sumOf { it.amount }
    val expense = counted.filter { !it.isIncome }.sumOf { it.amount }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(AppIcons.Back, "Назад", tint = p.text) }
                BankBadge(bank, 34.dp)
                Spacer(Modifier.width(10.dp))
                Text(bank, color = p.text, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(p.hero).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Остаток", color = p.heroText2, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(balance?.let { money(it.amount) } ?: "—", color = p.onHero, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    balance?.let { "обновлено " + formatTime(it.updatedAt, dateTimeFormat) }
                        ?: "Остаток ещё не приходил из уведомлений банка",
                    color = p.heroText2, fontSize = 13.sp,
                )
                TextButton(onClick = onEditBalance, contentPadding = PaddingValues(0.dp)) {
                    Text("Изменить остаток вручную", color = p.onHero, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { MonthSwitcher(month) { month = it } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PCard(Modifier.weight(1f), padding = 14.dp) {
                    Text("Доход", color = p.text2, fontSize = 13.sp)
                    Text("+" + money(income), color = p.income, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                }
                PCard(Modifier.weight(1f), padding = 14.dp) {
                    Text("Расход", color = p.text2, fontSize = 13.sp)
                    Text("−" + money(expense), color = p.expense, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                }
            }
        }
        item { SectionTitle("Операции по карте") }
        if (inMonth.isEmpty()) item {
            PCard { Text("В этом месяце операций по этой карте нет.", color = p.text2, fontSize = 14.sp) }
        }
        items(inMonth, key = { it.id }) { t ->
            PCard(padding = 12.dp) { TransactionRow(t) { onOpen(t) } }
        }
    }
}
