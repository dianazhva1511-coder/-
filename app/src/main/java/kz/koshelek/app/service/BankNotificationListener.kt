package kz.koshelek.app.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kz.koshelek.app.data.Repository
import kz.koshelek.app.parser.NotificationParser

/**
 * Читает уведомления, которые приходят на телефон. Обрабатываются только уведомления
 * Kaspi, Halyk, Forte, Samsung Wallet и SMS от этих банков; все остальные сразу пропускаются
 * и никуда не сохраняются. Данные не покидают телефон.
 */
class BankNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repo by lazy { Repository(applicationContext) }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val n = sbn.notification ?: return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = n.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        val label = appLabel(sbn.packageName)
        val bank = NotificationParser.detectBank(sbn.packageName, label, title) ?: return
        val time = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()

        scope.launch {
            repo.handleNotification(sbn.packageName, label, bank, title, text, time)
        }
    }

    private fun appLabel(pkg: String): String = try {
        val pm = packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA)).toString()
    } catch (e: Exception) {
        pkg
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
