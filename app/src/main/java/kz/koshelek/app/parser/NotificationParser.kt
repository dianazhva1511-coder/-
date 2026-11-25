package kz.koshelek.app.parser

/** Тип операции: поступление или списание. */
enum class TxType { INCOME, EXPENSE }

data class ParsedTransaction(
    val amount: Double,
    val type: TxType,
    val merchant: String,
    val category: String,
)

/**
 * Распознаёт тексты уведомлений банков Kaspi, Forte, Halyk и Samsung Wallet.
 * Работает по ключевым словам, поэтому подходит и для push, и для SMS.
 */
object NotificationParser {

    const val KASPI = "Kaspi"
    const val HALYK = "Halyk"
    const val FORTE = "Forte"
    const val SAMSUNG = "Samsung Wallet"

    /** Известные пакеты приложений. Если пакет другой, банк узнаётся по названию приложения. */
    private val bankByPackage = mapOf(
        "kz.kaspi.mobile" to KASPI,
        "kz.kaspi.business" to KASPI,
        "kz.halyk.onlinebank" to HALYK,
        "kz.halykbank.homebank" to HALYK,
        "kz.halyk.homebank" to HALYK,
        "kz.forte.mobile" to FORTE,
        "kz.fortebank.mobile" to FORTE,
        "kz.forte.bank" to FORTE,
        "com.samsung.android.spay" to SAMSUNG,
        "com.samsung.android.samsungpay.gear" to SAMSUNG,
    )

    /** Приложения SMS: для них банк определяется по отправителю (заголовку). */
    private val smsPackages = setOf(
        "com.samsung.android.messaging",
        "com.google.android.apps.messaging",
        "com.android.mms",
    )

    private fun bankByName(name: String): String? {
        val n = name.lowercase()
        return when {
            "kaspi" in n || "каспи" in n -> KASPI
            "halyk" in n || "халык" in n || "homebank" in n -> HALYK
            "forte" in n || "форте" in n -> FORTE
            "samsung wallet" in n || "samsung pay" in n -> SAMSUNG
            else -> null
        }
    }

    /**
     * Возвращает название банка, если уведомление пришло от банка, Samsung Wallet
     * или это SMS от банка. Иначе null: такие уведомления не читаются вообще.
     */
    fun detectBank(packageName: String, appLabel: String, title: String): String? {
        bankByPackage[packageName]?.let { return it }
        if (packageName in smsPackages) return bankByName(title)
        return bankByName(appLabel)
    }

    // Сумма: "5 000 ₸", "1 500,50 KZT", "12000.00 тг", "₸ 2 300"
    private const val NUM = """(\d{1,3}(?:[   ]\d{3})+|\d+)(?:[.,](\d{1,2}))?"""
    private const val CUR = """(?:₸|〒|KZT|тг\.?|тенге)"""
    private val amountAfter = Regex("""(?<![\d.,])$NUM\s?$CUR""", RegexOption.IGNORE_CASE)
    private val amountBefore = Regex("""$CUR\s?$NUM""", RegexOption.IGNORE_CASE)

    private val balanceWords = listOf(
        "доступно", "баланс", "остаток", "лимит", "available", "balance", "қолжетімді", "бонус",
    )

    private val ignoreWords = listOf(
        "код подтверждения", "код для", "одноразовый", "otp", "пароль", "не сообщайте",
        "кредитный лимит одобрен", "акция", "розыгрыш",
    )

    /** Однозначные признаки, проверяются первыми. */
    private val strongIncome = listOf(
        "возврат", "перевод от", "вам перевел", "входящий перевод", "refund",
    )
    private val strongExpense = listOf(
        "покупка", "оплата", "оплачено", "списан", "снятие", "отправлен", "перевод на", "purchase", "payment",
    )

    private val incomeWords = listOf(
        "поступ", "пополн", "зачисл", "входящий", "перевод от", "получен", "вам перевели",
        "возврат", "кешбэк", "кэшбэк", "cashback", "зарплат", "заработн", "refund",
        "credited", "received",
    )

    private val expenseWords = listOf(
        "покупка", "оплат", "списан", "снятие", "снят", "перевод", "платеж", "платёж",
        "purchase", "payment", "withdraw", "debited", "сатып алу", "төлем",
    )

    private data class AmountMatch(val value: Double, val start: Int, val end: Int)

    private fun findAmounts(text: String): List<AmountMatch> {
        val result = mutableListOf<AmountMatch>()
        for (m in amountAfter.findAll(text)) {
            result += AmountMatch(toNumber(m.groupValues[1], m.groupValues[2]), m.range.first, m.range.last + 1)
        }
        if (result.isEmpty()) {
            for (m in amountBefore.findAll(text)) {
                result += AmountMatch(toNumber(m.groupValues[1], m.groupValues[2]), m.range.first, m.range.last + 1)
            }
        }
        return result
    }

    private fun toNumber(intPart: String, frac: String): Double {
        val digits = intPart.filter { it.isDigit() }
        return if (frac.isEmpty()) digits.toDouble() else "$digits.$frac".toDouble()
    }

    private fun isBalance(text: String, start: Int): Boolean {
        val before = text.substring(maxOf(0, start - 25), start).lowercase()
        return balanceWords.any { it in before }
    }

    fun parse(title: String, text: String): ParsedTransaction? {
        val full = listOf(title, text).filter { it.isNotBlank() }.joinToString(". ")
        val lower = full.lowercase()
        if (ignoreWords.any { it in lower }) return null

        val amount = findAmounts(full).firstOrNull { !isBalance(full, it.start) && it.value > 0 } ?: return null

        val signBefore = full.substring(maxOf(0, amount.start - 2), amount.start).trim()
        val type = when {
            signBefore.endsWith("+") -> TxType.INCOME
            signBefore.endsWith("-") || signBefore.endsWith("−") -> TxType.EXPENSE
            strongIncome.any { it in lower } -> TxType.INCOME
            strongExpense.any { it in lower } -> TxType.EXPENSE
            incomeWords.any { it in lower } -> TxType.INCOME
            expenseWords.any { it in lower } -> TxType.EXPENSE
            else -> return null
        }

        val merchant = extractMerchant(full, amount, title)
        return ParsedTransaction(amount.value, type, merchant, Categories.guess(merchant, full, type))
    }

    private val merchantAfterV = Regex("""(?:^|\s)(?:в|на|at|в магазине)\s+([^\n.,;]{2,40})""", RegexOption.IGNORE_CASE)
    private val fromPerson = Regex("""(?:от|отправитель:?)\s+([^\n.,;]{2,40})""", RegexOption.IGNORE_CASE)

    private fun extractMerchant(full: String, amount: AmountMatch, title: String): String {
        val tail = full.substring(amount.end)
            .split(Regex("""(?i)доступно|баланс|остаток|карта\s*\*|\*\d{4}"""))
            .first()
        fromPerson.find(tail)?.let { return clean(it.groupValues[1]) }
        merchantAfterV.find(tail)?.let { return clean(it.groupValues[1]) }
        val t = clean(tail)
        if (t.length >= 2 && t.any { it.isLetter() }) return t.take(40)
        merchantAfterV.find(full)?.let { return clean(it.groupValues[1]) }
        return clean(title).ifEmpty { "Без названия" }
    }

    private fun clean(s: String): String =
        s.trim().trim('.', ',', ':', ';', '-', '—', ' ').replace(Regex("""\s+"""), " ").take(40)
}

/** Категории и автоматическое определение по названию магазина. */
object Categories {
    const val OTHER = "Другое"
    const val TRANSFERS = "Переводы"
    const val INCOME = "Доход"

    val expense = listOf(
        "Продукты", "Кафе и рестораны", "Транспорт", "Такси", "Покупки", "Связь и интернет",
        "Коммунальные", "Здоровье", "Развлечения", TRANSFERS, OTHER,
    )
    val income = listOf(INCOME, TRANSFERS, "Кешбэк", OTHER)

    private val rules = listOf(
        "Продукты" to listOf("magnum", "магнум", "small", "смолл", "galmart", "anvar", "анвар", "airba", "ramstore", "metro", "toimart", "спар", "spar", "дастархан", "продукт", "market", "маркет", "supermarket"),
        "Кафе и рестораны" to listOf("coffee", "кофе", "cafe", "кафе", "kfc", "burger", "бургер", "mcdonald", "salam bro", "pizza", "пицц", "restaurant", "ресторан", "starbucks", "doner", "донер", "wolt", "glovo", "chocofood"),
        "Такси" to listOf("yandex go", "yandex.go", "яндекс go", "taxi", "такси", "uber", "indrive", "indriver"),
        "Транспорт" to listOf("onay", "онай", "azs", "азс", "helios", "sinooil", "qazaqoil", "гелиос", "parking", "парков"),
        "Покупки" to listOf("wildberries", "ozon", "kaspi магазин", "kaspi.kz магазин", "technodom", "технодом", "sulpak", "сулпак", "mechta", "мечта", "lc waikiki", "zara", "dns"),
        "Связь и интернет" to listOf("beeline", "билайн", "kcell", "activ", "актив", "tele2", "altel", "kazakhtelecom", "казахтелеком"),
        "Коммунальные" to listOf("алсеко", "alseco", "энерго", "водоканал", "коммунал", "kezek", "ivc", "ерц"),
        "Здоровье" to listOf("аптек", "apteka", "pharm", "клиник", "clinic", "invitro", "olymp", "медицин"),
        "Развлечения" to listOf("kinopark", "кинопарк", "chaplin", "кино", "netflix", "spotify", "steam", "youtube", "apple.com", "google"),
    )

    fun guess(merchant: String, fullText: String, type: TxType): String {
        val m = (merchant + " " + fullText).lowercase()
        if (type == TxType.INCOME) {
            return when {
                "кешбэк" in m || "кэшбэк" in m || "cashback" in m || "бонус" in m -> "Кешбэк"
                "перевод" in m -> TRANSFERS
                else -> INCOME
            }
        }
        for ((cat, words) in rules) if (words.any { it in m }) return cat
        if ("перевод" in m) return TRANSFERS
        return OTHER
    }
}
