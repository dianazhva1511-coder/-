@file:OptIn(ExperimentalLayoutApi::class)

package kz.koshelek.app.ui

import kz.koshelek.app.data.AppSettings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.koshelek.app.data.Balance
import kz.koshelek.app.data.CategoryLimit
import kz.koshelek.app.data.Transaction
import kz.koshelek.app.data.counts
import kz.koshelek.app.data.isIncome
import kz.koshelek.app.parser.Categories
import java.time.YearMonth

@Composable
fun CategoriesScreen(
    all: List<Transaction>,
    balances: List<Balance>,
    limits: List<CategoryLimit>,
    budget: Double,
    onEditLimit: (String, Double?) -> Unit,
    onEditBalance: (String, Double?) -> Unit,
    onEditBudget: () -> Unit,
) {
    val p = LocalPalette.current
    var month by remember { mutableStateOf(AppSettings.currentPeriod()) }
    val list = all.inMonth(month).filter { it.counts }
    val incomeBy = list.filter { it.isIncome }.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amount } }
    val expenseBy = list.filter { !it.isIncome }.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amount } }
    val limitMap = limits.associate { it.category to it.amount }
    val incomeCats = (AppSettings.incomeCategories().filter { it != Categories.OWN && it != Categories.OTHER } + incomeBy.keys).distinct()
    val expenseCats = (AppSettings.expenseCategories().filter { it != Categories.OWN } + expenseBy.keys).distinct()
    val banks = walletsOf(balances)
    val balanceMap = balances.associate { it.bank to it.amount }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        MonthSwitcher(month) { month = it }

        Header("Доходы", money(incomeBy.values.sum()), p.income)
        PCard(padding = 12.dp) {
            Grid {
                incomeCats.forEach { cat ->
                    Cell(shortName(cat), incomeBy[cat]?.let { money(it) } ?: "0 ₸", null) {
                        CircleIcon(iconOf(cat), p.colorOf(cat), 54.dp, cat)
                    }
                }
            }
        }

        Header("Кошельки", if (balances.isEmpty()) "—" else money(balances.sumOf { it.amount }), p.savings)
        PCard(padding = 12.dp) {
            Grid {
                banks.forEach { bank ->
                    Cell(bank, balanceMap[bank]?.let { money(it) } ?: "нет данных", null, onClick = { onEditBalance(bank, balanceMap[bank]) }, labelBelow = true) {
                        BankBadge(bank, 54.dp)
                    }
                }
            }
            Text(
                "Остатки обновляются из уведомлений банков («Доступно …»). Нажмите на карту, чтобы поправить вручную.",
                color = p.text2, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
            )
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.Bottom) {
            Text("Расходы", color = p.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onEditBudget).padding(4.dp)) {
                Text(if (budget > 0) "бюджет ${money(budget)}" else "задать бюджет", color = p.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(money(expenseBy.values.sum()), color = p.expense, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        PCard(padding = 12.dp) {
            Grid {
                expenseCats.forEach { cat ->
                    val spent = expenseBy[cat] ?: 0.0
                    val limit = limitMap[cat]
                    val share = limit?.let { (spent / it).toFloat() }
                    val note = when {
                        limit == null -> null
                        spent > limit -> "лимит превышен" to p.expense
                        share != null && share >= 0.8f -> "${(share * 100).toInt()}% лимита" to p.warning
                        else -> "из ${money(limit)}" to p.text2
                    }
                    Cell(shortName(cat), money(spent), note, onClick = { onEditLimit(cat, limit) }) {
                        RingIcon(iconOf(cat), p.colorOf(cat), share)
                    }
                }
            }
            Text(
                "Нажмите на категорию, чтобы задать лимит на месяц.",
                color = p.text2, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Header(title: String, value: String, color: androidx.compose.ui.graphics.Color) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, color = p.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        Text(value, color = color, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun Grid(content: @Composable () -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        maxItemsInEachRow = 4,
    ) { content() }
}

@Composable
private fun Cell(
    label: String,
    value: String,
    note: Pair<String, androidx.compose.ui.graphics.Color>?,
    onClick: (() -> Unit)? = null,
    labelBelow: Boolean = false,
    icon: @Composable () -> Unit,
) {
    val p = LocalPalette.current
    var m = Modifier.width(80.dp).clip(RoundedCornerShape(14.dp))
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (!labelBelow) Text(label, color = p.text2, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        icon()
        Text(value, color = p.text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
        if (labelBelow) Text(label, color = p.text2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        if (note != null) Text(note.first, color = note.second, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, textAlign = TextAlign.Center)
    }
}
