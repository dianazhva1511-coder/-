package kz.koshelek.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kz.koshelek.app.data.AppSettings
import kz.koshelek.app.data.Repository
import kz.koshelek.app.parser.Categories
import kz.koshelek.app.parser.NotificationParser
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Разделы настроек, которые пользователь меняет сам. */
@Composable
fun SettingsSections(repo: Repository) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var askName by remember { mutableStateOf<String?>(null) } // "wallet", "expense", "income"
    var renaming by remember { mutableStateOf<String?>(null) }
    var ruleOpen by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var catTab by remember { mutableIntStateOf(0) }

    fun toast(s: String) = Toast.makeText(context, s, Toast.LENGTH_SHORT).show()

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val list = repo.allTransactions()
            val f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val csv = buildString {
                append('﻿')
                append("Дата;Тип;Сумма;Категория;Карта;Описание;Источник\n")
                list.forEach { t ->
                    val d = Instant.ofEpochMilli(t.timestamp).atZone(ZoneId.systemDefault()).format(f)
                    val type = if (t.type == "INCOME") "Доход" else "Расход"
                    val amount = (if (t.type == "INCOME") t.amount else -t.amount).toString().replace('.', ',')
                    fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
                    append("$d;$type;$amount;${q(t.category)};${q(t.bank)};${q(t.merchant)};${if (t.source == "AUTO") "авто" else "вручную"}\n")
                }
            }
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
            }.onSuccess { toast("Сохранено операций: ${list.size}") }.onFailure { toast("Не удалось сохранить файл") }
        }
    }

    // Размер шрифта
    Section("Размер шрифта")
    PCard {
        Text("Меняет размер всех надписей и сумм в приложении.", color = p.text2, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            TextButton(onClick = { AppSettings.changeFontScale(AppSettings.fontScale - 0.05f) }) { Text("A−", color = p.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            Text("${(AppSettings.fontScale * 100).toInt()}%", color = p.text, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            TextButton(onClick = { AppSettings.changeFontScale(AppSettings.fontScale + 0.05f) }) { Text("A+", color = p.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        }
        androidx.compose.material3.Slider(
            value = AppSettings.fontScale, onValueChange = { AppSettings.changeFontScale(it) },
            valueRange = 0.8f..1.4f, steps = 11,
            colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = p.primary, activeTrackColor = p.primary),
        )
        Text("Пример: 125 000 ₸ · Продукты · Kaspi", color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        if (AppSettings.fontScale != 1f) TextButton(onClick = { AppSettings.changeFontScale(1f) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            Text("Вернуть обычный размер", color = p.primary)
        }
    }

    // Начало месяца
    Section("Начало месяца")
    PCard {
        Text("С какого числа считать месяц. Удобно, если зарплата приходит, например, 10-го.", color = p.text2, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            TextButton(onClick = { AppSettings.changeMonthStart(AppSettings.monthStart - 1) }) { Text("−", color = p.primary, fontSize = 22.sp) }
            Text("${AppSettings.monthStart}-е число", color = p.text, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            TextButton(onClick = { AppSettings.changeMonthStart(AppSettings.monthStart + 1) }) { Text("+", color = p.primary, fontSize = 22.sp) }
        }
    }

    // Карты и счета
    Section("Карты и счета", "+ Добавить") { askName = "wallet" }
    PCard(padding = 12.dp) {
        AppSettings.allWallets().forEachIndexed { i, w ->
            if (i > 0) HorizontalDivider(color = p.divider)
            val custom = w !in AppSettings.BASE_WALLETS
            ToggleRow(
                AppSettings.walletName(w),
                when {
                    AppSettings.walletName(w) != w -> "было: $w"
                    custom -> "своя карта"
                    else -> null
                },
                w !in AppSettings.hiddenWallets,
                onToggle = { AppSettings.setWalletHidden(w, !it) },
                onDelete = if (custom) ({ AppSettings.removeWallet(w) }) else null,
                onEdit = { renaming = w },
                leading = { BankBadge(w, 32.dp) },
            )
        }
        Text("Карандаш меняет название карты. Выключенные карты не показываются на главной и при добавлении операции.", color = p.text2, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }

    // Категории
    Section("Категории", "+ Добавить") { askName = if (catTab == 0) "expense" else "income" }
    Segmented(listOf("Расходы", "Доходы"), catTab) { catTab = it }
    PCard(padding = 12.dp) {
        val base = (if (catTab == 0) Categories.expense else Categories.income).filter { it != Categories.OWN }
        val custom = if (catTab == 0) AppSettings.customExpense else AppSettings.customIncome
        (base + custom).distinct().forEachIndexed { i, c ->
            if (i > 0) HorizontalDivider(color = p.divider)
            val isCustom = c in custom && c !in base
            ToggleRow(
                c, if (isCustom) "своя категория" else null, c !in AppSettings.hiddenCategories,
                onToggle = { AppSettings.setCategoryHidden(c, !it) },
                onDelete = if (isCustom) ({ AppSettings.removeCategory(c) }) else null,
                leading = { CircleIcon(iconOf(c), p.colorOf(c), 32.dp) },
            )
        }
    }

    // Правила
    Section("Правила категорий", "+ Правило") { ruleOpen = true }
    PCard(padding = 12.dp) {
        Text("Если в названии магазина есть слово, операция сразу попадёт в нужную категорию. Например: «Magnum» → Продукты.", color = p.text2, fontSize = 13.sp)
        if (AppSettings.rules.isEmpty()) Text("Правил пока нет.", color = p.text2, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        AppSettings.rules.forEach { r ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                CircleIcon(iconOf(r.category), p.colorOf(r.category), 30.dp)
                Spacer(Modifier.width(10.dp))
                Text("«${r.keyword}» → ${r.category}", color = p.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { AppSettings.removeRule(r) }) { Icon(AppIcons.Trash, "Удалить правило", tint = p.text2, modifier = Modifier.size(20.dp)) }
            }
        }
        if (AppSettings.rules.isNotEmpty()) TextButton(onClick = {
            scope.launch { val n = repo.applyRulesToAll(); toast("Обновлено операций: $n") }
        }) { Text("Применить к старым операциям", color = p.primary, fontWeight = FontWeight.Bold) }
    }

    // Банки
    Section("Какие уведомления читать")
    PCard(padding = 12.dp) {
        AppSettings.TRACKED.forEachIndexed { i, b ->
            if (i > 0) HorizontalDivider(color = p.divider)
            ToggleRow(
                if (b == NotificationParser.SAMSUNG) "Samsung Wallet" else b, null, b !in AppSettings.disabledBanks,
                onToggle = { AppSettings.setTracked(b, it) }, onDelete = null,
                leading = { BankBadge(b, 32.dp) },
            )
        }
    }

    // Данные
    Section("Данные")
    PCard {
        Text("Валюта: тенге (₸)", color = p.text, fontSize = 14.sp)
        TextButton(onClick = { exporter.launch("koshelek.csv") }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            Text("Сохранить все операции в файл (Excel, CSV)", color = p.primary, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = { confirmReset = true }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            Text("Удалить все операции", color = p.expense, fontWeight = FontWeight.Bold)
        }
    }

    askName?.let { kind ->
        NameDialog(
            when (kind) { "wallet" -> "Новая карта или счёт"; "income" -> "Новая категория дохода"; else -> "Новая категория расхода" },
            if (kind == "wallet") "Например: Jusan, Депозит" else "Например: Дети, Спорт",
            onDismiss = { askName = null },
        ) { name ->
            when (kind) {
                "wallet" -> AppSettings.addWallet(name)
                "income" -> AppSettings.addCategory(name, income = true)
                else -> AppSettings.addCategory(name, income = false)
            }
            askName = null
        }
    }
    renaming?.let { w ->
        NameDialog("Название карты", w, onDismiss = { renaming = null }, initial = AppSettings.walletName(w), confirm = "Сохранить") { name ->
            AppSettings.renameWallet(w, name); renaming = null
        }
    }
    if (ruleOpen) RuleDialog(onDismiss = { ruleOpen = false }) { k, c -> AppSettings.addRule(k, c); ruleOpen = false }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("Удалить все операции?") },
        text = { Text("Это нельзя отменить. Остатки по картам, цели и лимиты останутся. Можно сначала сохранить операции в файл.") },
        confirmButton = {
            TextButton(onClick = { scope.launch { repo.deleteAllTransactions(); toast("Операции удалены") }; confirmReset = false }) {
                Text("Удалить", color = p.expense)
            }
        },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена") } },
    )
}

@Composable
private fun Section(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Spacer(Modifier.height(4.dp))
    SectionTitle(title, action, onAction)
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String?,
    on: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: (() -> Unit)?,
    onEdit: (() -> Unit)? = null,
    leading: @Composable () -> Unit,
) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().clickable { onToggle(!on) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        leading()
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (on) p.text else p.text2, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (subtitle != null) Text(subtitle, color = p.text2, fontSize = 11.sp)
        }
        if (onEdit != null) IconButton(onClick = onEdit) { Icon(AppIcons.Pencil, "Переименовать", tint = p.text2, modifier = Modifier.size(20.dp)) }
        if (onDelete != null) IconButton(onClick = onDelete) { Icon(AppIcons.Trash, "Удалить", tint = p.text2, modifier = Modifier.size(20.dp)) }
        Switch(
            checked = on, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedTrackColor = p.primary, checkedThumbColor = p.onPrimary),
        )
    }
}

@Composable
private fun NameDialog(
    title: String,
    hint: String,
    onDismiss: () -> Unit,
    initial: String = "",
    confirm: String = "Добавить",
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(text, { text = it }, label = { Text("Название") }, placeholder = { Text(hint) }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onSave(text) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun RuleDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var keyword by remember { mutableStateOf("") }
    var income by remember { mutableStateOf(false) }
    val cats = (if (income) AppSettings.incomeCategories() else AppSettings.expenseCategories()).filter { it != Categories.OWN }
    var category by remember { mutableStateOf(cats.firstOrNull() ?: Categories.OTHER) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое правило") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(keyword, { keyword = it }, label = { Text("Слово в названии магазина") }, placeholder = { Text("Например: Magnum") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Расход", !income) { income = false; category = AppSettings.expenseCategories().firstOrNull() ?: Categories.OTHER }
                    Chip("Доход", income) { income = true; category = AppSettings.incomeCategories().firstOrNull() ?: Categories.OTHER }
                }
                Text("Категория", fontSize = 13.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cats.forEach { c -> Chip(c, c == category) { category = c } }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (keyword.isNotBlank()) onSave(keyword, category) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
