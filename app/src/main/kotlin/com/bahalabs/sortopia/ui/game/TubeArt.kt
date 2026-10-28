package com.bahalabs.sortopia.ui.game

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bahalabs.sortopia.engine.GameMode
import com.bahalabs.sortopia.ui.theme.Gold
import com.bahalabs.sortopia.ui.theme.ItemColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** One run of the same item in a tube. [amount] is fractional only while liquid is pouring. */
data class Seg(val color: Int, val amount: Float, val hidden: Boolean)

private val HiddenColor = Color(0xFF3A3358)
private val GlassStroke = Color(0x99FFFFFF)

/** Height of a tube in units of its width, per mode. */
fun aspect(mode: GameMode, capacity: Int): Float = when (mode) {
    GameMode.WATER -> 0.9f + capacity * 0.72f
    GameMode.BALL -> 0.6f + capacity * 0.72f
    GameMode.NUT -> 0.75f + capacity * 0.5f
}

fun itemColor(c: Int): Color = ItemColors[c % ItemColors.size]

private fun Color.shade(f: Float): Color = lerp(this, Color.Black, f)
private fun Color.tint(f: Float): Color = lerp(this, Color.White, f)

/** Geometry shared by drawing and the flying-item animation. Sizes are in px. */
class TubeGeometry(val mode: GameMode, val width: Float, val capacity: Int) {
    val height = width * aspect(mode, capacity)
    val innerLeft = width * 0.14f
    val innerRight = width * 0.86f
    val innerWidth = innerRight - innerLeft
    val bottom = when (mode) {
        GameMode.NUT -> height - width * 0.2f
        else -> height - width * 0.06f
    }
    val unit = when (mode) {
        GameMode.WATER -> (bottom - width * 0.62f) / capacity
        // Balls fill the tube's width, so the slot height follows the width.
        GameMode.BALL -> innerWidth * 0.98f
        GameMode.NUT -> width * 0.5f
    }

    val ballRadius get() = innerWidth * 0.47f
    val nutWidth get() = width * 0.94f
    val nutHeight get() = unit * 0.86f

    /** Centre of the item at [index] (0 = bottom), relative to the tube's top-left. */
    fun slotCenter(index: Int): Offset = Offset(width / 2, bottom - unit * (index + 0.5f))

    /** Where a stream of liquid enters the tube from above. */
    val mouth: Offset get() = Offset(width / 2, width * 0.1f)
}

fun DrawScope.drawTube(
    g: TubeGeometry,
    segs: List<Seg>,
    complete: Boolean,
    highlight: Color?,
    symbols: Boolean,
    text: TextMeasurer,
) {
    when (g.mode) {
        GameMode.WATER -> drawWater(g, segs, complete, highlight, symbols, text)
        GameMode.BALL -> drawBalls(g, segs, complete, highlight, symbols, text)
        GameMode.NUT -> drawBolt(g, segs, complete, highlight, symbols, text)
    }
}

private fun glassPath(g: TubeGeometry): Path {
    val r = g.innerWidth / 2
    return Path().apply {
        moveTo(g.innerLeft, g.width * 0.18f)
        lineTo(g.innerLeft, g.bottom - r)
        arcTo(Rect(g.innerLeft, g.bottom - 2 * r, g.innerRight, g.bottom), 180f, -180f, false)
        lineTo(g.innerRight, g.width * 0.18f)
    }
}

private fun DrawScope.drawGlass(g: TubeGeometry, complete: Boolean, highlight: Color?, back: Boolean) {
    val path = glassPath(g)
    if (back) {
        // Faint glass body behind the contents.
        drawPath(Path().apply { addPath(path); close() }, Color(0x14FFFFFF))
        return
    }
    val stroke = g.width * 0.065f
    val glow = highlight ?: if (complete) Gold else null
    if (glow != null) drawPath(path, glow.copy(alpha = 0.45f), style = Stroke(stroke * 3.2f, cap = StrokeCap.Round))
    drawPath(path, if (complete) Gold else GlassStroke, style = Stroke(stroke, cap = StrokeCap.Round))
    // Rim
    drawRoundRect(
        color = if (complete) Gold else Color(0xCCFFFFFF),
        topLeft = Offset(g.innerLeft - g.width * 0.08f, g.width * 0.12f),
        size = Size(g.innerWidth + g.width * 0.16f, g.width * 0.11f),
        cornerRadius = CornerRadius(g.width * 0.06f),
    )
    // Shine
    drawRoundRect(
        color = Color(0x40FFFFFF),
        topLeft = Offset(g.innerLeft + g.innerWidth * 0.14f, g.width * 0.32f),
        size = Size(g.innerWidth * 0.12f, (g.bottom - g.width * 0.32f) * 0.62f),
        cornerRadius = CornerRadius(g.width * 0.05f),
    )
}

private fun DrawScope.drawWater(g: TubeGeometry, segs: List<Seg>, complete: Boolean, highlight: Color?, symbols: Boolean, text: TextMeasurer) {
    drawGlass(g, complete, highlight, back = true)
    val clip = Path().apply {
        val r = g.innerWidth / 2
        addRoundRect(
            RoundRect(
                Rect(g.innerLeft, 0f, g.innerRight, g.bottom),
                topLeft = CornerRadius.Zero, topRight = CornerRadius.Zero,
                bottomLeft = CornerRadius(r), bottomRight = CornerRadius(r),
            ),
        )
    }
    clipPath(clip) {
        var level = 0f
        for (s in segs) {
            if (s.amount <= 0f) continue
            val top = g.bottom - (level + s.amount) * g.unit
            val bot = g.bottom - level * g.unit
            val base = if (s.hidden) HiddenColor else itemColor(s.color)
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(base.shade(0.18f), base.tint(0.12f), base, base.shade(0.25f)),
                    startX = g.innerLeft, endX = g.innerRight,
                ),
                topLeft = Offset(g.innerLeft, top),
                size = Size(g.innerWidth, bot - top + 0.5f),
            )
            // Surface highlight on each layer
            drawRect(base.tint(0.35f), topLeft = Offset(g.innerLeft, top), size = Size(g.innerWidth, min(g.unit * 0.1f, bot - top)))
            val cy = (top + bot) / 2
            if (s.hidden && s.amount >= 0.6f) drawQuestion(Offset(g.width / 2, cy), g.innerWidth * 0.5f, text)
            else if (symbols && !s.hidden && s.amount >= 0.6f) {
                // One symbol per unit so a run of 3 shows 3 symbols.
                val whole = s.amount.toInt().coerceAtLeast(1)
                for (k in 0 until whole) drawSymbol(s.color, Offset(g.width / 2, bot - (k + 0.5f) * g.unit), g.innerWidth * 0.22f)
            }
            level += s.amount
        }
    }
    drawGlass(g, complete, highlight, back = false)
}

private fun DrawScope.drawBalls(g: TubeGeometry, segs: List<Seg>, complete: Boolean, highlight: Color?, symbols: Boolean, text: TextMeasurer) {
    drawGlass(g, complete, highlight, back = true)
    var index = 0
    for (s in segs) {
        repeat(s.amount.toInt()) {
            drawBall(g.slotCenter(index), g.ballRadius, s.color, s.hidden, symbols, text)
            index++
        }
    }
    drawGlass(g, complete, highlight, back = false)
}

fun DrawScope.drawBall(center: Offset, radius: Float, color: Int, hidden: Boolean, symbols: Boolean, text: TextMeasurer) {
    val base = if (hidden) HiddenColor else itemColor(color)
    drawCircle(Color(0x55000000), radius, center + Offset(0f, radius * 0.12f))
    drawCircle(
        brush = Brush.radialGradient(
            listOf(base.tint(0.55f), base, base.shade(0.35f)),
            center = center + Offset(-radius * 0.35f, -radius * 0.4f),
            radius = radius * 1.5f,
        ),
        radius = radius,
        center = center,
    )
    drawCircle(Color(0x99FFFFFF), radius * 0.22f, center + Offset(-radius * 0.38f, -radius * 0.42f))
    if (hidden) drawQuestion(center, radius, text)
    else if (symbols) drawSymbol(color, center, radius * 0.5f)
}

private fun DrawScope.drawBolt(g: TubeGeometry, segs: List<Seg>, complete: Boolean, highlight: Color?, symbols: Boolean, text: TextMeasurer) {
    val rodW = g.width * 0.2f
    val rodLeft = (g.width - rodW) / 2
    val rodTop = g.width * 0.22f
    val glow = highlight ?: if (complete) Gold else null
    if (glow != null) {
        drawRoundRect(glow.copy(alpha = 0.35f), Offset(rodLeft - rodW * 0.8f, rodTop - rodW * 0.4f), Size(rodW * 2.6f, g.bottom - rodTop + rodW), CornerRadius(rodW))
    }
    // Rod with thread lines
    drawRoundRect(
        brush = Brush.horizontalGradient(listOf(Color(0xFF8C93A8), Color(0xFFE6EAF2), Color(0xFF7A8197)), startX = rodLeft, endX = rodLeft + rodW),
        topLeft = Offset(rodLeft, rodTop),
        size = Size(rodW, g.bottom - rodTop),
        cornerRadius = CornerRadius(rodW * 0.4f),
    )
    var y = rodTop + rodW * 0.6f
    while (y < g.bottom - 4) {
        drawLine(Color(0x55303648), Offset(rodLeft, y), Offset(rodLeft + rodW, y - rodW * 0.35f), strokeWidth = g.width * 0.02f)
        y += rodW * 0.45f
    }
    // Base plate
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFB8BFD1), Color(0xFF5E6478)), startY = g.bottom, endY = g.height),
        topLeft = Offset(g.width * 0.04f, g.bottom),
        size = Size(g.width * 0.92f, g.height - g.bottom - g.width * 0.02f),
        cornerRadius = CornerRadius(g.width * 0.08f),
    )
    if (complete) {
        drawRoundRect(Gold, Offset(g.width * 0.04f, g.bottom), Size(g.width * 0.92f, g.width * 0.04f), CornerRadius(g.width * 0.02f))
    }
    var index = 0
    for (s in segs) {
        repeat(s.amount.toInt()) {
            drawNut(g.slotCenter(index), g.nutWidth, g.nutHeight, s.color, s.hidden, symbols, text)
            index++
        }
    }
}

fun DrawScope.drawNut(center: Offset, w: Float, h: Float, color: Int, hidden: Boolean, symbols: Boolean, text: TextMeasurer) {
    val base = if (hidden) HiddenColor else itemColor(color)
    val left = center.x - w / 2
    val top = center.y - h / 2
    val side = w * 0.2f
    val radius = CornerRadius(h * 0.18f)
    drawRoundRect(Color(0x55000000), Offset(left, top + h * 0.1f), Size(w, h), radius)
    // Three visible faces of a hexagonal nut: darker sides, lit front.
    drawRoundRect(base.shade(0.35f), Offset(left, top), Size(w, h), radius)
    drawRect(
        brush = Brush.verticalGradient(listOf(base.tint(0.35f), base, base.shade(0.12f)), startY = top, endY = top + h),
        topLeft = Offset(left + side, top),
        size = Size(w - 2 * side, h),
    )
    drawRect(base.tint(0.5f), Offset(left + side, top), Size(w - 2 * side, h * 0.12f))
    drawLine(base.shade(0.45f), Offset(left + side, top), Offset(left + side, top + h), strokeWidth = w * 0.015f)
    drawLine(base.shade(0.45f), Offset(left + w - side, top), Offset(left + w - side, top + h), strokeWidth = w * 0.015f)
    if (hidden) drawQuestion(center, h * 0.55f, text)
    else if (symbols) drawSymbol(color, center, h * 0.32f)
}

private fun DrawScope.drawQuestion(center: Offset, size: Float, text: TextMeasurer) {
    val layout = text.measure("?", TextStyle(color = Color(0xCCFFFFFF), fontSize = (size / density).sp, fontWeight = FontWeight.Black))
    drawText(layout, topLeft = center - Offset(layout.size.width / 2f, layout.size.height / 2f))
}

/** A distinct white shape per colour, for colour-blind players. */
fun DrawScope.drawSymbol(color: Int, c: Offset, r: Float) {
    val white = Color(0xE6FFFFFF)
    val stroke = Stroke(r * 0.32f, cap = StrokeCap.Round)
    fun poly(n: Int, rot: Float, radius: Float = r) = Path().apply {
        for (i in 0 until n) {
            val a = rot + i * 2 * PI.toFloat() / n
            val p = c + Offset(cos(a) * radius, sin(a) * radius)
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }
    when (color % 12) {
        0 -> drawCircle(white, r * 0.75f, c)
        1 -> drawPath(poly(3, -PI.toFloat() / 2), white)
        2 -> drawRect(white, c - Offset(r * 0.7f, r * 0.7f), Size(r * 1.4f, r * 1.4f))
        3 -> drawPath(poly(4, 0f), white)
        4 -> drawPath(Path().apply {
            for (i in 0 until 10) {
                val a = -PI.toFloat() / 2 + i * PI.toFloat() / 5
                val rr = if (i % 2 == 0) r else r * 0.45f
                val p = c + Offset(cos(a) * rr, sin(a) * rr)
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }, white)
        5 -> {
            drawLine(white, c - Offset(r, 0f), c + Offset(r, 0f), r * 0.4f, StrokeCap.Round)
            drawLine(white, c - Offset(0f, r), c + Offset(0f, r), r * 0.4f, StrokeCap.Round)
        }
        6 -> {
            drawLine(white, c - Offset(r * 0.75f, r * 0.75f), c + Offset(r * 0.75f, r * 0.75f), r * 0.4f, StrokeCap.Round)
            drawLine(white, c + Offset(-r * 0.75f, r * 0.75f), c + Offset(r * 0.75f, -r * 0.75f), r * 0.4f, StrokeCap.Round)
        }
        7 -> drawCircle(white, r * 0.7f, c, style = stroke)
        8 -> drawPath(poly(6, 0f), white)
        9 -> drawPath(poly(3, PI.toFloat() / 2), white)
        10 -> drawPath(poly(5, -PI.toFloat() / 2), white, style = stroke)
        else -> {
            drawLine(white, c + Offset(-r * 0.8f, -r * 0.4f), c + Offset(r * 0.8f, -r * 0.4f), r * 0.32f, StrokeCap.Round)
            drawLine(white, c + Offset(-r * 0.8f, r * 0.4f), c + Offset(r * 0.8f, r * 0.4f), r * 0.32f, StrokeCap.Round)
        }
    }
}
