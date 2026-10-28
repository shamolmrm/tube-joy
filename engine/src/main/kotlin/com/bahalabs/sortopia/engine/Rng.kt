package com.bahalabs.sortopia.engine

/**
 * SplitMix64. Levels are generated from a seed, so the generator must give the same board for
 * the same seed forever: kotlin.random's algorithm is not guaranteed across versions, this is.
 */
class Rng(seed: Long) {
    private var state = seed

    fun nextLong(): Long {
        state += -0x61c8864680b583ebL
        var z = state
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }

    /** Uniform in [0, bound). */
    fun nextInt(bound: Int): Int {
        require(bound > 0)
        return ((nextLong() ushr 1) % bound).toInt()
    }

    fun <T> shuffle(list: MutableList<T>) {
        for (i in list.lastIndex downTo 1) {
            val j = nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
    }

    companion object {
        /** Mixes several values into one well-spread seed. */
        fun seedOf(vararg parts: Long): Long {
            var h = 0x2545F4914F6CDD1DL
            for (p in parts) h = Rng(h xor p).nextLong()
            return h
        }
    }
}
