package kz.koshelek.app.ui

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kz.koshelek.app.service.BankNotificationListener
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val Ru: Locale = Locale.forLanguageTag("ru")
val IncomeColor = Color(0xFF2E9E5B)
val ExpenseColor = Color(0xFFD9534F)

val ChartColors = listOf(
    Color(0xFF1E6B52), Color(0xFFF2B233), Color(0xFF3F7CC4), Color(0xFFD9534F), Color(0xFF8E5CC4),
    Color(0xFF2BA7A0), Color(0xFFE07B39), Color(0xFF6B8E23), Color(0xFFB0577F), Color(0xFF7A7A7A),
)

@Composable
fun KoshelekTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFF6FD3AE), secondary = Color(0xFFF2B233))
    } else {
        lightColorScheme(primary = Color(0xFF1E6B52), secondary = Color(0xFFF2B233))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

fun money(v: Double): String =
    if (v % 1.0 == 0.0) String.format(Ru, "%,.0f ₸", v) else String.format(Ru, "%,.2f ₸", v)

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM, EEEE", Ru)
val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Ru)
val dateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM HH:mm", Ru)
val monthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Ru)

fun formatTime(ms: Long, f: DateTimeFormatter): String =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(f)

/** Включён ли доступ к уведомлениям для нашего приложения. */
fun hasNotificationAccess(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
    val me = ComponentName(context, BankNotificationListener::class.java)
    return flat.split(":").any { ComponentName.unflattenFromString(it) == me }
}

/** Статус доступа к уведомлениям, обновляется при возвращении в приложение. */
@Composable
fun rememberNotificationAccess(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var granted by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(hasNotificationAccess(context)) }
    androidx.compose.runtime.DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) granted = hasNotificationAccess(context)
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return granted
}
