package kz.koshelek.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.koshelek.app.parser.NotificationParser
import kz.koshelek.app.parser.TxType

@Composable
fun SettingsScreen(
    theme: AppTheme,
    budget: Double,
    onTheme: (AppTheme) -> Unit,
    onBudget: () -> Unit,
    onOpenLog: () -> Unit,
    onBack: () -> Unit,
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val hasAccess = rememberNotificationAccess()

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(AppIcons.Back, "Назад", tint = p.text) }
            Text("Настройки", color = p.text, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        }

        Text("Тема", color = p.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTheme.entries.forEach { t ->
                val tp = paletteOf(t)
                val sel = t == theme
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                        .border(if (sel) 2.dp else 1.dp, if (sel) p.primary else p.divider, RoundedCornerShape(18.dp))
                        .clickable { onTheme(t) }.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(12.dp)).background(tp.bg), contentAlignment = Alignment.Center) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(tp.expense, tp.income, tp.savings).forEach { c -> Box(Modifier.size(14.dp).clip(CircleShape).background(c)) }
                        }
                    }
                    Text(t.title, color = p.text, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
                }
            }
        }

        PCard(onClick = onBudget) {
            Text("Бюджет на месяц", color = p.text, fontWeight = FontWeight.Bold)
            Text(if (budget > 0) money(budget) else "Не задан. Нажмите, чтобы задать.", color = p.text2, fontSize = 14.sp)
        }

        PCard {
            Text("1. Доступ к уведомлениям", color = p.text, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (hasAccess) "Включён. Операции добавляются автоматически." else "Выключен. Нажмите кнопку и включите «Кошелёк» в списке.",
                color = if (hasAccess) p.income else p.expense, fontSize = 14.sp,
            )
            Spacer(Modifier.height(8.dp))
            ActionButton("Открыть настройки уведомлений") {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            Text(
                "Если переключатель серый: Настройки → Приложения → Кошелёк → ⋮ → «Разрешить ограниченные настройки», затем повторите.",
                color = p.text2, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
            )
        }

        PCard {
            Text("2. Не выключать в фоне", color = p.text, fontWeight = FontWeight.Bold)
            Text("Выберите для «Кошелька» батарею «Без ограничений».", color = p.text2, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            ActionButton("Открыть настройки приложения") {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${context.packageName}"))
                )
            }
        }

        PCard(onClick = onOpenLog) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Bell, null, tint = p.text)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Журнал уведомлений", color = p.text, fontWeight = FontWeight.Bold)
                    Text("Что пришло от банков и как распознано", color = p.text2, fontSize = 13.sp)
                }
                Icon(AppIcons.Right, null, tint = p.text2)
            }
        }

        PCard {
            Text("Проверить распознавание", color = p.text, fontWeight = FontWeight.Bold)
            var sample by remember { mutableStateOf("") }
            OutlinedTextField(sample, { sample = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Текст уведомления") })
            if (sample.isNotBlank()) {
                val r = NotificationParser.parse("", sample)
                Text(
                    if (r == null) "Не распознано"
                    else "${if (r.type == TxType.INCOME) "Поступление" else "Списание"} ${money(r.amount)} · ${r.merchant} · ${r.category}",
                    color = p.text, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        PCard {
            Text("О приложении", color = p.text, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Кошелёк, версия ${appVersion(context)}", color = p.text2, fontSize = 14.sp)
            Text("Автор: Adilbek Kairbekov", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        Text(
            "Все данные хранятся только на этом телефоне. У приложения нет доступа в интернет.",
            color = p.text2, fontSize = 12.sp, modifier = Modifier.padding(4.dp),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier.clip(RoundedCornerShape(14.dp)).background(p.primary).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
    ) { Text(label, color = p.onPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
}

private fun appVersion(context: android.content.Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
