package com.bahalabs.sortopia.ui.game

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bahalabs.sortopia.R
import com.bahalabs.sortopia.SortopiaApp
import com.bahalabs.sortopia.engine.Board
import com.bahalabs.sortopia.engine.GameMode
import com.bahalabs.sortopia.ui.common.ActionButton
import com.bahalabs.sortopia.ui.common.BannerAd
import com.bahalabs.sortopia.ui.common.CoinChip
import com.bahalabs.sortopia.ui.common.CoinIcon
import com.bahalabs.sortopia.ui.common.Confetti
import com.bahalabs.sortopia.ui.common.CornerBadge
import com.bahalabs.sortopia.ui.common.PillButton
import com.bahalabs.sortopia.ui.theme.BoardTheme
import com.bahalabs.sortopia.ui.theme.GameIcons
import com.bahalabs.sortopia.ui.theme.Gold
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val HintColor = Color(0xFF7CFFB2)

@Composable
fun GameScreen(puzzle: Puzzle, onBack: () -> Unit, onNext: (Puzzle) -> Unit) {
    val app = LocalContext.current.applicationContext as SortopiaApp
    val c = app.container
    val vm: GameViewModel = viewModel(key = puzzle.toString()) { GameViewModel(puzzle, c.store, c.sounds) }
    val state by vm.state.collectAsState()
    val progress by c.store.progress.collectAsState(initial = null)
    val activity: Activity = LocalActivity.current ?: return
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var offer by remember { mutableStateOf<Boost?>(null) }
    val theme = progress?.theme ?: BoardTheme.MIDNIGHT
    val vibrate = progress?.vibration ?: true

    state.message?.let { msg ->
        val text = stringResource(msg)
        LaunchedEffect(msg) {
            Toast.makeText(activity, text, Toast.LENGTH_SHORT).show()
            vm.messageShown()
        }
    }
    val adMissing = stringResource(R.string.ad_not_ready)

    fun watchFor(boost: Boost) {
        scope.launch {
            if (c.ads.showRewarded(activity)) vm.grant(boost)
            else Toast.makeText(activity, adMissing, Toast.LENGTH_SHORT).show()
        }
    }

    fun goNext() {
        scope.launch {
            val p = state.puzzle
            // Full-screen ads only between levels, never in the first levels, at most one per 3 levels.
            if (p is Puzzle.Level && p.number >= 4 && c.store.levelsSinceAd() >= 3) {
                if (c.ads.showInterstitial(activity)) c.store.resetLevelsSinceAd()
            }
            when (p) {
                is Puzzle.Level -> onNext(Puzzle.Level(p.mode, p.number + 1))
                is Puzzle.Daily -> onBack()
            }
        }
    }

    Box(Modifier.fillMaxSize().background(theme.brush)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            TopBar(state, progress?.coins ?: 0, onBack)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val board = state.board
                if (board != null) {
                    BoardView(
                        state = state,
                        board = board,
                        symbols = progress?.colorSymbols ?: false,
                        accent = theme.accent,
                        onTap = { i ->
                            if (vibrate) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            vm.tap(i)
                        },
                        onAnimDone = vm::animDone,
                    )
                }
            }
            BottomBar(
                state = state,
                onUndo = { if (state.undosLeft > 0) vm.undo() else offer = Boost.UNDOS },
                onRestart = vm::restart,
                onHint = { offer = Boost.HINT },
                onTube = { offer = Boost.TUBE },
            )
            BannerAd(c.ads)
        }

        AnimatedVisibility(state.stuck && !state.won, enter = fadeIn(), exit = fadeOut()) {
            StuckOverlay(
                canUndo = state.canUndo,
                tubeAvailable = !state.extraTubeUsed,
                onUndo = { vm.dismissStuck(); if (state.undosLeft > 0) vm.undo() else offer = Boost.UNDOS },
                onTube = { vm.dismissStuck(); offer = Boost.TUBE },
                onRestart = vm::restart,
            )
        }

        offer?.let { boost ->
            OfferOverlay(
                boost = boost,
                coins = progress?.coins ?: 0,
                onWatch = { offer = null; watchFor(boost) },
                onPay = { offer = null; vm.buyWithCoins(boost) },
                onDismiss = { offer = null },
            )
        }

        if (state.won) {
            Confetti()
            WinOverlay(
                daily = state.puzzle is Puzzle.Daily,
                reward = state.reward,
                onBonus = {
                    scope.launch {
                        if (c.ads.showRewarded(activity)) vm.claimBonus()
                        else Toast.makeText(activity, adMissing, Toast.LENGTH_SHORT).show()
                    }
                },
                onNext = ::goNext,
            )
        }
    }
}

@Composable
private fun TopBar(state: GameUiState, coins: Int, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            val title = when (val p = state.puzzle) {
                is Puzzle.Level -> stringResource(R.string.level_n, p.number)
                is Puzzle.Daily -> stringResource(R.string.daily_title)
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(modeName(state.mode)), color = Color(0xB3FFFFFF), style = MaterialTheme.typography.labelSmall)
                if (state.spec?.hard == true) Tag(stringResource(R.string.hard_level), Color(0xFFFF4D6D))
                if (state.spec?.mystery == true) Tag(stringResource(R.string.mystery), Color(0xFF8B6CFF))
            }
        }
        CoinChip(coins)
    }
}

@Composable
private fun Tag(text: String, color: Color) {
    Text(
        text,
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 7.dp, vertical = 1.dp),
    )
}

fun modeName(mode: GameMode) = when (mode) {
    GameMode.WATER -> R.string.mode_water
    GameMode.BALL -> R.string.mode_ball
    GameMode.NUT -> R.string.mode_nut
}

@Composable
private fun BottomBar(state: GameUiState, onUndo: () -> Unit, onRestart: () -> Unit, onHint: () -> Unit, onTube: () -> Unit) {
    val busy = state.anim != null || state.board == null
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ActionButton(GameIcons.Undo, stringResource(R.string.undo), onUndo, enabled = !busy && state.canUndo) {
            CornerBadge {
                if (state.undosLeft > 0) Text(state.undosLeft.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                else Icon(GameIcons.Video, null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
        ActionButton(Icons.Filled.Refresh, stringResource(R.string.restart), onRestart, enabled = !busy && state.canUndo)
        ActionButton(GameIcons.Hint, stringResource(R.string.hint), onHint, enabled = !busy) {
            CornerBadge { Icon(GameIcons.Video, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
        }
        ActionButton(Icons.Filled.Add, stringResource(R.string.add_tube), onTube, enabled = !busy && !state.extraTubeUsed) {
            if (!state.extraTubeUsed) CornerBadge { Icon(GameIcons.Video, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
        }
    }
}

/** Splits a tube's items into drawable runs. Hidden items stay separate so each shows its own "?". */
private fun segsOf(board: Board, tube: Int): MutableList<Seg> {
    val out = ArrayList<Seg>()
    board.tubes[tube].forEachIndexed { i, c ->
        val hidden = board.isHidden(tube, i)
        val last = out.lastOrNull()
        if (last != null && !hidden && !last.hidden && last.color == c) out[out.lastIndex] = last.copy(amount = last.amount + 1)
        else out.add(Seg(c, 1f, hidden))
    }
    return out
}

private fun MutableList<Seg>.removeTop(amount: Float): MutableList<Seg> {
    var left = amount
    while (left > 0f && isNotEmpty()) {
        val last = last()
        if (last.amount <= left) { left -= last.amount; removeAt(lastIndex) }
        else { this[lastIndex] = last.copy(amount = last.amount - left); left = 0f }
    }
    return this
}

private fun MutableList<Seg>.addTop(color: Int, amount: Float): MutableList<Seg> {
    if (amount <= 0f) return this
    val last = lastOrNull()
    if (last != null && !last.hidden && last.color == color) this[lastIndex] = last.copy(amount = last.amount + amount)
    else add(Seg(color, amount, false))
    return this
}

@Composable
private fun BoardView(
    state: GameUiState,
    board: Board,
    symbols: Boolean,
    accent: Color,
    onTap: (Int) -> Unit,
    onAnimDone: (Long) -> Unit,
) {
    val mode = state.mode
    val anim = state.anim
    val water = mode == GameMode.WATER
    val progress = remember(anim?.id) { Animatable(0f) }
    LaunchedEffect(anim?.id) {
        if (anim != null) {
            progress.animateTo(1f, tween(if (water) 820 else 360, easing = if (water) LinearEasing else FastOutSlowInEasing))
            onAnimDone(anim.id)
        }
    }
    val p = progress.value
    val shown = anim?.before ?: board
    // The extra-tube booster can add a tube mid-animation; always lay out the current count.
    val n = board.tubeCount
    val perRow = if (n <= 5) n else ceil(n / 2f).toInt()
    val rows = ceil(n / perRow.toFloat()).toInt()
    val text = rememberTextMeasurer()
    val density = LocalDensity.current
    val boardCoords = remember { mutableStateOf<LayoutCoordinates?>(null) }
    val cellCoords = remember { mutableStateMapOf<Int, LayoutCoordinates>() }
    val pulse by rememberInfiniteTransition(label = "hint").animateFloat(0.35f, 1f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "pulse")

    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 10.dp).onGloballyPositioned { boardCoords.value = it }, contentAlignment = Alignment.Center) {
        val ratio = aspect(mode, board.capacity)
        val liftDp = 26.dp
        // Bolts are short and wide, so nut mode can use more of each slot's width.
        val nut = mode == GameMode.NUT
        val byWidth = maxWidth / perRow * (if (nut) 0.8f else 0.64f)
        val byHeight = (maxHeight / rows - liftDp - 18.dp) / ratio
        val tubeW = minOf(byWidth, byHeight, if (nut) 88.dp else 72.dp)
        val tubeWpx = with(density) { tubeW.toPx() }
        val geometry = remember(tubeWpx, mode, board.capacity) { TubeGeometry(mode, tubeWpx, board.capacity) }
        val liftPx = with(density) { liftDp.toPx() }
        val mouthY = geometry.width * 0.12f
        // Water: the pouring tube flies to a spot just above and beside the target, pivoting on its mouth.
        val pourShift: Offset = run {
            if (!water || anim == null) return@run Offset.Zero
            val root = boardCoords.value ?: return@run Offset.Zero
            val src = cellCoords[anim.move.from] ?: return@run Offset.Zero
            val dst = cellCoords[anim.move.to] ?: return@run Offset.Zero
            val dir = pourDir(anim, root, src, dst)
            val srcMouth = root.localPositionOf(src, Offset.Zero) + Offset(geometry.width / 2, mouthY)
            val dstMouth = root.localPositionOf(dst, Offset.Zero) + Offset(geometry.width / 2, liftPx + mouthY)
            dstMouth + Offset(-dir * geometry.width * 0.55f, -geometry.width * 1.0f) - srcMouth
        }
        val gap = ((maxWidth - tubeW * perRow) / (perRow + 1)).coerceIn(4.dp, 26.dp)

        Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            for (r in 0 until rows) {
                val rowRange = r * perRow until min(n, (r + 1) * perRow)
                // The pouring tube flies over other rows: its row must draw on top.
                val pouringHere = water && anim != null && anim.move.from in rowRange
                Row(Modifier.zIndex(if (pouringHere) 1f else 0f), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (i in rowRange) {
                        val isSrc = anim != null && anim.move.from == i
                        val isDst = anim != null && anim.move.to == i
                        val segs: List<Seg> = when {
                            i >= shown.tubeCount -> emptyList()
                            anim == null -> segsOf(board, i)
                            water && isSrc -> segsOf(shown, i).removeTop(anim.move.count * pourFraction(p))
                            water && isDst -> segsOf(shown, i).addTop(anim.color, anim.move.count * pourFraction(p))
                            isSrc -> segsOf(shown, i).removeTop(anim.move.count.toFloat())
                            else -> segsOf(shown, i)
                        }
                        val complete = anim == null && board.isComplete(i)
                        val hinted = state.hint != null && (state.hint.from == i || state.hint.to == i)
                        val highlight = when {
                            hinted -> HintColor.copy(alpha = pulse)
                            state.selected == i -> accent
                            else -> null
                        }
                        TubeCell(
                            index = i,
                            geometry = geometry,
                            widthDp = tubeW,
                            liftDp = liftDp,
                            segs = segs,
                            selected = state.selected == i,
                            complete = complete,
                            highlight = highlight,
                            symbols = symbols,
                            tilt = if (water && isSrc) tiltOf(p) * dirOf(anim, boardCoords.value, cellCoords) else 0f,
                            shift = if (water && isSrc) pourShift * moveOf(p) else Offset.Zero,
                            pivotY = mouthY,
                            shake = state.shake?.takeIf { it.first == i }?.second,
                            text = text,
                            onPositioned = { cellCoords[i] = it },
                            onTap = { onTap(i) },
                        )
                    }
                }
            }
        }

        // Moving items drawn on top of everything.
        if (anim != null) {
            Canvas(Modifier.fillMaxSize()) {
                val root = boardCoords.value ?: return@Canvas
                val src = cellCoords[anim.move.from] ?: return@Canvas
                val dst = cellCoords[anim.move.to] ?: return@Canvas
                val srcOrigin = root.localPositionOf(src, Offset.Zero)
                val dstOrigin = root.localPositionOf(dst, Offset.Zero) + Offset(0f, liftPx)
                if (water) {
                    val f = pourFraction(p)
                    if (f > 0f && f < 1f) {
                        val dir = pourDir(anim, root, src, dst)
                        // The pivot (mouth) sits exactly at srcMouth + pourShift while pouring.
                        val mouth = srcOrigin + Offset(geometry.width / 2, mouthY) + pourShift + Offset(dir * geometry.width * 0.18f, geometry.width * 0.05f)
                        val dstLevel = shown.tubes[anim.move.to].size + anim.move.count * f
                        val target = dstOrigin + Offset(geometry.width / 2, geometry.bottom - dstLevel * geometry.unit)
                        val stream = androidx.compose.ui.graphics.Path().apply {
                            moveTo(mouth.x, mouth.y)
                            quadraticTo(target.x, mouth.y, target.x, target.y)
                        }
                        drawPath(stream, itemColor(anim.color), style = androidx.compose.ui.graphics.drawscope.Stroke(geometry.innerWidth * 0.2f, cap = StrokeCap.Round))
                    }
                } else {
                    val fromSize = shown.tubes[anim.move.from].size
                    val toSize = shown.tubes[anim.move.to].size
                    for (k in 0 until anim.move.count) {
                        val start = srcOrigin + geometry.slotCenter(fromSize - anim.move.count + k) // lifted: drawn from top of cell
                        val end = dstOrigin + geometry.slotCenter(toSize + k)
                        val control = Offset((start.x + end.x) / 2, min(start.y, end.y) - geometry.width * 1.6f)
                        val t = p
                        val pos = start * ((1 - t) * (1 - t)) + control * (2 * (1 - t) * t) + end * (t * t)
                        if (mode == GameMode.BALL) drawBall(pos, geometry.ballRadius, anim.color, false, symbols, text)
                        else drawNut(pos, geometry.nutWidth, geometry.nutHeight, anim.color, false, symbols, text)
                    }
                }
            }
        }
    }
}

private operator fun Offset.times(f: Float) = Offset(x * f, y * f)

/** Water: 0–0.25 fly over and tilt, 0.25–0.8 pour, 0.8–1 fly back. */
private fun pourFraction(p: Float) = ((p - 0.25f) / 0.55f).coerceIn(0f, 1f)
private fun moveOf(p: Float): Float {
    val t = when {
        p < 0.25f -> p / 0.25f
        p > 0.8f -> (1f - p) / 0.2f
        else -> 1f
    }
    return t * t * (3 - 2 * t) // smoothstep
}
private fun tiltOf(p: Float) = moveOf(p)

/** +1 when the liquid travels right on screen, −1 when left (tubes can be on different rows). */
private fun pourDir(anim: MoveAnim, root: LayoutCoordinates, src: LayoutCoordinates, dst: LayoutCoordinates): Float {
    val dx = root.localPositionOf(dst, Offset.Zero).x - root.localPositionOf(src, Offset.Zero).x
    return if (dx > 0f || (dx == 0f && anim.move.to > anim.move.from)) 1f else -1f
}

private fun dirOf(anim: MoveAnim, root: LayoutCoordinates?, cells: Map<Int, LayoutCoordinates>): Float {
    val src = cells[anim.move.from]
    val dst = cells[anim.move.to]
    if (root == null || src == null || dst == null) return if (anim.move.to > anim.move.from) 1f else -1f
    return pourDir(anim, root, src, dst)
}

@Composable
private fun TubeCell(
    index: Int,
    geometry: TubeGeometry,
    widthDp: androidx.compose.ui.unit.Dp,
    liftDp: androidx.compose.ui.unit.Dp,
    segs: List<Seg>,
    selected: Boolean,
    complete: Boolean,
    highlight: Color?,
    symbols: Boolean,
    tilt: Float,
    shift: Offset,
    pivotY: Float,
    shake: Long?,
    text: androidx.compose.ui.text.TextMeasurer,
    onPositioned: (LayoutCoordinates) -> Unit,
    onTap: () -> Unit,
) {
    val density = LocalDensity.current
    val liftPx = with(density) { liftDp.toPx() }
    val lift by animateFloatAsState(if (selected || tilt != 0f) 1f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "lift")
    val shakeX = remember { Animatable(0f) }
    LaunchedEffect(shake) {
        if (shake != null) {
            for (k in 0 until 6) shakeX.animateTo(if (k % 2 == 0) 10f else -10f, tween(40))
            shakeX.animateTo(0f, tween(40))
        }
    }
    val heightDp = with(density) { geometry.height.toDp() } + liftDp
    val heightPx = geometry.height + liftPx
    Box(
        Modifier
            .width(widthDp)
            .height(heightDp)
            .onGloballyPositioned(onPositioned)
            .zIndex(if (tilt != 0f) 1f else 0f)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onTap),
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .offset { IntOffset(shakeX.value.roundToInt(), 0) }
                .graphicsLayer {
                    translationX = shift.x
                    translationY = liftPx * (1f - lift) + shift.y
                    rotationZ = tilt * 72f
                    // Pivot on the mouth so the stream leaves from a fixed point.
                    transformOrigin = TransformOrigin(0.5f, pivotY / heightPx)
                },
        ) {
            drawTube(geometry, segs, complete, highlight, symbols, text)
        }
    }
}

@Composable
private fun Scrim(onDismiss: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xAA0B0820))
            .clickable(remember { MutableInteractionSource() }, indication = null) { onDismiss?.invoke() },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun DialogCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .padding(24.dp)
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF3A2C8C), Color(0xFF231A55))))
            .border(2.dp, Color(0x40FFFFFF), RoundedCornerShape(28.dp))
            .clickable(remember { MutableInteractionSource() }, indication = null) {}
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
}

@Composable
private fun WinOverlay(daily: Boolean, reward: Int, onBonus: () -> Unit, onNext: () -> Unit) {
    var bonusTaken by remember { mutableStateOf(false) }
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f)) }
    Scrim {
        Box(Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
            DialogCard {
                Text(
                    stringResource(if (daily) R.string.daily_complete else R.string.level_complete),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Gold,
                )
                Stars()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinIcon(34.dp)
                    Text(stringResource(R.string.earned, reward), Modifier.padding(start = 8.dp), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                }
                if (!bonusTaken) {
                    PillButton(
                        stringResource(R.string.claim_x3),
                        onClick = { bonusTaken = true; onBonus() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = listOf(Color(0xFFFFC83D), Color(0xFFFF8A00)),
                        leading = { Icon(GameIcons.Video, null, tint = Color.White, modifier = Modifier.size(22.dp)) },
                    )
                }
                PillButton(
                    stringResource(if (daily) R.string.continue_ else R.string.next_level),
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Stars() {
    val anims = List(3) { remember { Animatable(0f) } }
    LaunchedEffect(Unit) {
        anims.forEachIndexed { i, a ->
            launch {
                kotlinx.coroutines.delay(150L * i)
                a.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 260f))
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        anims.forEachIndexed { i, a ->
            val s = if (i == 1) 56.dp else 44.dp
            Canvas(Modifier.size(s).graphicsLayer { scaleX = a.value; scaleY = a.value }) {
                val r = size.minDimension / 2
                val path = androidx.compose.ui.graphics.Path().apply {
                    for (k in 0 until 10) {
                        val ang = -Math.PI / 2 + k * Math.PI / 5
                        val rr = if (k % 2 == 0) r else r * 0.45f
                        val pt = center + Offset((kotlin.math.cos(ang) * rr).toFloat(), (sin(ang) * rr).toFloat())
                        if (k == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                    close()
                }
                drawPath(path, androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFFFF1A8), Gold, Color(0xFFFF9F1C))))
            }
        }
    }
}

@Composable
private fun StuckOverlay(canUndo: Boolean, tubeAvailable: Boolean, onUndo: () -> Unit, onTube: () -> Unit, onRestart: () -> Unit) {
    Scrim {
        DialogCard {
            Text(stringResource(R.string.no_moves), style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text(stringResource(R.string.no_moves_body), color = Color(0xCCFFFFFF), style = MaterialTheme.typography.bodyMedium)
            if (canUndo) PillButton(stringResource(R.string.undo), onUndo, Modifier.fillMaxWidth(), colors = listOf(Color(0xFF9C84FF), Color(0xFF5F45D8)))
            if (tubeAvailable) {
                PillButton(
                    stringResource(R.string.add_tube), onTube, Modifier.fillMaxWidth(),
                    colors = listOf(Color(0xFFFFC83D), Color(0xFFFF8A00)),
                    leading = { Icon(GameIcons.Video, null, tint = Color.White, modifier = Modifier.size(22.dp)) },
                )
            }
            PillButton(stringResource(R.string.try_again), onRestart, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun OfferOverlay(boost: Boost, coins: Int, onWatch: () -> Unit, onPay: () -> Unit, onDismiss: () -> Unit) {
    Scrim(onDismiss) {
        DialogCard {
            val title = when (boost) {
                Boost.HINT -> R.string.get_hint
                Boost.TUBE -> R.string.get_tube
                Boost.UNDOS -> R.string.get_undos
            }
            Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text(stringResource(R.string.watch_or_pay, boost.coinCost), color = Color(0xCCFFFFFF), style = MaterialTheme.typography.bodyMedium)
            PillButton(
                stringResource(R.string.watch_video), onWatch, Modifier.fillMaxWidth(),
                colors = listOf(Color(0xFFFFC83D), Color(0xFFFF8A00)),
                leading = { Icon(GameIcons.Video, null, tint = Color.White, modifier = Modifier.size(22.dp)) },
            )
            PillButton(
                stringResource(R.string.use_coins, boost.coinCost), onPay, Modifier.fillMaxWidth(),
                colors = listOf(Color(0xFF9C84FF), Color(0xFF5F45D8)),
                enabled = coins >= boost.coinCost,
                leading = { CoinIcon(22.dp) },
            )
            Text(
                stringResource(R.string.cancel),
                color = Color(0xB3FFFFFF),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onDismiss).padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(2.dp))
        }
    }
}
