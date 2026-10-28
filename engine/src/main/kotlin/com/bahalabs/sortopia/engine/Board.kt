package com.bahalabs.sortopia.engine

/** How many items a move carries. */
enum class MoveRule {
    /** Liquid and nuts: the whole same-colour run on top moves, as much as fits. */
    POUR,

    /** Balls: exactly one item per move. */
    SINGLE,
}

data class Move(val from: Int, val to: Int, val count: Int)

/**
 * A board of tubes. Each tube lists its items bottom → top as colour indices.
 * [hidden] is, per tube, how many items from the bottom are still face-down ("mystery" levels);
 * the top item of a tube is always revealed.
 */
data class Board(
    val tubes: List<List<Int>>,
    val capacity: Int,
    val rule: MoveRule,
    val hidden: List<Int> = List(tubes.size) { 0 },
) {
    init {
        require(hidden.size == tubes.size)
        require(tubes.all { it.size <= capacity })
    }

    val tubeCount: Int get() = tubes.size

    fun isHidden(tube: Int, index: Int): Boolean = index < hidden[tube]

    fun top(tube: Int): Int? = tubes[tube].lastOrNull()

    /** A tube that is full of one colour. */
    fun isComplete(tube: Int): Boolean {
        val t = tubes[tube]
        return t.size == capacity && t.all { it == t[0] }
    }

    val isSolved: Boolean
        get() = tubes.indices.all { tubes[it].isEmpty() || isComplete(it) }

    /** Length of the same-colour run on top of [tube]. */
    fun topRun(tube: Int): Int {
        val t = tubes[tube]
        if (t.isEmpty()) return 0
        val c = t.last()
        var n = 0
        for (i in t.lastIndex downTo 0) {
            if (t[i] != c || isHidden(tube, i)) break
            n++
        }
        return n
    }

    /** The move from [from] to [to], or null when it isn't allowed. */
    fun moveOf(from: Int, to: Int): Move? {
        if (from == to) return null
        val src = tubes[from]
        val dst = tubes[to]
        if (src.isEmpty() || dst.size >= capacity) return null
        if (dst.isNotEmpty() && dst.last() != src.last()) return null
        val count = when (rule) {
            MoveRule.SINGLE -> 1
            MoveRule.POUR -> minOf(topRun(from), capacity - dst.size)
        }
        return if (count > 0) Move(from, to, count) else null
    }

    fun canMove(from: Int, to: Int): Boolean = moveOf(from, to) != null

    fun apply(move: Move): Board {
        val src = tubes[move.from]
        val moved = src.subList(src.size - move.count, src.size)
        val newTubes = tubes.mapIndexed { i, t ->
            when (i) {
                move.from -> t.subList(0, t.size - move.count).toList()
                move.to -> t + moved
                else -> t
            }
        }
        // Whatever is now on top of the source tube is turned face-up.
        val newHidden = hidden.mapIndexed { i, h ->
            if (i == move.from) minOf(h, maxOf(0, newTubes[i].size - 1)) else h
        }
        return copy(tubes = newTubes, hidden = newHidden)
    }

    fun legalMoves(): List<Move> {
        val out = ArrayList<Move>()
        for (f in tubes.indices) for (t in tubes.indices) moveOf(f, t)?.let(out::add)
        return out
    }

    /** Adds an empty tube (the "extra tube" booster). */
    fun withExtraTube(): Board = copy(tubes = tubes + listOf(emptyList()), hidden = hidden + 0)

    /** True when no move can change anything: the player must undo, restart or add a tube. */
    val isStuck: Boolean
        get() = !isSolved && legalMoves().none { m -> !isPointless(m) }

    /** Moving a whole single-colour tube into an empty tube changes nothing. */
    fun isPointless(m: Move): Boolean {
        val src = tubes[m.from]
        return tubes[m.to].isEmpty() && m.count == src.size && hidden[m.from] == 0
    }

    companion object {
        fun solved(colors: Int, capacity: Int, empty: Int, rule: MoveRule): Board =
            Board(List(colors) { c -> List(capacity) { c } } + List(empty) { emptyList() }, capacity, rule)
    }
}
