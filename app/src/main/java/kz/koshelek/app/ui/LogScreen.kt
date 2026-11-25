package kz.koshelek.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kz.koshelek.app.data.RawNotification
import kz.koshelek.app.data.Repository

/** Все уведомления от банков, которые увидело приложение, и что с ними сделано. */
@Composable
fun LogScreen(log: List<RawNotification>, repo: Repository, onBack: () -> Unit) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(AppIcons.Back, "Назад", tint = p.text) }
                Text("Журнал", color = p.text, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(
                "Здесь видны все уведомления банков. Если операция «не распознано», " +
                    "нажмите «Скопировать» и отправьте текст разработчику, чтобы научить приложение.",
                color = p.text2, fontSize = 14.sp,
            )
            Row(Modifier.padding(top = 4.dp)) {
                TextButton(onClick = { copy(context, log.joinToString("\n\n") { it.asText() }) }) { Text("Скопировать всё", color = p.primary) }
                TextButton(onClick = { scope.launch { repo.clearLog() } }) { Text("Очистить", color = p.text2) }
            }
        }
        if (log.isEmpty()) item { Text("Пока ничего не пришло.", color = p.text2) }
        items(log, key = { it.id }) { n ->
            PCard(Modifier.fillMaxWidth(), padding = 12.dp) {
                Text("${n.appName} · ${formatTime(n.timestamp, dateTimeFormat)}", color = p.text2, fontSize = 12.sp)
                Text(n.title, color = p.text, fontWeight = FontWeight.Medium)
                Text(n.text, color = p.text, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        n.result,
                        color = if (n.result == "не распознано") p.expense else p.income,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { copy(context, n.asText()) }) { Text("Скопировать", color = p.primary) }
                }
            }
        }
    }
}

private fun RawNotification.asText() = "[$appName / $packageName] $title | $text => $result"

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Кошелёк", text))
    Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
}
