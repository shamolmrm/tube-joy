package com.bahalabs.sortopia.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BoardTest {

    private fun board(rule: MoveRule, vararg tubes: List<Int>) = Board(tubes.toList(), 4, rule)

    @Test
    fun pourMovesTheWholeTopRunThatFits() {
        val b = board(MoveRule.POUR, listOf(0, 1, 1, 1), listOf(2, 1), emptyList())
        assertThat(b.moveOf(0, 1)).isEqualTo(Move(0, 1, 2))
        assertThat(b.moveOf(0, 2)).isEqualTo(Move(0, 2, 3))
    }

    @Test
    fun singleMovesOneItem() {
        val b = board(MoveRule.SINGLE, listOf(0, 1, 1), emptyList())
        assertThat(b.moveOf(0, 1)).isEqualTo(Move(0, 1, 1))
    }

    @Test
    fun cannotMoveOntoAnotherColourOrIntoAFullTube() {
        val b = board(MoveRule.POUR, listOf(0), listOf(1), listOf(0, 0, 0, 0))
        assertThat(b.moveOf(0, 1)).isNull()
        assertThat(b.moveOf(0, 2)).isNull()
        assertThat(b.moveOf(1, 1)).isNull()
    }

    @Test
    fun applyMovesItemsAndDetectsSolved() {
        val b = board(MoveRule.POUR, listOf(0, 0, 1, 1), listOf(1, 1), listOf(0, 0))
        val after = b.apply(b.moveOf(0, 1)!!).let { it.apply(it.moveOf(0, 2)!!) }
        assertThat(after.tubes).containsExactly(emptyList<Int>(), listOf(1, 1, 1, 1), listOf(0, 0, 0, 0)).inOrder()
        assertThat(after.isSolved).isTrue()
    }

    @Test
    fun hiddenItemsAreRevealedWhenTheyReachTheTop() {
        val b = Board(listOf(listOf(0, 1, 2), emptyList()), 4, MoveRule.POUR, hidden = listOf(2, 0))
        assertThat(b.isHidden(0, 1)).isTrue()
        val after = b.apply(b.moveOf(0, 1)!!)
        assertThat(after.hidden[0]).isEqualTo(1)
        assertThat(after.isHidden(0, 1)).isFalse()
        assertThat(after.isHidden(0, 0)).isTrue()
    }

    @Test
    fun hiddenItemsDoNotJoinTheTopRun() {
        val b = Board(listOf(listOf(1, 1, 1), emptyList()), 4, MoveRule.POUR, hidden = listOf(2, 0))
        assertThat(b.topRun(0)).isEqualTo(1)
    }
}
