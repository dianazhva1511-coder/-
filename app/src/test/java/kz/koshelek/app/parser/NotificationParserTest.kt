package kz.koshelek.app.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationParserTest {

    private fun p(title: String, text: String) = NotificationParser.parse(title, text)

    @Test fun kaspiPurchase() {
        val r = p("Kaspi.kz", "Покупка 5 400 ₸ в MAGNUM CASH&CARRY. Доступно 120 350,25 ₸")!!
        assertEquals(5400.0, r.amount, 0.001)
        assertEquals(TxType.EXPENSE, r.type)
        assertEquals("MAGNUM CASH&CARRY", r.merchant)
        assertEquals("Продукты", r.category)
    }

    @Test fun kaspiIncomingTransfer() {
        val r = p("Kaspi.kz", "Вам поступил перевод 15 000 ₸ от Айгерим Н.")!!
        assertEquals(15000.0, r.amount, 0.001)
        assertEquals(TxType.INCOME, r.type)
        assertEquals("Айгерим Н", r.merchant)
    }

    @Test fun kaspiOutgoingTransfer() {
        val r = p("Перевод", "Перевод 3 000 ₸ отправлен Ерлан С. Доступно 50 000 ₸")!!
        assertEquals(3000.0, r.amount, 0.001)
        assertEquals(TxType.EXPENSE, r.type)
    }

    @Test fun halykSms() {
        val r = p("Halyk Bank", "Оплата: 1 250,50 KZT YANDEX.GO Almaty. Карта *1234. Остаток: 80 000,00 KZT")!!
        assertEquals(1250.50, r.amount, 0.001)
        assertEquals(TxType.EXPENSE, r.type)
        assertEquals("Такси", r.category)
    }

    @Test fun forteIncome() {
        val r = p("ForteBank", "Зачисление 250 000.00 KZT. Заработная плата. Баланс: 260 000.00 KZT")!!
        assertEquals(250000.0, r.amount, 0.001)
        assertEquals(TxType.INCOME, r.type)
    }

    @Test fun samsungWallet() {
        val r = p("Samsung Wallet", "Оплачено 2 100 ₸ в Coffee Boom")!!
        assertEquals(2100.0, r.amount, 0.001)
        assertEquals(TxType.EXPENSE, r.type)
        assertEquals("Кафе и рестораны", r.category)
    }

    @Test fun refundIsIncome() {
        val r = p("Kaspi.kz", "Возврат покупки 4 990 ₸ WILDBERRIES")!!
        assertEquals(TxType.INCOME, r.type)
    }

    @Test fun signedAmounts() {
        assertEquals(TxType.INCOME, p("Kaspi Gold", "+7 000 ₸ Пополнение")!!.type)
        assertEquals(TxType.EXPENSE, p("Kaspi Gold", "-900 ₸ ONAY")!!.type)
    }

    @Test fun purchaseWithCashbackStaysExpense() {
        val r = p("Kaspi.kz", "Покупка 10 000 ₸ в Technodom. Кешбэк 100 ₸")!!
        assertEquals(TxType.EXPENSE, r.type)
        assertEquals(10000.0, r.amount, 0.001)
    }

    @Test fun otpIgnored() {
        assertNull(p("Kaspi.kz", "Код подтверждения 1234. Не сообщайте никому. Сумма 5 000 ₸"))
    }

    @Test fun noAmountIgnored() {
        assertNull(p("Halyk", "Обновите приложение до новой версии"))
    }

    @Test fun detectBank() {
        assertEquals("Kaspi", NotificationParser.detectBank("kz.kaspi.mobile", "Kaspi.kz", ""))
        assertEquals("Halyk", NotificationParser.detectBank("com.samsung.android.messaging", "Сообщения", "Halyk Bank"))
        assertEquals("Forte", NotificationParser.detectBank("kz.some.unknown", "ForteBank", ""))
        assertNull(NotificationParser.detectBank("org.telegram.messenger", "Telegram", "Kaspi"))
    }

    @Test fun qrPayments() {
        val a = p("Kaspi.kz", "Оплата по QR 2 500 ₸ Shawarma House. Доступно 40 000 ₸")!!
        assertEquals(TxType.EXPENSE, a.type); assertEquals(2500.0, a.amount, 0.001)
        val b = p("Halyk", "Kaspi QR: покупка 1 800 KZT, ИП Асанов")!!
        assertEquals(TxType.EXPENSE, b.type)
        val c = p("ForteBank", "Оплата QR 3 200,00 KZT. Остаток 10 000,00 KZT")!!
        assertEquals(3200.0, c.amount, 0.001)
    }

    @Test fun transfersBothWays() {
        assertEquals(TxType.EXPENSE, p("Kaspi.kz", "Перевод 5 000 ₸ Ерлан С.")!!.type)
        assertEquals(TxType.EXPENSE, p("Halyk", "Перевод на карту 10 000 KZT выполнен")!!.type)
        assertEquals(TxType.EXPENSE, p("ForteBank", "Списание 7 000 KZT: перевод по номеру телефона")!!.type)
        assertEquals(TxType.INCOME, p("Kaspi.kz", "Пополнение 20 000 ₸ от Динара К.")!!.type)
        assertEquals(TxType.INCOME, p("Halyk", "Зачисление перевода 12 000,00 KZT")!!.type)
        assertEquals(TxType.INCOME, p("ForteBank", "Входящий перевод 3 000 KZT")!!.type)
    }
}
