package kz.koshelek.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import kz.koshelek.app.parser.Categories

/** Окружность в виде пути SVG. */
private fun c(cx: Float, cy: Float, r: Float) =
    "M${cx - r},${cy}a$r,$r 0 1,0 ${2 * r},0a$r,$r 0 1,0 ${-2 * r},0"

/** Контурный значок 24×24 из путей SVG (как в макетах). */
private fun icon(name: String, vararg paths: String, width: Float = 2f): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach { d ->
            addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

object AppIcons {
    val Home = icon("home", "M4 11l8-7 8 7v9H4z")
    val Pie = icon("pie", "M12 3a9 9 0 1 0 9 9h-9z", "M15 3.5A9 9 0 0 1 20.5 9H15z")
    val Grid = icon("grid", c(7f, 7f, 3f), c(17f, 7f, 3f), c(7f, 17f, 3f), c(17f, 17f, 3f))
    val Chart = icon("chart", "M4 19V9M10 19V5M16 19v-7M22 19H2")
    val Plus = icon("plus", "M12 5v14M5 12h14", width = 2.6f)
    val Cart = icon("cart", "M3 4h2l2.4 10.2a1 1 0 0 0 1 .8h8.8a1 1 0 0 0 1-.8L20 7H6", c(9f, 19f, 1.5f), c(17f, 19f, 1.5f))
    val Coffee = icon("coffee", "M4 8h13v5a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5z", "M17 10h1.5a2.5 2.5 0 0 1 0 5H17", "M8 3v2M12 3v2")
    val Car = icon("car", "M5 16v-5l2-5h10l2 5v5", "M3 16h18v2H3z", c(7.5f, 13f, 1f), c(16.5f, 13f, 1f))
    val Bag = icon("bag", "M5 8h14l-1 12H6z", "M9 8V6a3 3 0 0 1 6 0v2")
    val Wifi = icon("wifi", "M5 9a10 10 0 0 1 14 0", "M8 12.5a6 6 0 0 1 8 0", c(12f, 16f, 1.2f))
    val Heart = icon("heart", "M12 20s-7-4.5-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.5-7 10-7 10z")
    val Play = icon("play", c(12f, 12f, 9f), "M10 8.5l5 3.5-5 3.5z")
    val Bus = icon("bus", "M6 4h12a1 1 0 0 1 1 1v12H5V5a1 1 0 0 1 1-1z", "M5 11h14", "M8 20v-3M16 20v-3")
    val Transfer = icon("transfer", "M7 7h11l-3-3M17 17H6l3 3")
    val Refresh = icon("refresh", "M20 12a8 8 0 1 1-3-6.2", "M20 4v5h-5")
    val Briefcase = icon("briefcase", "M5 7h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z", "M8 7V5h8v2")
    val Down = icon("down", "M12 5v14M6 13l6 6 6-6", width = 2.4f)
    val Up = icon("up", "M12 19V5M6 11l6-6 6 6", width = 2.4f)
    val Dots = icon("dots", c(6f, 12f, 1.3f), c(12f, 12f, 1.3f), c(18f, 12f, 1.3f))
    val Wallet = icon("wallet", "M5 6h14a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z", "M16 12.5h2")
    val Target = icon("target", c(12f, 12f, 8f), c(12f, 12f, 4f), c(12f, 12f, 0.6f))
    val Gear = icon("gear", c(12f, 12f, 3.2f), "M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3M5.3 5.3l2.1 2.1M16.6 16.6l2.1 2.1M5.3 18.7l2.1-2.1M16.6 7.4l2.1-2.1")
    val Search = icon("search", c(11f, 11f, 7f), "M20 20l-4-4")
    val Back = icon("back", "M15 6l-6 6 6 6")
    val Left = icon("left", "M15 6l-6 6 6 6")
    val Right = icon("right", "M9 6l6 6-6 6")
    val Bell = icon("bell", "M6 16v-5a6 6 0 0 1 12 0v5l2 2H4z", "M10 20a2 2 0 0 0 4 0")
    val Trash = icon("trash", "M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3")
    val Eye = icon("eye", "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z", c(12f, 12f, 3f))
    val Home2 = icon("house", "M4 11l8-7 8 7v9H4z", "M10 20v-5h4v5")
    val PaletteIcon = icon("palette", c(12f, 12f, 9f), c(8f, 10f, 1f), c(12f, 7.5f, 1f), c(16f, 10f, 1f))
}

/** Значок категории. */
fun iconOf(category: String) = when (category) {
    "Продукты" -> AppIcons.Cart
    "Кафе и рестораны" -> AppIcons.Coffee
    "Такси" -> AppIcons.Car
    "Транспорт" -> AppIcons.Bus
    "Покупки" -> AppIcons.Bag
    "Связь и интернет" -> AppIcons.Wifi
    "Коммунальные" -> AppIcons.Home2
    "Здоровье" -> AppIcons.Heart
    "Развлечения" -> AppIcons.Play
    Categories.TRANSFERS, Categories.OWN -> AppIcons.Transfer
    "Кешбэк" -> AppIcons.Refresh
    "Зарплата" -> AppIcons.Briefcase
    Categories.INCOME -> AppIcons.Down
    else -> AppIcons.Dots
}

/** Короткое имя категории для подписи под кружком. */
fun shortName(category: String) = when (category) {
    "Кафе и рестораны" -> "Кафе"
    "Связь и интернет" -> "Связь"
    "Коммунальные" -> "Коммуналка"
    else -> category
}
