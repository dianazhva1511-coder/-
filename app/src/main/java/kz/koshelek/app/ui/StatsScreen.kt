package kz.koshelek.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun StatsScreen(all: List<Transaction>) {
    val p = LocalPalette.current
    var mode by remember { mutableIntStateOf(0) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    val list = all.inMonth(month).filter { it.counts && it.isIncome == (mode == 1) }
    val total = list.sumOf { it.amount }
    val byCat = list.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amount } }.toList().sortedByDescending { it.second }
    val days = if (month == YearMonth.now()) LocalDate.now().dayOfMonth else month.lengthOfMonth()

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Статистика", color = p.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 16.dp))
        Segmented(listOf("Расходы", "Доходы"), mode) { mode = it }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.hero).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (mode == 0) "Всего расходов" else "Всего доходов", color = p.heroText2, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(money(total), color = p.onHero, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
        MonthSwitcher(month) { month = it }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Donut(byCat.map { it.second to p.colorOf(it.first) }, 210.dp, 30.dp, p.chip)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("в среднем в день", color = p.text2, fontSize = 13.sp)
                Text(money(if (days > 0) Math.round(total / days).toDouble() else 0.0), color = p.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        if (byCat.isEmpty()) Text("Нет операций за этот месяц", color = p.text2, modifier = Modifier.padding(4.dp))
        PCard {
            byCat.forEachIndexed { i, (cat, sum) ->
                if (i > 0) Spacer(Modifier.height(14.dp))
                val color = p.colorOf(cat)
                val share = if (total > 0) (sum / total).toFloat() else 0f
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircleIcon(iconOf(cat), color)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row {
                            Text(cat, color = p.text, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text("${(share * 100).toInt()}%  ", color = p.text2, fontSize = 13.sp)
                            Text(money(sum), color = p.text, fontWeight = FontWeight.Bold)
                        }
                        Bar(share, color, p.chip)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
