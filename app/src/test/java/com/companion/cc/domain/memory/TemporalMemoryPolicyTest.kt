package com.companion.cc.domain.memory

import org.junit.Assert.assertTrue
import org.junit.Test

class TemporalMemoryPolicyTest {
    @Test
    fun `recent memory has stronger temporal weight than old memory`() {
        val now = 30L * 86_400_000L
        val recentAt = now - 2L * 86_400_000L
        val oldAt = now - 30L * 86_400_000L
        val recent = TemporalMemoryPolicy.weight(updatedAt = recentAt, now = now)
        val old = TemporalMemoryPolicy.weight(updatedAt = oldAt, now = now)

        assertTrue(recent > old)
        assertTrue(TemporalMemoryPolicy.label(recentAt, now) == "近期")
        assertTrue(TemporalMemoryPolicy.label(oldAt, now) == "历史")
    }
}
