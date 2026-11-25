package kz.koshelek.app

import kz.koshelek.app.data.AppSettings
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kz.koshelek.app.data.Goal
import kz.koshelek.app.data.Repository
import kz.koshelek.app.data.Transaction
import kz.koshelek.app.ui.AddSheet
import kz.koshelek.app.ui.AmountDialog
import kz.koshelek.app.ui.AppIcons
import kz.koshelek.app.ui.AppTheme
import kz.koshelek.app.ui.CardScreen
import kz.koshelek.app.ui.CategoriesScreen
import kz.koshelek.app.ui.EditTransactionDialog
import kz.koshelek.app.ui.GoalDialog
import kz.koshelek.app.ui.HomeScreen
import kz.koshelek.app.ui.KoshelekTheme
import kz.koshelek.app.ui.LocalPalette
import kz.koshelek.app.ui.LogScreen
import kz.koshelek.app.ui.OperationsScreen
import kz.koshelek.app.ui.Prefs
import kz.koshelek.app.ui.ReportScreen
import kz.koshelek.app.ui.SettingsScreen
import kz.koshelek.app.ui.StatsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        AppSettings.init(this)
        applyBars(prefs.theme)
        setContent {
            var theme by remember { mutableStateOf(prefs.theme) }
            LaunchedEffect(theme) { applyBars(theme) }
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                    density.density, density.fontScale * AppSettings.fontScale,
                ),
            ) {
                KoshelekTheme(theme) {
                    App(prefs, theme) { theme = it; prefs.theme = it }
                }
            }
        }
    }

    private fun applyBars(theme: AppTheme) {
        val t = android.graphics.Color.TRANSPARENT
        if (theme == AppTheme.LIGHT) enableEdgeToEdge(SystemBarStyle.light(t, t), SystemBarStyle.light(t, t))
        else enableEdgeToEdge(SystemBarStyle.dark(t), SystemBarStyle.dark(t))
    }
}

private enum class Tab { HOME, STATS, CATEGORIES, REPORT }
private enum class Sub { NONE, OPERATIONS, SETTINGS, LOG, CARD }

/** Какой диалог открыт для ввода суммы. */
private sealed class AmountTarget {
    data class Limit(val category: String, val current: Double?) : AmountTarget()
    data class BankBalance(val bank: String, val current: Double?) : AmountTarget()
    data object Budget : AmountTarget()
}

@Composable
fun App(prefs: Prefs, theme: AppTheme, onTheme: (AppTheme) -> Unit) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val repo = remember { Repository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    val transactions by repo.transactions.collectAsState(initial = emptyList())
    val log by repo.rawNotifications.collectAsState(initial = emptyList())
    val goals by repo.goals.collectAsState(initial = emptyList())
    val limits by repo.limits.collectAsState(initial = emptyList())
    val balances by repo.balances.collectAsState(initial = emptyList())
    var budget by remember { mutableDoubleStateOf(prefs.budget) }

    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var sub by rememberSaveable { mutableStateOf(Sub.NONE) }
    var card by rememberSaveable { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Transaction?>(null) }
    var goalDialog by remember { mutableStateOf<Goal?>(null) }
    var goalDialogOpen by remember { mutableStateOf(false) }
    var amountTarget by remember { mutableStateOf<AmountTarget?>(null) }

    BackHandler(enabled = sub != Sub.NONE) { sub = if (sub == Sub.LOG) Sub.SETTINGS else Sub.NONE }
    BackHandler(enabled = sub == Sub.NONE && tab != Tab.HOME) { tab = Tab.HOME }

    Box(Modifier.fillMaxSize().background(p.bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth().statusBarsPadding()) {
                when (sub) {
                    Sub.OPERATIONS -> OperationsScreen(transactions, onBack = { sub = Sub.NONE }, onOpen = { editing = it })
                    Sub.SETTINGS -> SettingsScreen(
                        theme, budget, onTheme, onBudget = { amountTarget = AmountTarget.Budget },
                        onOpenLog = { sub = Sub.LOG }, onBack = { sub = Sub.NONE }, repo = repo,
                    )
                    Sub.LOG -> LogScreen(log, repo, onBack = { sub = Sub.SETTINGS })
                    Sub.CARD -> {
                        val bal = balances.firstOrNull { it.bank == card }
                        CardScreen(
                            card, bal, transactions, onBack = { sub = Sub.NONE }, onOpen = { editing = it },
                            onEditBalance = { amountTarget = AmountTarget.BankBalance(card, bal?.amount) },
                        )
                    }
                    Sub.NONE -> when (tab) {
                        Tab.HOME -> HomeScreen(
                            transactions, balances, goals, budget,
                            onOpenOperations = { sub = Sub.OPERATIONS },
                            onOpenSettings = { sub = Sub.SETTINGS },
                            onOpenTransaction = { editing = it },
                            onGoalClick = { goalDialog = it; goalDialogOpen = true },
                            onSetBudget = { amountTarget = AmountTarget.Budget },
                            onOpenCard = { card = it; sub = Sub.CARD },
                        )
                        Tab.STATS -> StatsScreen(transactions)
                        Tab.CATEGORIES -> CategoriesScreen(
                            transactions, balances, limits, budget,
                            onEditLimit = { c, cur -> amountTarget = AmountTarget.Limit(c, cur) },
                            onEditBalance = { b, cur -> amountTarget = AmountTarget.BankBalance(b, cur) },
                            onEditBudget = { amountTarget = AmountTarget.Budget },
                        )
                        Tab.REPORT -> ReportScreen(transactions)
                    }
                }
            }
            BottomBar(if (sub == Sub.NONE) tab else null, onTab = { tab = it; sub = Sub.NONE }, onAdd = { adding = true })
        }
    }

    if (adding) {
        AddSheet(onDismiss = { adding = false }) { amount, type, note, category, wallet ->
            scope.launch { repo.addManual(amount, type, note, category, wallet, System.currentTimeMillis()) }
            adding = false
        }
    }
    editing?.let { t ->
        EditTransactionDialog(
            t,
            onDismiss = { editing = null },
            onSave = { scope.launch { repo.update(it) }; editing = null },
            onDelete = { scope.launch { repo.delete(t.id) }; editing = null },
        )
    }
    if (goalDialogOpen) {
        GoalDialog(
            goalDialog,
            onDismiss = { goalDialogOpen = false },
            onCreate = { n, tgt -> scope.launch { repo.addGoal(n, tgt) }; goalDialogOpen = false },
            onUpdate = { g -> scope.launch { repo.updateGoal(g) }; goalDialogOpen = false },
            onDelete = { g -> scope.launch { repo.deleteGoal(g.id) }; goalDialogOpen = false },
        )
    }
    amountTarget?.let { target ->
        when (target) {
            is AmountTarget.Limit -> AmountDialog(
                "Лимит: ${target.category}", "Сколько можно тратить на эту категорию в месяц.", target.current,
                onDismiss = { amountTarget = null },
            ) { v -> scope.launch { repo.setLimit(target.category, v) }; amountTarget = null }
            is AmountTarget.BankBalance -> AmountDialog(
                "Остаток: ${target.bank}", "Обычно обновляется сам из уведомлений банка.", target.current,
                onDismiss = { amountTarget = null },
            ) { v -> if (v != null) scope.launch { repo.setBalance(target.bank, v) }; amountTarget = null }
            AmountTarget.Budget -> AmountDialog(
                "Бюджет на месяц", "Сколько вы планируете тратить за месяц. Кольцо на главной покажет, сколько осталось.",
                budget.takeIf { it > 0 }, onDismiss = { amountTarget = null },
            ) { v -> budget = v ?: 0.0; prefs.budget = budget; amountTarget = null }
        }
    }
}

@Composable
private fun BottomBar(tab: Tab?, onTab: (Tab) -> Unit, onAdd: () -> Unit) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(top = 24.dp).background(p.nav).navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                NavItem("Главная", AppIcons.Home, tab == Tab.HOME) { onTab(Tab.HOME) }
                NavItem("Статистика", AppIcons.Pie, tab == Tab.STATS) { onTab(Tab.STATS) }
                Spacer(Modifier.weight(1f))
                NavItem("Категории", AppIcons.Grid, tab == Tab.CATEGORIES) { onTab(Tab.CATEGORIES) }
                NavItem("Отчёт", AppIcons.Chart, tab == Tab.REPORT) { onTab(Tab.REPORT) }
            }
        }
        Box(
            Modifier.align(Alignment.TopCenter).size(64.dp).clip(CircleShape).background(p.fab).clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) { Icon(AppIcons.Plus, "Добавить операцию", tint = p.onPrimary, modifier = Modifier.size(30.dp)) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val color = if (selected) p.primary else p.text2
    Column(
        Modifier.weight(1f).fillMaxSize().clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
    }
}
