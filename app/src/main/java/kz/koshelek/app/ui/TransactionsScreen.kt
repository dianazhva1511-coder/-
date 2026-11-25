@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package kz.koshelek.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kz.koshelek.app.data.Repository
import kz.koshelek.app.data.Transaction
import kz.koshelek.app.parser.Categories
import kz.koshelek.app.parser.TxType

@Composable
fun TransactionsScreen(all: List<Transaction>, repo: Repository, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Transaction?>(null) }

    Box(modifier.fillMaxSize()) {
        if (all.isEmpty()) {
            Text(
                "Операций пока нет. Они появятся сами после первой покупки или перевода. " +
                    "Можно добавить вручную кнопкой «+».",
                Modifier.padding(24.dp),
            )
        }
        val grouped = all.groupBy { it.timestamp.toLocalDate() }
        LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
            grouped.forEach { (day, list) ->
                item(key = "h$day") {
                    Text(
                        day.format(dayFormat),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                    )
                }
                items(list, key = { it.id }) { t ->
                    TransactionRow(t) { editing = t }
                    HorizontalDivider()
                }
            }
        }
        FloatingActionButton(
            onClick = { adding = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Filled.Add, contentDescription = "Добавить") }
    }

    if (adding) {
        AddDialog(onDismiss = { adding = false }) { amount, type, merchant, cat ->
            scope.launch { repo.addManual(amount, type, merchant, cat, System.currentTimeMillis()) }
            adding = false
        }
    }
    editing?.let { t ->
        EditDialog(
            t,
            onDismiss = { editing = null },
            onSave = { scope.launch { repo.update(it) }; editing = null },
            onDelete = { scope.launch { repo.delete(t.id) }; editing = null },
        )
    }
}

@Composable
private fun TransactionRow(t: Transaction, onClick: () -> Unit) {
    val income = t.type == "INCOME"
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(t.merchant, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${t.category} · ${t.bank} · ${formatTime(t.timestamp, timeFormat)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            (if (income) "+" else "−") + money(t.amount),
            color = if (income) IncomeColor else ExpenseColor,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CategoryPicker(type: TxType, selected: String, onSelect: (String) -> Unit) {
    val list = if (type == TxType.INCOME) Categories.income else Categories.expense
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        list.forEach { c ->
            FilterChip(selected = c == selected, onClick = { onSelect(c) }, label = { Text(c) })
        }
    }
}

@Composable
private fun AddDialog(onDismiss: () -> Unit, onAdd: (Double, TxType, String, String) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TxType.EXPENSE) }
    var merchant by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Categories.OTHER) }
    val amount = amountText.replace(" ", "").replace(",", ".").toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая операция") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(type == TxType.EXPENSE, { type = TxType.EXPENSE; category = Categories.OTHER }, { Text("Расход") })
                    FilterChip(type == TxType.INCOME, { type = TxType.INCOME; category = Categories.INCOME }, { Text("Поступление") })
                }
                OutlinedTextField(
                    amountText, { amountText = it }, label = { Text("Сумма, ₸") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                )
                OutlinedTextField(merchant, { merchant = it }, label = { Text("Описание") }, singleLine = true)
                CategoryPicker(type, category) { category = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = amount != null && amount > 0,
                onClick = { onAdd(amount!!, type, merchant.ifBlank { category }, category) },
            ) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun EditDialog(t: Transaction, onDismiss: () -> Unit, onSave: (Transaction) -> Unit, onDelete: () -> Unit) {
    var merchant by remember { mutableStateOf(t.merchant) }
    var category by remember { mutableStateOf(t.category) }
    val type = TxType.valueOf(t.type)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text((if (type == TxType.INCOME) "+" else "−") + money(t.amount)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${t.bank} · ${formatTime(t.timestamp, dateTimeFormat)}", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(merchant, { merchant = it }, label = { Text("Описание") }, singleLine = true)
                CategoryPicker(type, category) { category = it }
                if (t.rawText.isNotBlank()) {
                    Text("Уведомление: ${t.rawText}", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onDelete) { Text("Удалить операцию", color = ExpenseColor) }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(t.copy(merchant = merchant, category = category)) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
