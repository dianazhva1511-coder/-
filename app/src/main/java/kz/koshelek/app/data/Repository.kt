package kz.koshelek.app.data

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kz.koshelek.app.parser.Categories
import kz.koshelek.app.parser.NotificationParser
import kz.koshelek.app.parser.TxType
import kotlin.math.abs

private val lock = Mutex()

class Repository(context: Context) {
    private val db = AppDatabase.get(context)
    val transactions = db.transactions().all()
    val rawNotifications = db.rawNotifications().latest()
    val goals = db.extras().goals()
    val limits = db.extras().limits()
    val balances = db.extras().balances()

    /**
     * Обрабатывает уведомление банка: распознаёт, проверяет на дубликаты и сохраняет.
     *
     * Дубликаты: при оплате через Samsung Pay приходят два уведомления (от Wallet и от банка)
     * на одну и ту же сумму. Они склеиваются в одну операцию, банк берётся из уведомления банка.
     * Повтор того же самого текста от того же банка в течение 10 минут тоже пропускается.
     *
     * Перевод между своими картами: списание в одном банке и поступление той же суммы в другом
     * в течение 5 минут помечаются категорией «Между своими» и не считаются в доходах и расходах.
     */
    suspend fun handleNotification(
        packageName: String, appName: String, bank: String, title: String, text: String, time: Long,
    ) = lock.withLock {
        val parsed = NotificationParser.parse(title, text)
        val raw = "$title | $text"
        if (bank != NotificationParser.SAMSUNG) {
            NotificationParser.parseBalance(title, text)?.let {
                db.extras().setBalance(Balance(bank, it, time))
            }
        }
        val result: String = if (parsed == null) {
            "не распознано"
        } else {
            val type = parsed.type.name
            val recent = db.transactions().between(time - 10 * 60_000, time + 60_000)
            val sameText = recent.firstOrNull { it.bank.startsWith(bank) && it.rawText == raw }
            val pair = recent.firstOrNull {
                it.type == type && abs(it.amount - parsed.amount) < 0.01 &&
                    abs(it.timestamp - time) < 3 * 60_000 &&
                    (isWallet(it.bank) != (bank == NotificationParser.SAMSUNG))
            }
            when {
                sameText != null -> "дубликат"
                pair != null -> {
                    val realBank = if (bank == NotificationParser.SAMSUNG) pair.bank else bank
                    val merchant = if (bank == NotificationParser.SAMSUNG) pair.merchant else parsed.merchant
                    val category = if (bank == NotificationParser.SAMSUNG) pair.category else parsed.category
                    db.transactions().update(
                        pair.copy(bank = "$realBank · Samsung Pay", merchant = merchant, category = category)
                    )
                    "объединено с Samsung Pay"
                }
                else -> {
                    val opposite = if (parsed.type == TxType.INCOME) TxType.EXPENSE.name else TxType.INCOME.name
                    val mirror = if (bank == NotificationParser.SAMSUNG) null else recent.firstOrNull {
                        it.type == opposite && abs(it.amount - parsed.amount) < 0.01 &&
                            abs(it.timestamp - time) < 5 * 60_000 &&
                            !it.bank.startsWith(bank) && !isWallet(it.bank) &&
                            it.category != Categories.OWN && it.source == "AUTO"
                    }
                    val category = if (mirror != null) Categories.OWN else parsed.category
                    if (mirror != null) db.transactions().update(mirror.copy(category = Categories.OWN))
                    db.transactions().insert(
                        Transaction(
                            amount = parsed.amount, type = type, bank = bank, merchant = parsed.merchant,
                            category = category, timestamp = time, source = "AUTO", rawText = raw,
                        )
                    )
                    when {
                        mirror != null -> "перевод между своими"
                        parsed.type == TxType.INCOME -> "добавлено: поступление"
                        else -> "добавлено: списание"
                    }
                }
            }
        }
        db.rawNotifications().insert(RawNotification(0, packageName, appName, title, text, time, result))
    }

    private fun isWallet(bank: String) = bank == NotificationParser.SAMSUNG

    suspend fun addManual(amount: Double, type: TxType, merchant: String, category: String, bank: String, time: Long) {
        db.transactions().insert(
            Transaction(
                amount = amount, type = type.name, bank = bank, merchant = merchant,
                category = category, timestamp = time, source = "MANUAL",
            )
        )
    }

    suspend fun update(t: Transaction) = db.transactions().update(t)
    suspend fun delete(id: Long) = db.transactions().delete(id)
    suspend fun clearLog() = db.rawNotifications().clear()

    suspend fun addGoal(name: String, target: Double) = db.extras().insertGoal(Goal(0, name, target, 0.0))
    suspend fun updateGoal(g: Goal) = db.extras().updateGoal(g)
    suspend fun deleteGoal(id: Long) = db.extras().deleteGoal(id)

    suspend fun setLimit(category: String, amount: Double?) {
        if (amount == null || amount <= 0) db.extras().deleteLimit(category)
        else db.extras().setLimit(CategoryLimit(category, amount))
    }

    suspend fun setBalance(bank: String, amount: Double) =
        db.extras().setBalance(Balance(bank, amount, System.currentTimeMillis()))
}

/** Операция учитывается в доходах и расходах (переводы между своими картами не считаются). */
val Transaction.counts: Boolean get() = category != Categories.OWN
val Transaction.isIncome: Boolean get() = type == TxType.INCOME.name
