package com.bahalabs.sortopia.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bahalabs.sortopia.ads.Ads
import com.bahalabs.sortopia.ui.theme.CardBorder
import com.bahalabs.sortopia.ui.theme.CardWhite
import com.bahalabs.sortopia.ui.theme.Gold
import com.bahalabs.sortopia.ui.theme.ItemColors
import kotlin.random.Random

@Composable
fun CoinIcon(size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2
        drawCircle(Color(0xFFB8860B), r)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF1A8), Gold, Color(0xFFE0A100)), center = center - Offset(r * 0.3f, r * 0.3f), radius = r * 1.3f), r * 0.86f)
        drawCircle(Color(0x66B8860B), r * 0.55f, style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.12f))
    }
}

@Composable
fun CoinChip(coins: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x33000000))
            .border(1.dp, CardBorder, RoundedCornerShape(50))
            .padding(start = 6.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinIcon()
        Text(coins.toString(), Modifier.padding(start = 6.dp), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}

/** Frosted-glass card used across the menus. */
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(CardWhite)
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            .padding(16.dp),
        content = content,
    )
}

/** Chunky pill button that squashes when pressed (the "juicy" feel casual players expect). */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(Color(0xFF7CE38B), Color(0xFF2FB15A)),
    leading: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    textColor: Color = Color.White,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier
            .scale(if (pressed) 0.95f else 1f)
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Brush.verticalGradient(if (enabled) colors else listOf(Color(0xFF5B5675), Color(0xFF433E5C))))
            .border(2.dp, Color(0x40FFFFFF), RoundedCornerShape(50))
            .clickable(source, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = if (height < 48.dp) 16.dp else 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            leading?.invoke()
            Text(text, color = textColor, style = MaterialTheme.typography.labelLarge, fontSize = 17.sp, maxLines = 1)
        }
    }
}

/** Round action button with a label under it, used in the game's bottom bar. */
@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badge: (@Composable BoxScope.() -> Unit)? = null,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(
                Modifier
                    .scale(if (pressed) 0.9f else 1f)
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(if (enabled) listOf(Color(0xFF9C84FF), Color(0xFF5F45D8)) else listOf(Color(0xFF4A4566), Color(0xFF39354F))))
                    .border(2.dp, Color(0x55FFFFFF), CircleShape)
                    .clickable(source, indication = null, enabled = enabled, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = label, tint = if (enabled) Color.White else Color(0x88FFFFFF), modifier = Modifier.size(28.dp))
            }
            badge?.invoke(this)
        }
        Text(label, color = Color(0xDDFFFFFF), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun BoxScope.CornerBadge(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .align(Alignment.TopEnd)
            .padding(top = 0.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFFF4D6D))
            .border(1.5.dp, Color.White, RoundedCornerShape(50))
            .padding(horizontal = 6.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** A one-shot burst of confetti over the whole screen. */
@Composable
fun Confetti(modifier: Modifier = Modifier) {
    val pieces = remember {
        val r = Random(System.nanoTime())
        List(90) {
            floatArrayOf(r.nextFloat(), -r.nextFloat() * 0.3f, (r.nextFloat() - 0.5f) * 0.6f, 0.6f + r.nextFloat() * 0.9f, r.nextFloat() * 360f, it.toFloat())
        }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(2600, easing = LinearEasing)) }
    Canvas(modifier.fillMaxSize()) {
        val p = t.value
        for (pc in pieces) {
            val x = (pc[0] + pc[2] * p) * size.width
            val y = (pc[1] + pc[3] * p * 1.3f) * size.height
            if (y > size.height) continue
            val color = ItemColors[pc[5].toInt() % ItemColors.size].copy(alpha = (1.2f - p).coerceIn(0f, 1f))
            rotate(pc[4] + p * 720f, Offset(x, y)) {
                drawRect(color, Offset(x - 6f, y - 10f), Size(12f, 20f))
            }
        }
    }
}

/** Adaptive banner at the bottom of the game screen. Takes no space until an ad may load. */
@Composable
fun BannerAd(ads: Ads, modifier: Modifier = Modifier) {
    val ready by ads.ready.collectAsState()
    if (!ready) return
    val context = androidx.compose.ui.platform.LocalContext.current
    val width = with(androidx.compose.ui.platform.LocalDensity.current) {
        androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width.toDp().value.toInt()
    }
    val banner = remember(width) { ads.createBanner(context, width) } ?: return
    DisposableEffect(banner) { onDispose { banner.destroy() } }
    Box(modifier.fillMaxWidth().height(banner.heightDp.dp), contentAlignment = Alignment.Center) {
        AndroidView(factory = { banner.view })
    }
}
