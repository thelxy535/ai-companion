package com.companion.cc.domain.memory

/** Keeps current context useful without deleting older lived experience. */
object TemporalMemoryPolicy {
    private const val DAY_MILLIS = 86_400_000L
    private const val HALF_LIFE_DAYS = 14.0

    fun weight(updatedAt: Long, now: Long): Double {
        val ageDays = (now - updatedAt).coerceAtLeast(0L).toDouble() / DAY_MILLIS
        return Math.pow(0.5, ageDays / HALF_LIFE_DAYS)
    }

    fun label(updatedAt: Long, now: Long): String =
        if (now - updatedAt <= 7L * DAY_MILLIS) "近期" else "历史"
}
