package kz.koshelek.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kz.koshelek.app.R
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import kz.koshelek.app.parser.Categories

enum class AppTheme(val title: String) {
    LIGHT("Светлая"),
    BLACK("Чёрная"),
    SUNSET("Закат"),
}

/** Все цвета одной темы. */
data class Palette(
    val dark: Boolean,
    val bg: Brush,
    val bgSolid: Color,
    val card: Color,
    val cardBorder: Color,
    val divider: Color,
    val text: Color,
    val text2: Color,
    val primary: Color,
    val onPrimary: Color,
    val fab: Brush,
    val income: Color,
    val expense: Color,
    val savings: Color,
    val warning: Color,
    val hero: Brush,
    val onHero: Color,
    val heroText2: Color,
    val heroInner: Color,
    val nav: Color,
    val chip: Color,
    val categoryColors: List<Color>,
)

private val Light = Palette(
    dark = false,
    bg = SolidColor(Color(0xFFF3F6F5)), bgSolid = Color(0xFFF3F6F5),
    card = Color.White, cardBorder = Color.Transparent, divider = Color(0xFFEEF2F0),
    text = Color(0xFF15201C), text2 = Color(0xFF55605B),
    primary = Color(0xFF127A63), onPrimary = Color.White, fab = SolidColor(Color(0xFF127A63)),
    income = Color(0xFF1F7A45), expense = Color(0xFFC0392B), savings = Color(0xFF2F63A3), warning = Color(0xFF9A6300),
    hero = SolidColor(Color(0xFF127A63)), onHero = Color.White, heroText2 = Color(0xFFD6F0E8),
    heroInner = Color(0x1FFFFFFF),
    nav = Color.White, chip = Color(0xFFE7ECE9),
    categoryColors = listOf(
        Color(0xFFF59E0B), Color(0xFFEF5A3C), Color(0xFF3F7CC4), Color(0xFF8E5CC4), Color(0xFF2BA7A0),
        Color(0xFF1E6B52), Color(0xFFD6336C), Color(0xFF5B7DB8), Color(0xFF6B8E23), Color(0xFF7A7A7A),
        Color(0xFF22A05B),
    ),
)

private val Black = Palette(
    dark = true,
    bg = SolidColor(Color.Black), bgSolid = Color.Black,
    card = Color(0xFF1C1C1E), cardBorder = Color.Transparent, divider = Color(0xFF2C2C2E),
    text = Color.White, text2 = Color(0xFF8E8E93),
    primary = Color(0xFFA6FF00), onPrimary = Color.Black, fab = SolidColor(Color(0xFFA6FF00)),
    income = Color(0xFFA6FF00), expense = Color(0xFFFA114F), savings = Color(0xFF00E5F0), warning = Color(0xFFFFD60A),
    hero = SolidColor(Color(0xFF1C1C1E)), onHero = Color.White, heroText2 = Color(0xFF8E8E93),
    heroInner = Color(0xFF2C2C2E),
    nav = Color(0xFF0D0D0D), chip = Color(0xFF2C2C2E),
    categoryColors = listOf(
        Color(0xFFFF9500), Color(0xFFFFD60A), Color(0xFF0A84FF), Color(0xFFFA114F), Color(0xFF64D2FF),
        Color(0xFF30D158), Color(0xFFFF375F), Color(0xFFBF5AF2), Color(0xFF5E5CE6), Color(0xFF8E8E93),
        Color(0xFFA6FF00),
    ),
)

private val Sunset = Palette(
    dark = true,
    bg = Brush.verticalGradient(listOf(Color(0xFF2A0F45), Color(0xFF170A2B), Color(0xFF0E0620))),
    bgSolid = Color(0xFF170A2B),
    card = Color(0x14FFFFFF), cardBorder = Color(0x1AFFFFFF), divider = Color(0x1AFFFFFF),
    text = Color.White, text2 = Color(0xFFC8B6E2),
    primary = Color(0xFFFF4F9A), onPrimary = Color.White,
    fab = Brush.linearGradient(listOf(Color(0xFFFF9A3C), Color(0xFFFF4F9A))),
    income = Color(0xFF3FF0E0), expense = Color(0xFFFF7DB6), savings = Color(0xFFFFB36B), warning = Color(0xFFFFB36B),
    hero = Brush.linearGradient(listOf(Color(0xFFC9520F), Color(0xFFC42A6C), Color(0xFF5B28B8))),
    onHero = Color.White, heroText2 = Color(0xFFFFE6F1), heroInner = Color(0x59170A2B),
    nav = Color(0xEB0E0620), chip = Color(0x22FFFFFF),
    categoryColors = listOf(
        Color(0xFFFF9A3C), Color(0xFFFF4F9A), Color(0xFF3FF0E0), Color(0xFFB57BFF), Color(0xFFFFB36B),
        Color(0xFF5EE6A8), Color(0xFFFF6F91), Color(0xFF7B9CFF), Color(0xFFFFD86B), Color(0xFFC8B6E2),
        Color(0xFF3FF0E0),
    ),
)

fun paletteOf(t: AppTheme) = when (t) {
    AppTheme.LIGHT -> Light
    AppTheme.BLACK -> Black
    AppTheme.SUNSET -> Sunset
}

val LocalPalette = staticCompositionLocalOf { Black }

@Composable
fun KoshelekTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val p = paletteOf(theme)
    val scheme = if (p.dark) {
        darkColorScheme(
            primary = p.primary, onPrimary = p.onPrimary, background = p.bgSolid, surface = Color(0xFF1C1C1E),
            onSurface = p.text, onBackground = p.text, surfaceVariant = p.chip, onSurfaceVariant = p.text2,
            surfaceContainerHigh = Color(0xFF1C1C1E), surfaceContainerLow = Color(0xFF1C1C1E),
            secondaryContainer = p.chip, onSecondaryContainer = p.text,
        )
    } else {
        lightColorScheme(
            primary = p.primary, onPrimary = p.onPrimary, background = p.bgSolid, surface = Color.White,
            onSurface = p.text, onBackground = p.text, surfaceVariant = p.chip, onSurfaceVariant = p.text2,
            secondaryContainer = Color(0xFFD6F0E8), onSecondaryContainer = p.text,
        )
    }
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}

/** Индекс цвета категории в палитре темы. */
private val categoryIndex = mapOf(
    "Продукты" to 0, "Кафе и рестораны" to 1, "Такси" to 2, "Покупки" to 3, "Связь и интернет" to 4,
    "Коммунальные" to 5, "Здоровье" to 6, "Транспорт" to 7, "Развлечения" to 8, Categories.OTHER to 9,
    Categories.TRANSFERS to 7, Categories.OWN to 9, Categories.INCOME to 10, "Зарплата" to 10, "Кешбэк" to 4,
)

fun Palette.colorOf(category: String): Color =
    kz.koshelek.app.data.AppSettings.categoryColors[category]?.let { Color(it) } ?:
    categoryColors[(categoryIndex[category] ?: (category.hashCode() and 0x7fffffff)) % categoryColors.size]

/** Цвет значка на заливке категории: тёмный на светлых неоновых, белый на насыщенных. */
fun onColor(c: Color): Color {
    val l = 0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue
    return if (l > 0.45f) Color.Black else Color.White
}

/** Цвета банков для значков-кружков. */
fun Palette.bankColor(bank: String): Color {
    val b = bank.lowercase()
    return when {
        "kaspi" in b -> if (dark) Color(0xFFFA114F) else Color(0xFFE4002B)
        "halyk" in b -> if (dark) Color(0xFF30D158) else Color(0xFF00805F)
        "forte" in b -> if (dark) Color(0xFFBF5AF2) else Color(0xFFA6195A)
        "samsung" in b -> Color(0xFF0A84FF)
        else -> categoryColors[9]
    }
}

/** Шрифт приложения: Unbounded (есть кириллица и знак ₸). */
val AppFont = FontFamily(
    Font(R.font.unbounded_regular, FontWeight.Normal),
    Font(R.font.unbounded_regular, FontWeight.Medium),
    Font(R.font.unbounded_semibold, FontWeight.SemiBold),
    Font(R.font.unbounded_bold, FontWeight.Bold),
    Font(R.font.unbounded_extrabold, FontWeight.ExtraBold),
)

private val AppTypography = Typography().run {
    Typography(
        displayLarge = displayLarge.copy(fontFamily = AppFont), displayMedium = displayMedium.copy(fontFamily = AppFont),
        displaySmall = displaySmall.copy(fontFamily = AppFont), headlineLarge = headlineLarge.copy(fontFamily = AppFont),
        headlineMedium = headlineMedium.copy(fontFamily = AppFont), headlineSmall = headlineSmall.copy(fontFamily = AppFont),
        titleLarge = titleLarge.copy(fontFamily = AppFont), titleMedium = titleMedium.copy(fontFamily = AppFont),
        titleSmall = titleSmall.copy(fontFamily = AppFont), bodyLarge = bodyLarge.copy(fontFamily = AppFont),
        bodyMedium = bodyMedium.copy(fontFamily = AppFont), bodySmall = bodySmall.copy(fontFamily = AppFont),
        labelLarge = labelLarge.copy(fontFamily = AppFont), labelMedium = labelMedium.copy(fontFamily = AppFont),
        labelSmall = labelSmall.copy(fontFamily = AppFont),
    )
}
