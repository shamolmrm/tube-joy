package com.bahalabs.sortopia.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bahalabs.sortopia.audio.Sfx
import com.bahalabs.sortopia.audio.Sounds
import com.bahalabs.sortopia.data.GameStore
import com.bahalabs.sortopia.engine.Board
import com.bahalabs.sortopia.engine.GameMode
import com.bahalabs.sortopia.engine.LevelSpec
import com.bahalabs.sortopia.engine.Levels
import com.bahalabs.sortopia.engine.Move
import com.bahalabs.sortopia.engine.Solver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What is being played: a numbered level of a mode, or today's challenge. */
sealed interface Puzzle {
    data class Level(val mode: GameMode, val number: Int) : Puzzle
    data class Daily(val epochDay: Long) : Puzzle
}

/** A move being animated: the UI draws [before] morphing into the new board. */
data class MoveAnim(val id: Long, val before: Board, val move: Move, val color: Int)

/** Things the player can get with a video or coins. */
enum class Boost(val coinCost: Int) { HINT(60), TUBE(150), UNDOS(80) }

data class GameUiState(
    val puzzle: Puzzle,
    val mode: GameMode,
    val spec: LevelSpec? = null,
    val board: Board? = null,
    val selected: Int? = null,
    val anim: MoveAnim? = null,
    val hint: Move? = null,
    val undosLeft: Int = FREE_UNDOS,
    val canUndo: Boolean = false,
    val extraTubeUsed: Boolean = false,
    val stuck: Boolean = false,
    val won: Boolean = false,
    val reward: Int = 0,
    /** Bumped on an invalid tap so the UI can shake the tube. */
    val shake: Pair<Int, Long>? = null,
    val message: Int? = null,
) {
    companion object {
        const val FREE_UNDOS = 5
    }
}

class GameViewModel(
    private val puzzle: Puzzle,
    private val store: GameStore,
    private val sounds: Sounds,
) : ViewModel() {

    private val mode = when (puzzle) {
        is Puzzle.Level -> puzzle.mode
        is Puzzle.Daily -> Levels.dailyMode(puzzle.epochDay)
    }

    private val _state = MutableStateFlow(GameUiState(puzzle, mode))
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private val history = ArrayList<Board>()
    private var start: Board? = null
    private var animIds = 0L

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val (spec, board) = withContext(Dispatchers.Default) {
                when (puzzle) {
                    is Puzzle.Level -> Levels.spec(puzzle.mode, puzzle.number) to Levels.board(puzzle.mode, puzzle.number)
                    is Puzzle.Daily -> Levels.dailySpec(puzzle.epochDay) to Levels.daily(puzzle.epochDay)
                }
            }
            start = board
            history.clear()
            _state.value = GameUiState(puzzle, mode, spec = spec, board = board)
        }
    }

    fun tap(tube: Int) {
        val s = _state.value
        val board = s.board ?: return
        if (s.anim != null || s.won) return
        val sel = s.selected
        when {
            sel == null -> {
                if (board.tubes[tube].isEmpty() || board.isComplete(tube)) {
                    invalid(tube)
                } else {
                    sounds.play(Sfx.SELECT, 0.5f)
                    _state.update { it.copy(selected = tube) }
                }
            }
            sel == tube -> _state.update { it.copy(selected = null) }
            else -> {
                val move = board.moveOf(sel, tube)
                if (move == null) {
                    // Tapping another full tube switches the selection instead of failing.
                    if (board.tubes[tube].isNotEmpty() && !board.isComplete(tube)) {
                        sounds.play(Sfx.SELECT, 0.5f)
                        _state.update { it.copy(selected = tube) }
                    } else {
                        invalid(tube)
                    }
                } else {
                    play(board, move)
                }
            }
        }
    }

    private fun invalid(tube: Int) {
        sounds.play(Sfx.INVALID, 0.6f)
        _state.update { it.copy(selected = null, shake = tube to System.nanoTime()) }
    }

    private fun play(board: Board, move: Move) {
        history.add(board)
        val color = board.tubes[move.from].last()
        val next = board.apply(move)
        sounds.play(if (mode == GameMode.WATER) Sfx.POUR else Sfx.CLINK)
        _state.update {
            it.copy(
                board = next,
                selected = null,
                hint = null,
                canUndo = true,
                anim = MoveAnim(++animIds, board, move, color),
            )
        }
    }

    /** Called by the UI when a move's animation has finished. */
    fun animDone(id: Long) {
        val s = _state.value
        val anim = s.anim ?: return
        if (anim.id != id) return
        val board = s.board ?: return
        if (board.isComplete(anim.move.to)) sounds.play(Sfx.COMPLETE, 0.7f)
        when {
            board.isSolved -> win()
            board.isStuck -> _state.update { it.copy(anim = null, stuck = true) }
            else -> _state.update { it.copy(anim = null) }
        }
    }

    private fun win() {
        sounds.play(Sfx.WIN)
        val reward = when (puzzle) {
            is Puzzle.Daily -> DAILY_REWARD
            is Puzzle.Level -> if (_state.value.spec?.hard == true) HARD_REWARD else LEVEL_REWARD
        }
        _state.update { it.copy(anim = null, won = true, reward = reward) }
        viewModelScope.launch {
            store.addCoins(reward)
            when (puzzle) {
                is Puzzle.Daily -> store.completeDaily(puzzle.epochDay)
                is Puzzle.Level -> store.completeLevel(puzzle.mode, puzzle.number)
            }
        }
    }

    /** The player watched the bonus video on the win screen: triple the reward. */
    fun claimBonus() {
        val extra = _state.value.reward * 2
        _state.update { it.copy(reward = it.reward * 3) }
        viewModelScope.launch { store.addCoins(extra) }
    }

    fun undo() {
        val s = _state.value
        if (s.anim != null || s.won || history.isEmpty()) return
        if (s.undosLeft <= 0) return
        val prev = history.removeAt(history.lastIndex)
        // An added tube stays when undoing past the moment it was added.
        val restored = if (prev.tubeCount < (s.board?.tubeCount ?: 0)) prev.withExtraTube() else prev
        _state.update {
            it.copy(board = restored, selected = null, hint = null, stuck = false, undosLeft = it.undosLeft - 1, canUndo = history.isNotEmpty())
        }
    }

    fun restart() {
        val first = start ?: return
        history.clear()
        _state.update {
            it.copy(board = first, selected = null, hint = null, stuck = false, won = false, anim = null, canUndo = false, extraTubeUsed = false, undosLeft = GameUiState.FREE_UNDOS)
        }
    }

    /** Pays for a boost with coins. Returns false when the player can't afford it. */
    fun buyWithCoins(boost: Boost) {
        viewModelScope.launch {
            if (store.spend(boost.coinCost)) grant(boost)
            else _state.update { it.copy(message = com.bahalabs.sortopia.R.string.not_enough_coins) }
        }
    }

    /** Gives a boost the player earned (by video or coins). */
    fun grant(boost: Boost) {
        when (boost) {
            Boost.UNDOS -> _state.update { it.copy(undosLeft = it.undosLeft + 5) }
            Boost.TUBE -> _state.update {
                val b = it.board ?: return@update it
                it.copy(board = b.withExtraTube(), extraTubeUsed = true, stuck = false)
            }
            Boost.HINT -> showHint()
        }
    }

    private fun showHint() {
        val board = _state.value.board ?: return
        viewModelScope.launch {
            val move = withContext(Dispatchers.Default) { Solver.hint(board, maxNodes = 250_000) }
            if (move == null) _state.update { it.copy(message = com.bahalabs.sortopia.R.string.hint_none) }
            else _state.update { it.copy(hint = move, selected = null) }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }

    fun dismissStuck() = _state.update { it.copy(stuck = false) }

    companion object {
        const val LEVEL_REWARD = 20
        const val HARD_REWARD = 40
        const val DAILY_REWARD = 100
    }
}
