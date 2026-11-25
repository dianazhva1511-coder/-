package kz.koshelek.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.koshelek.app.parser.Categories
import kz.koshelek.app.parser.NotificationParser
import java.time.LocalDate
import java.time.YearMonth

/** Правило «если в названии магазина есть слово → такая категория». */
data class Rule(val keyword: String, val category: String)

/**
 * Настройки, которые пользователь меняет сам. Хранятся на телефоне (SharedPreferences),
 * а в интерфейсе обновляются сразу, без перезапуска.
 */
object AppSettings {
    val BASE_WALLETS = listOf("Kaspi", "Halyk", "Forte", "Наличные")
    val TRACKED = listOf("Kaspi", "Halyk", "Forte", NotificationParser.SAMSUNG)

    private var sp: SharedPreferences? = null

    var customExpense by mutableStateOf(listOf<String>()); private set
    var customIncome by mutableStateOf(listOf<String>()); private set
    var hiddenCategories by mutableStateOf(setOf<String>()); private set
    var extraWallets by mutableStateOf(listOf<String>()); private set
    var hiddenWallets by mutableStateOf(setOf<String>()); private set
    var rules by mutableStateOf(listOf<Rule>()); private set
    var disabledBanks by mutableStateOf(setOf<String>()); private set
    var monthStart by mutableIntStateOf(1); private set
    var walletNames by mutableStateOf(mapOf<String, String>()); private set
    var fontScale by mutableFloatStateOf(1f); private set

    fun init(context: Context) {
        if (sp != null) return
        val s = context.applicationContext.getSharedPreferences("koshelek", Context.MODE_PRIVATE)
        sp = s
        customExpense = s.list("customExpense")
        customIncome = s.list("customIncome")
        hiddenCategories = s.list("hiddenCategories").toSet()
        extraWallets = s.list("extraWallets")
        hiddenWallets = s.list("hiddenWallets").toSet()
        disabledBanks = s.list("disabledBanks").toSet()
        rules = s.list("rules").mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size == 2) Rule(parts[0], parts[1]) else null
        }
        monthStart = s.getInt("monthStart", 1).coerceIn(1, 28)
        walletNames = s.list("walletNames").mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
        fontScale = s.getFloat("fontScale", 1f).coerceIn(0.8f, 1.4f)
    }

    private fun SharedPreferences.list(key: String): List<String> =
        getString(key, null)?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()

    private fun save(key: String, values: Collection<String>) {
        sp?.edit()?.putString(key, values.joinToString("\n"))?.apply()
    }

    // Категории
    fun expenseCategories(): List<String> =
        (Categories.expense.filter { it !in hiddenCategories } + customExpense).distinct()

    fun incomeCategories(): List<String> =
        (Categories.income.filter { it !in hiddenCategories } + customIncome).distinct()

    fun addCategory(name: String, income: Boolean) {
        val n = name.trim()
        if (n.isEmpty()) return
        if (income) { customIncome = (customIncome + n).distinct(); save("customIncome", customIncome) }
        else { customExpense = (customExpense + n).distinct(); save("customExpense", customExpense) }
        if (n in hiddenCategories) setCategoryHidden(n, false)
    }

    fun removeCategory(name: String) {
        customExpense = customExpense - name; save("customExpense", customExpense)
        customIncome = customIncome - name; save("customIncome", customIncome)
    }

    fun setCategoryHidden(name: String, hidden: Boolean) {
        hiddenCategories = if (hidden) hiddenCategories + name else hiddenCategories - name
        save("hiddenCategories", hiddenCategories)
    }

    // Карты и счета
    fun allWallets(): List<String> = (BASE_WALLETS + extraWallets).distinct()

    fun wallets(balances: List<Balance>): List<String> =
        (allWallets() + balances.map { it.bank }).distinct().filter { it !in hiddenWallets }

    fun addWallet(name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        extraWallets = (extraWallets + n).distinct(); save("extraWallets", extraWallets)
        if (n in hiddenWallets) setWalletHidden(n, false)
    }

    fun removeWallet(name: String) {
        extraWallets = extraWallets - name; save("extraWallets", extraWallets)
    }

    fun setWalletHidden(name: String, hidden: Boolean) {
        hiddenWallets = if (hidden) hiddenWallets + name else hiddenWallets - name
        save("hiddenWallets", hiddenWallets)
    }

    /** Как показывать карту: своё название, если пользователь его задал. "Kaspi · Samsung Pay" тоже переименуется. */
    fun walletName(bank: String): String {
        walletNames[bank]?.let { return it }
        val key = walletNames.keys.firstOrNull { bank.startsWith("$it ") } ?: return bank
        return walletNames.getValue(key) + bank.removePrefix(key)
    }

    fun renameWallet(bank: String, name: String) {
        val n = name.trim()
        walletNames = if (n.isEmpty() || n == bank) walletNames - bank else walletNames + (bank to n)
        save("walletNames", walletNames.map { "${it.key}\t${it.value}" })
    }

    fun changeFontScale(v: Float) {
        fontScale = (Math.round(v * 20) / 20f).coerceIn(0.8f, 1.4f)
        sp?.edit()?.putFloat("fontScale", fontScale)?.apply()
    }

    // Правила
    fun addRule(keyword: String, category: String) {
        val k = keyword.trim()
        if (k.isEmpty()) return
        rules = rules.filter { !it.keyword.equals(k, ignoreCase = true) } + Rule(k, category)
        save("rules", rules.map { "${it.keyword}\t${it.category}" })
    }

    fun removeRule(rule: Rule) {
        rules = rules - rule
        save("rules", rules.map { "${it.keyword}\t${it.category}" })
    }

    /** Категория по правилам пользователя или null, если ни одно правило не подошло. */
    fun ruleCategory(merchant: String, raw: String = ""): String? = rules.firstOrNull {
        merchant.contains(it.keyword, ignoreCase = true) || raw.contains(it.keyword, ignoreCase = true)
    }?.category

    // Какие банки слушать
    fun isTracked(bank: String) = TRACKED.none { bank.startsWith(it) && it in disabledBanks }

    fun setTracked(bank: String, on: Boolean) {
        disabledBanks = if (on) disabledBanks - bank else disabledBanks + bank
        save("disabledBanks", disabledBanks)
    }

    // Начало месяца
    fun changeMonthStart(day: Int) {
        monthStart = day.coerceIn(1, 28)
        sp?.edit()?.putInt("monthStart", monthStart)?.apply()
    }

    /** К какому «месяцу» относится день с учётом дня начала месяца (например, день зарплаты). */
    fun periodOf(d: LocalDate): YearMonth =
        if (d.dayOfMonth >= monthStart) YearMonth.from(d) else YearMonth.from(d).minusMonths(1)

    fun currentPeriod(): YearMonth = periodOf(LocalDate.now())
}
