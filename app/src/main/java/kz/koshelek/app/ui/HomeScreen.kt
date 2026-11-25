package kz.koshelek.app.ui

import kz.koshelek.app.data.AppSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.koshelek.app.data.Balance
import kz.koshelek.app.data.Goal
import kz.koshelek.app.data.Transaction
import kz.koshelek.app.data.counts
import kz.koshelek.app.data.isIncome
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HomeScreen(
    all: List<Transaction>,
    balances: List<Balance>,
    goals: List<Goal>,
    budget: Double,
    onOpenOperations: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTransaction: (Transaction) -> Unit,
    onGoalClick: (Goal?) -> Unit,
    onSetBudget: () -> Unit,
    onOpenCard: (String) -> Unit,
) {
    val p = LocalPalette.current
    val hasAccess = rememberNotificationAccess()
    var hidden by remember { mutableStateOf(false) }
    var showCards by remember { mutableStateOf(false) }
    val month = AppSettings.currentPeriod()
    val inMonth = all.inMonth(month).filter { it.counts }
    val income = inMonth.filter { it.isIncome }.sumOf { it.amount }
    val expense = inMonth.filter { !it.isIncome }.sumOf { it.amount }
    val onCards = balances.sumOf { it.amount }
    fun m(v: Double) = if (hidden) "••• ₸" else money(v)

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(LocalDate.now().format(headerDate).uppercase(Ru), color = p.text2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Сводка", color = p.text, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            }
            IconButton(onClick = { hidden = !hidden }) { Icon(AppIcons.Eye, "Скрыть суммы", tint = p.text) }
            IconButton(onClick = onOpenSettings) { Icon(AppIcons.Gear, "Настройки", tint = p.text) }
        }

        if (!hasAccess) {
            PCard(onClick = onOpenSettings) {
                Text("Автоучёт выключен", color = p.expense, fontWeight = FontWeight.Bold)
                Text("Нажмите, чтобы разрешить чтение уведомлений банков.", color = p.text2, fontSize = 14.sp)
            }
        }

        // Баланс: нажатие раскрывает список карт
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(p.hero)
                .clickable { showCards = !showCards }.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Всего на картах", color = p.heroText2, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(if (showCards) "Скрыть карты ▴" else "Мои карты ▾", color = p.onHero, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                if (balances.isEmpty()) "—" else m(onCards),
                color = p.onHero, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold,
            )
            if (balances.isEmpty() && !showCards) {
                Text("Остаток появится после первой операции по карте", color = p.heroText2, fontSize = 13.sp)
            }
            if (showCards) {
                walletsOf(balances).chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { bank ->
                            CardTile(bank, balances.firstOrNull { it.bank == bank }, ::m, Modifier.weight(1f)) { onOpenCard(bank) }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroStat("Доход", "+" + m(income), Modifier.weight(1f))
                HeroStat("Расход", "−" + m(expense), Modifier.weight(1f))
            }
        }

        // Кольца
        PCard(onClick = if (budget <= 0) onSetBudget else null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val goal = goals.firstOrNull()
                val spentShare = if (budget > 0) (expense / budget).toFloat() else 0f
                val incomeShare = if (income + expense > 0) (income / maxOf(income, expense)).toFloat() else 0f
                val goalShare = goal?.let { if (it.target > 0) (it.saved / it.target).toFloat() else 0f } ?: 0f
                Rings(listOf(spentShare to p.expense, incomeShare to p.income, goalShare to p.savings), 140.dp, 16.dp)
                Spacer(Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (budget > 0) {
                        RingLabel("Расходы", "${(spentShare * 100).toInt()}% бюджета", p.expense)
                        RingLabel("Осталось", m(budget - expense), p.warning)
                    } else {
                        RingLabel("Бюджет", "Нажмите, чтобы задать", p.expense)
                    }
                    RingLabel("Доходы", m(income), p.income)
                    RingLabel("Цель", goal?.let { "${(goalShare * 100).toInt()}% «${it.name}»" } ?: "Не задана", p.savings)
                }
            }
        }

        // Цели
        SectionTitle("Мои цели", "+ Цель") { onGoalClick(null) }
        if (goals.isEmpty()) {
            PCard(onClick = { onGoalClick(null) }) {
                Text("Добавьте цель накоплений, например «Отпуск»", color = p.text2, fontSize = 14.sp)
            }
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                goals.forEach { g ->
                    val share = if (g.target > 0) (g.saved / g.target).toFloat() else 0f
                    PCard(Modifier.width(230.dp), padding = 14.dp, onClick = { onGoalClick(g) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircleIcon(AppIcons.Target, p.savings, 36.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(g.name, color = p.text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
                            Text("${(share * 100).toInt()}%", color = p.savings, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("${m(g.saved)} из ${m(g.target)}", color = p.text, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Bar(share, p.savings, p.chip)
                    }
                }
            }
        }

        // Последние операции
        SectionTitle("Последние операции", "Все ›", onOpenOperations)
        PCard(padding = 12.dp) {
            val last = all.take(5)
            if (last.isEmpty()) Text("Пока нет операций. Они появятся сами после покупки или перевода.", color = p.text2, fontSize = 14.sp)
            last.forEachIndexed { i, t ->
                if (i > 0) HorizontalDivider(color = p.divider)
                TransactionRow(t) { onOpenTransaction(t) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Мини-карта: цвет банка, остаток и снизу название карты. */
@Composable
private fun CardTile(bank: String, b: Balance?, m: (Double) -> String, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    val c = p.bankColor(bank)
    val on = onColor(c)
    Column(
        modifier.height(104.dp).clip(RoundedCornerShape(18.dp)).background(c).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(b?.let { m(it.amount) } ?: "нет данных", color = on, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Column {
            Text(AppSettings.walletName(bank), color = on, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(b?.let { formatTime(it.updatedAt, dateTimeFormat) } ?: "остаток не пришёл", color = on.copy(alpha = 0.75f), fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier) {
    val p = LocalPalette.current
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(p.heroInner).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(label, color = p.heroText2, fontSize = 12.sp)
        Text(value, color = p.onHero, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
private fun RingLabel(title: String, value: String, color: androidx.compose.ui.graphics.Color) {
    val p = LocalPalette.current
    Column {
        Text(title, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}


/** Все кошельки: три банка, наличные и любые другие, у которых есть остаток. */
fun walletsOf(balances: List<Balance>): List<String> = AppSettings.wallets(balances)
