package kz.koshelek.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kz.koshelek.app.parser.NotificationParser
import kz.koshelek.app.parser.TxType

@Composable
fun SettingsScreen(modifier: Modifier) {
    val context = LocalContext.current
    val hasAccess = rememberNotificationAccess()

    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Доступ к уведомлениям", fontWeight = FontWeight.Bold)
                Text(
                    if (hasAccess) "✅ Включён. Операции добавляются автоматически."
                    else "❌ Выключен. Нажмите кнопку и включите «Кошелёк» в списке."
                )
                Button(onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }) { Text("Открыть настройки уведомлений") }
                Text(
                    "Если переключатель серый: Настройки → Приложения → Кошелёк → ⋮ (вверху справа) → " +
                        "«Разрешить ограниченные настройки», затем повторите.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2. Не выключать в фоне", fontWeight = FontWeight.Bold)
                Text("Samsung может усыплять приложения. Выберите для «Кошелька» режим батареи «Без ограничений».")
                OutlinedButton(onClick = {
                    val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(android.net.Uri.parse("package:${context.packageName}"))
                    context.startActivity(i)
                }) { Text("Открыть настройки приложения") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("3. Уведомления в банках", fontWeight = FontWeight.Bold)
                Text(
                    "В приложениях Kaspi, Halyk, Forte и Samsung Wallet должны быть включены push-уведомления " +
                        "об операциях. SMS от банков тоже распознаются."
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Проверить распознавание", fontWeight = FontWeight.Bold)
                Text("Вставьте текст уведомления, чтобы увидеть, как приложение его поймёт.", style = MaterialTheme.typography.bodySmall)
                var sample by remember { mutableStateOf("") }
                OutlinedTextField(sample, { sample = it }, Modifier.fillMaxWidth(), label = { Text("Текст уведомления") })
                if (sample.isNotBlank()) {
                    val r = NotificationParser.parse("", sample)
                    Text(
                        if (r == null) "Не распознано"
                        else "${if (r.type == TxType.INCOME) "Поступление" else "Списание"} ${money(r.amount)} · ${r.merchant} · ${r.category}",
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        Text(
            "Все данные хранятся только на этом телефоне. Приложение не отправляет их в интернет.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
