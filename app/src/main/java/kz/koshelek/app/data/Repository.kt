package kz.koshelek.app.data

import android.content.Context
import kz.koshelek.app.parser.NotificationParser
import kz.koshelek.app.parser.TxType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

private val lock = Mutex()

class Repository(context: Context) {
    private val db = AppDatabase.get(context)
    val transactions = db.transactions().all()
    val rawNotifications = db.rawNotifications().latest()

    /**
     * Обрабатывает уведомление банка: распознаёт, проверяет на дубликаты и сохраняет.
     *
     * Дубликаты: при оплате через Samsung Pay приходят два уведомления (от Wallet и от банка)
     * на одну и ту же сумму. Они склеиваются в одну операцию, банк берётся из уведомления банка.
     * Повтор того же самого текста от того же банка в течение 10 минут тоже пропускается.
     */
    suspend fun handleNotification(
        packageName: String, appName: String, bank: String, title: String, text: String, time: Long,
    ) = lock.withLock {
        val parsed = NotificationParser.parse(title, text)
        val raw = "$title | $text"
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
                    // Склеиваем: "Kaspi · Samsung Pay", магазин берём из уведомления банка
                    val realBank = if (bank == NotificationParser.SAMSUNG) pair.bank else bank
                    val merchant = if (bank == NotificationParser.SAMSUNG) pair.merchant else parsed.merchant
                    val category = if (bank == NotificationParser.SAMSUNG) pair.category else parsed.category
                    db.transactions().update(
                        pair.copy(bank = "$realBank · Samsung Pay", merchant = merchant, category = category)
                    )
                    "объединено с Samsung Pay"
                }
                else -> {
                    db.transactions().insert(
                        Transaction(
                            amount = parsed.amount, type = type, bank = bank, merchant = parsed.merchant,
                            category = parsed.category, timestamp = time, source = "AUTO", rawText = raw,
                        )
                    )
                    if (parsed.type == TxType.INCOME) "добавлено: поступление" else "добавлено: списание"
                }
            }
        }
        db.rawNotifications().insert(RawNotification(0, packageName, appName, title, text, time, result))
    }

    private fun isWallet(bank: String) = bank == NotificationParser.SAMSUNG

    suspend fun addManual(amount: Double, type: TxType, merchant: String, category: String, time: Long) {
        db.transactions().insert(
            Transaction(
                amount = amount, type = type.name, bank = "Вручную", merchant = merchant,
                category = category, timestamp = time, source = "MANUAL",
            )
        )
    }

    suspend fun update(t: Transaction) = db.transactions().update(t)
    suspend fun delete(id: Long) = db.transactions().delete(id)
    suspend fun clearLog() = db.rawNotifications().clear()
}
