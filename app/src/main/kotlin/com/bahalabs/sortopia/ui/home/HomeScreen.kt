package com.bahalabs.sortopia.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bahalabs.sortopia.R
import com.bahalabs.sortopia.SortopiaApp
import com.bahalabs.sortopia.engine.GameMode
import com.bahalabs.sortopia.engine.Levels
import com.bahalabs.sortopia.ui.common.BannerAd
import com.bahalabs.sortopia.ui.common.CoinChip
import com.bahalabs.sortopia.ui.common.CoinIcon
import com.bahalabs.sortopia.ui.common.GlassCard
import com.bahalabs.sortopia.ui.common.PillButton
import com.bahalabs.sortopia.ui.game.Seg
import com.bahalabs.sortopia.ui.game.TubeGeometry
import com.bahalabs.sortopia.ui.game.drawTube
import com.bahalabs.sortopia.ui.game.GameViewModel
import com.bahalabs.sortopia.ui.game.modeName
import com.bahalabs.sortopia.ui.theme.BoardTheme
import com.bahalabs.sortopia.ui.theme.GameIcons
import java.util.TimeZone

/** Today's date in the player's time zone, as days since 1970 (java.time needs API 26+). */
fun localEpochDay(): Long {
    val now = System.currentTimeMillis()
    return Math.floorDiv(now + TimeZone.getDefault().getOffset(now), 86_400_000L)
}

@Composable
fun HomeScreen(onPlay: (GameMode, Int) -> Unit, onDaily: (Long) -> Unit, onSettings: () -> Unit, onThemes: () -> Unit) {
    val app = LocalContext.current.applicationContext as SortopiaApp
    val progress by app.container.store.progress.collectAsState(initial = null)
    val p = progress ?: return
    val today = remember { localEpochDay() }
    // A two-second welcome tune the first time home appears after launch (Music skips repeats).
    LaunchedEffect(Unit) { app.container.music.playWelcomeOnce(p.music) }

    Column(Modifier.fillMaxSize().background(p.theme.brush).statusBarsPadding().navigationBarsPadding()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CoinChip(p.coins)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onThemes) { Icon(GameIcons.Palette, stringResource(R.string.themes), tint = Color.White) }
                IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, stringResource(R.string.settings), tint = Color.White) }
            }
            Logo()
            DailyCard(done = p.dailyDoneDay == today, mode = Levels.dailyMode(today), onPlay = { onDaily(today) })
            for (mode in GameMode.entries) {
                ModeCard(mode, p.levels[mode] ?: 1, p.theme, onPlay = { onPlay(mode, p.levels[mode] ?: 1) })
            }
            Spacer(Modifier.height(12.dp))
        }
        // Outside the scrolling content, with a gap above it, so it never sits against a Play button.
        Spacer(Modifier.height(8.dp))
        BannerAd(app.container.ads)
    }
}

@Composable
private fun Logo() {
    val t by rememberInfiniteTransition(label = "logo").animateFloat(-3f, 3f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "bob")
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.app_name),
            modifier = Modifier.graphicsLayer { translationY = t * density },
            style = MaterialTheme.typography.displaySmall.copy(
                brush = Brush.horizontalGradient(listOf(Color(0xFFFF8FD8), Color(0xFFFFD23F), Color(0xFF3DA5FF))),
            ),
            fontSize = 46.sp,
        )
        Text(stringResource(R.string.tagline), color = Color(0xCCFFFFFF), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun DailyCard(done: Boolean, mode: GameMode, onPlay: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFF8A5B), Color(0xFFFF4D9D), Color(0xFF8B6CFF))))
            .border(2.dp, Color(0x55FFFFFF), RoundedCornerShape(26.dp))
            .clickable(enabled = !done, onClick = onPlay)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(54.dp).clip(CircleShape).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
            Icon(if (done) Icons.Filled.Check else GameIcons.Calendar, null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(stringResource(R.string.daily_title), color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(modeName(mode)), color = Color(0xE6FFFFFF), style = MaterialTheme.typography.bodyMedium)
            if (done) {
                Text(stringResource(R.string.daily_done), color = Color.White, style = MaterialTheme.typography.labelSmall)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinIcon(16.dp)
                    Text(
                        stringResource(R.string.daily_reward, GameViewModel.DAILY_REWARD),
                        Modifier.padding(start = 4.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    )
                }
            }
        }
        if (!done) {
            PillButton(stringResource(R.string.play), onPlay, height = 44.dp, colors = listOf(Color.White, Color(0xFFEDE6FF)), textColor = Color(0xFFE0407F))
        }
    }
}

@Composable
private fun ModeCard(mode: GameMode, level: Int, theme: BoardTheme, onPlay: () -> Unit) {
    val desc = when (mode) {
        GameMode.WATER -> R.string.mode_water_desc
        GameMode.BALL -> R.string.mode_ball_desc
        GameMode.NUT -> R.string.mode_nut_desc
    }
    GlassCard(Modifier.fillMaxWidth().clickable(onClick = onPlay)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Preview(mode, Modifier.width(78.dp).height(84.dp))
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(stringResource(modeName(mode)), color = Color.White, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                Text(stringResource(desc), color = Color(0xB3FFFFFF), style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, lineHeight = 16.sp)
                Text(
                    stringResource(R.string.level_n, level),
                    color = theme.accent,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            PillButton(stringResource(R.string.play), onPlay, height = 44.dp)
        }
    }
}

/** A tiny live drawing of the mode, made with the same art as the game board. */
@Composable
private fun Preview(mode: GameMode, modifier: Modifier) {
    val text = rememberTextMeasurer()
    val samples = when (mode) {
        GameMode.WATER -> listOf(listOf(0, 1, 0), listOf(1, 1, 1, 1), listOf(2, 0, 2))
        GameMode.BALL -> listOf(listOf(3, 4, 3), listOf(4, 4, 4, 4), listOf(5, 3))
        GameMode.NUT -> listOf(listOf(6, 7, 6), listOf(7, 7, 7, 7), listOf(1, 6))
    }
    Canvas(modifier) {
        val count = samples.size
        val g0 = TubeGeometry(mode, 1f, 4)
        val w = minOf(size.width / (count * 1.35f), size.height / g0.height)
        val g = TubeGeometry(mode, w, 4)
        val gap = (size.width - w * count) / (count + 1)
        samples.forEachIndexed { i, items ->
            val segs = items.map { Seg(it, 1f, false) }
            translate(left = gap + i * (w + gap), top = (size.height - g.height) / 2) {
                drawTube(g, segs, complete = i == 1, highlight = null, symbols = false, text = text)
            }
        }
    }
}
