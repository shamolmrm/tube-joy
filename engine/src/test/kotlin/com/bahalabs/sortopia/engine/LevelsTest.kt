package com.bahalabs.sortopia.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LevelsTest {

    @Test
    fun levelsAreDeterministic() {
        for (mode in GameMode.entries) {
            assertThat(Levels.board(mode, 37)).isEqualTo(Levels.board(mode, 37))
        }
        assertThat(Levels.board(GameMode.WATER, 5)).isNotEqualTo(Levels.board(GameMode.WATER, 6))
    }

    @Test
    fun everyGeneratedLevelIsSolvableAndUnsorted() {
        // Covers the whole difficulty curve, mystery levels and hard levels for every mode.
        val levels = (1..60) + listOf(80, 100, 120, 150, 240, 500, 1000)
        for (mode in GameMode.entries) for (level in levels) {
            val board = Levels.board(mode, level)
            val spec = Levels.spec(mode, level)
            assertThat(board.isSolved).isFalse()
            assertThat(board.tubes.count { it.isNotEmpty() }).isEqualTo(spec.colors)
            val result = Solver.solve(board, maxNodes = 400_000)
            assertThat(result).isInstanceOf(Solver.Result.Solved::class.java)
            // Replaying the solution really sorts the board.
            val end = (result as Solver.Result.Solved).moves.fold(board) { b, m -> b.apply(b.moveOf(m.from, m.to)!!) }
            assertThat(end.isSolved).isTrue()
        }
    }

    @Test
    fun dailyChallengesAreSolvable() {
        for (day in 20_000L..20_014L) {
            val board = Levels.daily(day)
            assertThat(Solver.solve(board, maxNodes = 400_000)).isInstanceOf(Solver.Result.Solved::class.java)
        }
    }

    @Test
    fun difficultyGrows() {
        assertThat(Levels.spec(GameMode.WATER, 1).colors).isEqualTo(3)
        assertThat(Levels.spec(GameMode.WATER, 200).colors).isEqualTo(12)
        assertThat(Levels.spec(GameMode.NUT, 200).colors).isEqualTo(10)
        assertThat(Levels.spec(GameMode.WATER, 30).hard).isTrue()
        assertThat(Levels.spec(GameMode.WATER, 30).mystery).isTrue()
    }
}
