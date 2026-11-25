@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package kz.koshelek.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.koshelek.app.data.Goal
import kz.koshelek.app.data.Transaction
import kz.koshelek.app.parser.Categories
import kz.koshelek.app.parser.TxType

fun parseAmount(s: String): Double? = s.replace(" ", "").replace(" ", "").replace(",", ".").toDoubleOrNull()

/** Быстрое добавление операции вручную (QR, наличные, переводы без уведомления). */
@Composable
fun AddSheet(onDismiss: () -> Unit, onAdd: (Double, TxType, String, String, String) -> Unit) {
    val p = LocalPalette.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TxType.EXPENSE) }
    var category by remember { mutableStateOf("Продукты") }
    var wallet by remember { mutableStateOf("Kaspi") }
    var note by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val amount = parseAmount(amountText)
    val cats = (if (type == TxType.INCOME) Categories.income else Categories.expense).filter { it != Categories.OWN }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = if (p.dark) androidx.compose.ui.graphics.Color(0xFF1C1C1E) else androidx.compose.ui.graphics.Color.White) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Segmented(listOf("Расход", "Доход"), if (type == TxType.EXPENSE) 0 else 1) {
                type = if (it == 0) TxType.EXPENSE else TxType.INCOME
                category = if (type == TxType.INCOME) Categories.INCOME else "Продукты"
            }
            OutlinedTextField(
                amountText, { amountText = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                placeholder = { Text("0 ₸", fontSize = 34.sp, color = p.text2, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                textStyle = TextStyle(fontFamily = AppFont, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = if (type == TxType.EXPENSE) p.expense else p.income, textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
            Text("Категория", color = p.text2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            FlowRow(
                Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 4,
            ) {
                cats.forEach { c ->
                    val sel = c == category
                    Column(
                        Modifier.width(78.dp).clip(RoundedCornerShape(14.dp)).clickable { category = c }.padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        RingIcon(iconOf(c), p.colorOf(c), if (sel) 1f else null, 56.dp)
                        Spacer(Modifier.height(4.dp))
                        Text(shortName(c), color = if (sel) p.text else p.text2, fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                    }
                }
            }
            Text("Откуда", color = p.text2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Kaspi", "Halyk", "Forte", "Наличные").forEach { w -> Chip(w, wallet == w) { wallet = w } }
            }
            OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Описание (необязательно)") }, singleLine = true)
            Button(
                onClick = { onAdd(amount!!, type, note.ifBlank { category }, category, wallet) },
                enabled = amount != null && amount > 0,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.primary, contentColor = p.onPrimary),
            ) { Text("Добавить", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun EditTransactionDialog(t: Transaction, onDismiss: () -> Unit, onSave: (Transaction) -> Unit, onDelete: () -> Unit) {
    val p = LocalPalette.current
    var merchant by remember { mutableStateOf(t.merchant) }
    var category by remember { mutableStateOf(t.category) }
    val type = TxType.valueOf(t.type)
    val cats = ((if (type == TxType.INCOME) Categories.income else Categories.expense) + t.category).distinct()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text((if (type == TxType.INCOME) "+" else "−") + money(t.amount)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${t.bank} · ${formatTime(t.timestamp, dateTimeFormat)}", color = p.text2, fontSize = 13.sp)
                OutlinedTextField(merchant, { merchant = it }, label = { Text("Описание") }, singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    cats.forEach { c -> Chip(shortName(c), c == category) { category = c } }
                }
                if (category == Categories.OWN) {
                    Text("«Между своими» не считается ни доходом, ни расходом.", color = p.text2, fontSize = 12.sp)
                }
                if (t.rawText.isNotBlank()) Text("Уведомление: ${t.rawText}", color = p.text2, fontSize = 12.sp)
                TextButton(onClick = onDelete) { Text("Удалить операцию", color = p.expense) }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(t.copy(merchant = merchant, category = category)) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

/** Ввод суммы: лимит, остаток, бюджет. */
@Composable
fun AmountDialog(title: String, hint: String, initial: Double?, onDismiss: () -> Unit, onSave: (Double?) -> Unit) {
    var text by remember { mutableStateOf(initial?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(hint, fontSize = 13.sp)
                OutlinedTextField(
                    text, { text = it }, Modifier.focusRequester(focus), label = { Text("Сумма, ₸") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(parseAmount(text)) }) { Text("Сохранить") } },
        dismissButton = {
            Row {
                if (initial != null) TextButton(onClick = { onSave(null) }) { Text("Убрать") }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        },
    )
}

/** Новая цель или пополнение существующей. */
@Composable
fun GoalDialog(
    goal: Goal?,
    onDismiss: () -> Unit,
    onCreate: (String, Double) -> Unit,
    onUpdate: (Goal) -> Unit,
    onDelete: (Goal) -> Unit,
) {
    var name by remember { mutableStateOf(goal?.name ?: "") }
    var target by remember { mutableStateOf(goal?.target?.toLong()?.toString() ?: "") }
    var add by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (goal == null) "Новая цель" else goal.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (goal != null) {
                    Text("Накоплено ${money(goal.saved)} из ${money(goal.target)}")
                    OutlinedTextField(
                        add, { add = it }, label = { Text("Отложить ещё, ₸ (минус, чтобы снять)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true)
                OutlinedTextField(
                    target, { target = it }, label = { Text("Сколько нужно, ₸") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                if (goal != null) TextButton(onClick = { onDelete(goal) }) { Text("Удалить цель") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val t = parseAmount(target) ?: 0.0
                if (goal == null) {
                    if (name.isNotBlank() && t > 0) onCreate(name.trim(), t)
                } else {
                    val delta = parseAmount(add.replace("−", "-")) ?: 0.0
                    onUpdate(goal.copy(name = name.ifBlank { goal.name }, target = if (t > 0) t else goal.target, saved = (goal.saved + delta).coerceAtLeast(0.0)))
                }
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
