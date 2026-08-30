package com.companion.cc.ui.chat

import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.ui.chat.components.deriveEmotionMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmotionMetricsTest {
    @Test
    fun `derived metrics stay within display range`() {
        val metrics = deriveEmotionMetrics(
            EmotionalState(mood = com.companion.cc.domain.model.Mood.HAPPY, energy = 0.8f, affection = 0.7f, stress = 0.2f)
        )

        assertEquals(6, metrics.size)
        assertTrue(metrics.all { it.second in 0f..1f })
    }
}
