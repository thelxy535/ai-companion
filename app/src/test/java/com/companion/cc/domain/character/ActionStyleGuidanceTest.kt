package com.companion.cc.domain.character

import org.junit.Assert.assertTrue
import org.junit.Test

class ActionStyleGuidanceTest {
    @Test
    fun `reserved character receives sparse subtle action guidance`() {
        val text = ActionStyleGuidance.build(
            temperament = TemperamentProfile(expressiveness = 0.2f, interruptionCost = 0.8f),
            state = InnerState(recentAction = "低头看着杯沿")
        )

        assertTrue(text.contains("动作很少"))
        assertTrue(text.contains("低头看着杯沿"))
    }

    @Test
    fun `expressive character still treats action as optional`() {
        val text = ActionStyleGuidance.build(
            temperament = TemperamentProfile(expressiveness = 0.85f),
            state = InnerState()
        )

        assertTrue(text.contains("可以更鲜活"))
        assertTrue(text.contains("不必每条都有"))
    }
}
