package kz.koshelek.app.ui

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kz.koshelek.app.service.BankNotificationListener
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

val Ru: Locale = Locale.forLanguageTag("ru")

fun money(v: Double): String {
    val a = abs(v)
    val s = if (a % 1.0 == 0.0) String.format(Ru, "%,.0f ₸", a) else String.format(Ru, "%,.2f ₸", a)
    return if (v < 0) "−$s" else s
}

/** Короткая запись крупных сумм: 2,48 млн ₸, 420 тыс ₸. */
fun moneyShort(v: Double): String = when {
    abs(v) >= 1_000_000 -> String.format(Ru, "%.2f млн ₸", v / 1_000_000)
    abs(v) >= 10_000 -> String.format(Ru, "%.0f тыс ₸", v / 1_000)
    else -> money(v)
}

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM, EEEE", Ru)
val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Ru)
val dateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM HH:mm", Ru)
val monthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Ru)
val monthShort: DateTimeFormatter = DateTimeFormatter.ofPattern("LLL", Ru)
val headerDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Ru)

fun formatTime(ms: Long, f: DateTimeFormatter): String =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(f)

fun String.cap(): String = replaceFirstChar { it.titlecase(Ru) }

/** Включён ли доступ к уведомлениям для нашего приложения. */
fun hasNotificationAccess(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
    val me = ComponentName(context, BankNotificationListener::class.java)
    return flat.split(":").any { ComponentName.unflattenFromString(it) == me }
}

/** Статус доступа к уведомлениям, обновляется при возвращении в приложение. */
@Composable
fun rememberNotificationAccess(): Boolean {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(hasNotificationAccess(context)) }
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) granted = hasNotificationAccess(context)
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return granted
}

/** Настройки приложения: тема и бюджет на месяц. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("koshelek", Context.MODE_PRIVATE)

    var theme: AppTheme
        get() = runCatching { AppTheme.valueOf(sp.getString("theme", null) ?: AppTheme.BLACK.name) }
            .getOrDefault(AppTheme.BLACK)
        set(v) = sp.edit().putString("theme", v.name).apply()

    var lastCrash: String?
        get() = sp.getString("lastCrash", null)
        set(v) = sp.edit().putString("lastCrash", v).apply()

    var budget: Double
        get() = sp.getFloat("budget", 0f).toDouble()
        set(v) = sp.edit().putFloat("budget", v.toFloat()).apply()
}
