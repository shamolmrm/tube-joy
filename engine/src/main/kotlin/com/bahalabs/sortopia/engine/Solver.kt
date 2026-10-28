package com.bahalabs.sortopia.engine

/**
 * Depth-first solver with a visited set and greedy move ordering. It doesn't look for the
 * shortest solution, only a sure one, fast: the generator uses it to prove every level is
 * solvable, and the game uses it for hints.
 */
object Solver {

    sealed interface Result {
        data class Solved(val moves: List<Move>) : Result
        data object Unsolvable : Result
        /** Gave up after [maxNodes]; the board may or may not be solvable. */
        data object TooHard : Result
    }

    fun solve(start: Board, maxNodes: Int = 150_000): Result {
        if (start.isSolved) return Result.Solved(emptyList())
        val seen = HashSet<String>()
        val path = ArrayList<Move>()
        var nodes = 0
        var exhausted = false

        fun dfs(board: Board): Boolean {
            if (board.isSolved) return true
            if (++nodes > maxNodes) {
                exhausted = true
                return false
            }
            if (!seen.add(key(board))) return false
            for (m in orderedMoves(board)) {
                path.add(m)
                if (dfs(board.apply(m))) return true
                path.removeAt(path.lastIndex)
                if (exhausted) return false
            }
            return false
        }

        return when {
            dfs(start) -> Result.Solved(path.toList())
            exhausted -> Result.TooHard
            else -> Result.Unsolvable
        }
    }

    /** The next move towards a solution, or null if none was found. */
    fun hint(board: Board, maxNodes: Int = 150_000): Move? =
        (solve(board, maxNodes) as? Result.Solved)?.moves?.firstOrNull()

    /** Tube order doesn't matter for solvability, so tubes are sorted to merge equivalent states. */
    private fun key(b: Board): String {
        val parts = ArrayList<String>(b.tubeCount)
        for (i in 0 until b.tubeCount) {
            val sb = StringBuilder()
            sb.append(b.hidden[i]).append(':')
            for (c in b.tubes[i]) sb.append(('a' + c))
            parts.add(sb.toString())
        }
        parts.sort()
        return parts.joinToString("|")
    }

    private fun orderedMoves(b: Board): List<Move> {
        val scored = ArrayList<Pair<Int, Move>>()
        var firstEmpty = -1
        for (i in 0 until b.tubeCount) if (b.tubes[i].isEmpty()) { firstEmpty = i; break }
        for (from in 0 until b.tubeCount) {
            if (b.tubes[from].isEmpty() || b.isComplete(from)) continue
            val run = b.topRun(from)
            val srcSize = b.tubes[from].size
            for (to in 0 until b.tubeCount) {
                val m = b.moveOf(from, to) ?: continue
                val dst = b.tubes[to]
                if (dst.isEmpty()) {
                    // All empty tubes are alike: only try the first one.
                    if (to != firstEmpty) continue
                    if (b.isPointless(m)) continue
                    // Emptying a tube into an empty one only helps when it uncovers something.
                    val score = if (srcSize > run) 20 else 0
                    scored.add(score to m)
                } else {
                    val completes = dst.size + m.count == b.capacity && dst.all { it == dst[0] }
                    val wholeRun = m.count == run
                    val uncovers = srcSize > m.count
                    var score = 40
                    if (completes) score += 100
                    if (wholeRun) score += 30
                    if (uncovers) score += 10
                    // Moving a single-colour tube onto another of the same colour that is
                    // smaller is fine, but splitting a run is rarely useful.
                    if (!wholeRun) score -= 25
                    scored.add(score to m)
                }
            }
        }
        scored.sortByDescending { it.first }
        return scored.map { it.second }
    }
}
