package kz.koshelek.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kz.koshelek.app.data.RawNotification
import kz.koshelek.app.data.Repository

/** Все уведомления от банков, которые увидело приложение, и что с ними сделано. */
@Composable
fun LogScreen(log: List<RawNotification>, repo: Repository, modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                "Здесь видны все уведомления банков. Если операция «не распознано», " +
                    "нажмите «Скопировать» и отправьте текст разработчику, чтобы научить приложение.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { copy(context, log.joinToString("\n\n") { it.asText() }) }) {
                    Text("Скопировать всё")
                }
                OutlinedButton(onClick = { scope.launch { repo.clearLog() } }) { Text("Очистить") }
            }
        }
        if (log.isEmpty()) item { Text("Пока ничего не пришло.") }
        items(log, key = { it.id }) { n ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${n.appName} · ${formatTime(n.timestamp, dateTimeFormat)}", style = MaterialTheme.typography.labelMedium)
                    Text(n.title, fontWeight = FontWeight.Medium)
                    Text(n.text)
                    Row {
                        Text(
                            n.result,
                            color = if (n.result == "не распознано") ExpenseColor else IncomeColor,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f).padding(top = 6.dp),
                        )
                        androidx.compose.material3.TextButton(onClick = { copy(context, n.asText()) }) { Text("Скопировать") }
                    }
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
