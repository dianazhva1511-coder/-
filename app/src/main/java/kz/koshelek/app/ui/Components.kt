package kz.koshelek.app.ui

import kz.koshelek.app.data.AppSettings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.koshelek.app.data.Transaction
import kz.koshelek.app.data.isIncome
import java.time.YearMonth

/** Карточка в цветах темы. */
@Composable
fun PCard(modifier: Modifier = Modifier, padding: Dp = 16.dp, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(22.dp)
    var m = modifier.clip(shape).background(p.card)
    if (p.cardBorder != Color.Transparent) m = m.border(1.dp, p.cardBorder, shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(padding), content = content)
}

@Composable
fun SectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = p.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action, color = p.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onAction).padding(8.dp),
            )
        }
    }
}

/** Круглый значок с заливкой. */
@Composable
fun CircleIcon(icon: ImageVector, color: Color, size: Dp = 42.dp, contentDescription: String? = null) {
    Box(Modifier.size(size).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription, tint = onColor(color), modifier = Modifier.size(size * 0.5f))
    }
}

/** Кружок-буква банка. */
@Composable
fun BankBadge(bank: String, size: Dp = 42.dp) {
    val p = LocalPalette.current
    val color = p.bankColor(bank)
    Box(Modifier.size(size).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(AppSettings.walletName(bank).take(1).uppercase(), color = onColor(color), fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.42f).sp)
    }
}

/** Значок в кольце прогресса (для лимитов). */
@Composable
fun RingIcon(icon: ImageVector, color: Color, progress: Float?, size: Dp = 60.dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        if (progress != null) {
            Canvas(Modifier.size(size)) {
                val w = 4.dp.toPx()
                val inset = w / 2
                val s = Size(this.size.width - w, this.size.height - w)
                drawArc(color.copy(alpha = 0.25f), 0f, 360f, false, Offset(inset, inset), s, style = Stroke(w))
                drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(inset, inset), s, style = Stroke(w, cap = StrokeCap.Round))
            }
        }
        CircleIcon(icon, color, if (progress != null) size - 16.dp else size)
    }
}

/** Концентрические кольца, как кольца активности. */
@Composable
fun Rings(values: List<Pair<Float, Color>>, size: Dp, stroke: Dp) {
    Canvas(Modifier.size(size)) {
        val w = stroke.toPx()
        val gap = w * 0.25f
        values.forEachIndexed { i, (v, color) ->
            val inset = w / 2 + i * (w + gap)
            val s = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            if (s.width <= 0) return@forEachIndexed
            drawArc(color.copy(alpha = 0.22f), 0f, 360f, false, Offset(inset, inset), s, style = Stroke(w))
            drawArc(color, -90f, 360f * v.coerceIn(0f, 1f), false, Offset(inset, inset), s, style = Stroke(w, cap = StrokeCap.Round))
        }
    }
}

/** Кольцевая диаграмма. */
@Composable
fun Donut(slices: List<Pair<Double, Color>>, size: Dp, stroke: Dp, empty: Color) {
    val total = slices.sumOf { it.first }
    Canvas(Modifier.size(size)) {
        val w = stroke.toPx()
        val s = Size(this.size.width - w, this.size.height - w)
        val o = Offset(w / 2, w / 2)
        if (total <= 0) {
            drawArc(empty, 0f, 360f, false, o, s, style = Stroke(w))
            return@Canvas
        }
        var start = -90f
        val gap = if (slices.size > 1) 1.5f else 0f
        slices.forEach { (v, color) ->
            val sweep = (v / total * 360f).toFloat()
            drawArc(color, start + gap / 2, (sweep - gap).coerceAtLeast(0.5f), false, o, s, style = Stroke(w))
            start += sweep
        }
    }
}

/** Линейный график из двух рядов (доходы и расходы). */
@Composable
fun LineChart(series: List<Pair<List<Double>, Color>>, labels: List<String>, grid: Color, labelColor: Color, height: Dp = 180.dp) {
    val max = series.flatMap { it.first }.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    Column {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val n = labels.size
            val padX = 10.dp.toPx()
            val stepX = if (n > 1) (size.width - padX * 2) / (n - 1) else 0f
            val top = 10.dp.toPx()
            val h = size.height - top * 2
            for (i in 0..3) {
                val y = top + h * i / 3f
                drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
            }
            series.forEach { (values, color) ->
                val path = Path()
                values.forEachIndexed { i, v ->
                    val x = padX + stepX * i
                    val y = top + h - (v / max).toFloat() * h
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                values.forEachIndexed { i, v ->
                    drawCircle(color, 4.dp.toPx(), Offset(padX + stepX * i, top + h - (v / max).toFloat() * h))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { Text(it, color = labelColor, fontSize = 12.sp) }
        }
    }
}

/** Переключатель месяца. */
@Composable
fun MonthSwitcher(month: YearMonth, onChange: (YearMonth) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(month.minusMonths(1)) }) { Icon(AppIcons.Left, "Прошлый месяц", tint = p.text) }
        Text(
            month.format(monthFormat).cap(), color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (AppSettings.monthStart > 1) Text(
            "с ${AppSettings.monthStart}-го", color = p.text2, fontSize = 12.sp,
        )
        IconButton(onClick = { onChange(month.plusMonths(1)) }, enabled = month < AppSettings.currentPeriod()) {
            Icon(AppIcons.Right, "Следующий месяц", tint = if (month < AppSettings.currentPeriod()) p.text else p.text2.copy(alpha = 0.3f))
        }
    }
}

/** Переключатель-пилюли. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.chip).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, o ->
            val sel = i == selected
            Box(
                Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (sel) p.primary else Color.Transparent).clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(o, color = if (sel) p.onPrimary else p.text2, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
            }
        }
    }
}

/** Строка операции. */
@Composable
fun TransactionRow(t: Transaction, onClick: (() -> Unit)? = null) {
    val p = LocalPalette.current
    val income = t.isIncome
    var m = Modifier.fillMaxWidth()
    if (onClick != null) m = m.clickable(onClick = onClick)
    Row(m.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        CircleIcon(iconOf(t.category), p.colorOf(t.category))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.merchant, color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${t.category} · ${AppSettings.walletName(t.bank)} · ${formatTime(t.timestamp, timeFormat)}",
                color = p.text2, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        val own = t.category == kz.koshelek.app.parser.Categories.OWN
        Text(
            (if (income) "+" else "−") + money(t.amount),
            color = if (own) p.text2 else if (income) p.income else p.expense,
            fontWeight = FontWeight.Bold, fontSize = 15.sp,
        )
    }
}

/** Полоса прогресса. */
@Composable
fun Bar(progress: Float, color: Color, track: Color, height: Dp = 7.dp) {
    Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height)).background(track)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(height).clip(RoundedCornerShape(height)).background(color))
    }
}

/** Месячные суммы. */
fun List<Transaction>.inMonth(m: YearMonth) = filter { AppSettings.periodOf(it.timestamp.toLocalDate()) == m }
