package kz.koshelek.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kz.koshelek.app.data.Repository
import kz.koshelek.app.ui.KoshelekTheme
import kz.koshelek.app.ui.LogScreen
import kz.koshelek.app.ui.OverviewScreen
import kz.koshelek.app.ui.SettingsScreen
import kz.koshelek.app.ui.TransactionsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KoshelekTheme { App() } }
    }
}

@Suppress("DEPRECATION")
private val tabs = listOf(
    "Обзор" to Icons.Filled.Home,
    "Операции" to Icons.Filled.List,
    "Журнал" to Icons.Filled.Notifications,
    "Настройки" to Icons.Filled.Settings,
)

@androidx.compose.runtime.Composable
fun App() {
    val context = LocalContext.current
    val repo = remember { Repository(context.applicationContext) }
    val transactions by repo.transactions.collectAsState(initial = emptyList())
    val log by repo.rawNotifications.collectAsState(initial = emptyList())
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, (label, icon) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        }
    ) { padding ->
        val m = Modifier.padding(padding)
        when (tab) {
            0 -> OverviewScreen(transactions, m, onOpenSettings = { tab = 3 })
            1 -> TransactionsScreen(transactions, repo, m)
            2 -> LogScreen(log, repo, m)
            else -> SettingsScreen(m)
        }
    }
}
