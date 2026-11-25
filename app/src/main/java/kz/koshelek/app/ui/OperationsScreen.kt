package kz.koshelek.app.ui

import kz.koshelek.app.data.AppSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.koshelek.app.data.Transaction
import kz.koshelek.app.data.counts
import kz.koshelek.app.data.isIncome

@Composable
fun OperationsScreen(all: List<Transaction>, onBack: () -> Unit, onOpen: (Transaction) -> Unit) {
    val p = LocalPalette.current
    var query by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("Все") }
    var type by remember { mutableIntStateOf(0) }
    val banks = listOf("Все") + AppSettings.allWallets().filter { it !in AppSettings.hiddenWallets } + "Вручную"
    val q = query.trim().lowercase()
    val filtered = all.filter { t ->
        (bank == "Все" || (bank == "Вручную" && t.source == "MANUAL") || t.bank.startsWith(bank)) &&
            (type == 0 || (type == 1 && !t.isIncome) || (type == 2 && t.isIncome)) &&
            (q.isEmpty() || t.merchant.lowercase().contains(q) || t.category.lowercase().contains(q) ||
                money(t.amount).replace(" ", "").replace(" ", "").contains(q.replace(" ", "")))
    }
    val grouped = filtered.groupBy { it.timestamp.toLocalDate() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        item {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(AppIcons.Back, "Назад", tint = p.text) }
                Text("Операции", color = p.text, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            }
            Row(
                Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(p.chip).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(AppIcons.Search, null, tint = p.text2, modifier = Modifier.width(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Поиск: магазин, сумма, категория", color = p.text2, fontSize = 15.sp)
                    BasicTextField(
                        query, { query = it }, singleLine = true,
                        textStyle = TextStyle(fontFamily = AppFont, color = p.text, fontSize = 15.sp),
                        cursorBrush = SolidColor(p.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                banks.forEach { b -> Chip(b, bank == b) { bank = b } }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Все", "Расходы", "Доходы").forEachIndexed { i, s -> Chip(s, type == i) { type = i } }
            }
            if (filtered.isEmpty()) Text("Ничего не найдено", color = p.text2, modifier = Modifier.padding(top = 24.dp))
        }
        grouped.forEach { (day, list) ->
            item(key = "h$day") {
                val sum = list.filter { it.counts }.sumOf { if (it.isIncome) it.amount else -it.amount }
                Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp, start = 4.dp, end = 4.dp)) {
                    Text(day.format(dayFormat).cap(), color = p.text2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text((if (sum >= 0) "+" else "") + money(sum), color = p.text2, fontSize = 13.sp)
                }
            }
            items(list, key = { it.id }) { t ->
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card).padding(horizontal = 12.dp)) {
                    TransactionRow(t) { onOpen(t) }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier.height(38.dp).clip(RoundedCornerShape(19.dp))
            .background(if (selected) p.primary else p.chip)
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) p.onPrimary else p.text, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

