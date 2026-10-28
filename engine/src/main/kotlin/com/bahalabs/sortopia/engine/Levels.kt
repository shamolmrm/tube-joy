package com.bahalabs.sortopia.engine

/** The three ways to play. They share the rules engine and differ in look and move rule. */
enum class GameMode(val rule: MoveRule, val maxColors: Int) {
    WATER(MoveRule.POUR, maxColors = 12),
    BALL(MoveRule.SINGLE, maxColors = 12),
    NUT(MoveRule.POUR, maxColors = 10),
}

data class LevelSpec(
    val colors: Int,
    val capacity: Int,
    val emptyTubes: Int,
    val rule: MoveRule,
    /** Everything but the top item of each tube starts face-down. */
    val mystery: Boolean,
    /** Shown to the player as a "Hard level" badge. */
    val hard: Boolean,
)

/**
 * Endless, deterministic levels: level N of a mode is always the same board, on every phone, so
 * nothing has to be stored or downloaded. The curve ramps quickly through the first levels
 * (players decide in minutes whether to keep a game), then grows slowly, with a "hard" level
 * every 10 and an easier breather after it.
 */
object Levels {
    const val CAPACITY = 4

    fun spec(mode: GameMode, level: Int): LevelSpec {
        require(level >= 1)
        val base = when {
            level == 1 -> 3
            level == 2 -> 4
            level <= 5 -> 5
            level <= 10 -> 6
            level <= 20 -> 7
            level <= 35 -> 8
            level <= 55 -> 9
            level <= 80 -> 10
            level <= 120 -> 11
            else -> 12
        }
        val hard = level >= 10 && level % 10 == 0
        val breather = level > 10 && level % 10 == 1
        val colors = (base + (if (hard) 1 else 0) - (if (breather) 2 else 0))
            .coerceIn(3, mode.maxColors)
        val mystery = when (mode) {
            GameMode.WATER -> level >= 25 && level % 6 == 0
            GameMode.BALL -> level >= 40 && level % 8 == 0
            GameMode.NUT -> level >= 60 && level % 9 == 0
        }
        return LevelSpec(colors, CAPACITY, emptyTubes = 2, mode.rule, mystery, hard)
    }

    fun board(mode: GameMode, level: Int): Board =
        Generator.generate(spec(mode, level), Rng.seedOf(mode.ordinal.toLong() + 1, level.toLong()))

    /** The mode featured on a given day. */
    fun dailyMode(epochDay: Long): GameMode = GameMode.entries[Math.floorMod(epochDay, GameMode.entries.size.toLong()).toInt()]

    /** One shared challenge per calendar day: a hard board, a mystery one every third day. */
    fun dailySpec(epochDay: Long): LevelSpec {
        val mode = dailyMode(epochDay)
        val colors = minOf(mode.maxColors, 10 + Math.floorMod(epochDay, 3L).toInt())
        return LevelSpec(colors, CAPACITY, 2, mode.rule, mystery = Math.floorMod(epochDay, 3L) == 2L, hard = true)
    }

    fun daily(epochDay: Long): Board = Generator.generate(dailySpec(epochDay), Rng.seedOf(99, epochDay))
}

object Generator {

    fun generate(spec: LevelSpec, seed: Long): Board {
        val rng = Rng(seed)
        var empty = spec.emptyTubes
        // In practice a solvable shuffle turns up within a few attempts; an extra empty tube is
        // the safety net that guarantees the loop ends.
        while (true) {
            var fallback: Board? = null
            repeat(ATTEMPTS) {
                val board = shuffled(spec, empty, rng)
                when (val r = Solver.solve(board, maxNodes = SOLVER_BUDGET)) {
                    is Solver.Result.Solved -> {
                        if (r.moves.size >= minMoves(spec)) return board
                        if (fallback == null) fallback = board
                    }
                    else -> Unit
                }
            }
            fallback?.let { return it }
            empty++
        }
    }

    private fun minMoves(spec: LevelSpec): Int = when (spec.rule) {
        MoveRule.POUR -> spec.colors + spec.colors / 2
        MoveRule.SINGLE -> spec.colors * 2
    }

    private fun shuffled(spec: LevelSpec, empty: Int, rng: Rng): Board {
        while (true) {
            val units = MutableList(spec.colors * spec.capacity) { it / spec.capacity }
            rng.shuffle(units)
            val tubes = units.chunked(spec.capacity)
            if (tubes.any { looksSolved(it) }) continue
            // Too many long runs makes a level trivially easy.
            if (tubes.count { runLength(it) >= 3 } > 1) continue
            val hidden = if (spec.mystery) List(spec.colors) { spec.capacity - 1 } + List(empty) { 0 } else List(spec.colors + empty) { 0 }
            return Board(tubes + List(empty) { emptyList() }, spec.capacity, spec.rule, hidden)
        }
    }

    private fun looksSolved(t: List<Int>) = t.all { it == t[0] }

    private fun runLength(t: List<Int>): Int {
        var n = 1
        for (i in t.lastIndex - 1 downTo 0) if (t[i] == t.last()) n++ else break
        return n
    }

    private const val ATTEMPTS = 40
    private const val SOLVER_BUDGET = 60_000
}
