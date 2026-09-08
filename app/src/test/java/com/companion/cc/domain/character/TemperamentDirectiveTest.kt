package com.companion.cc.domain.character

import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Mood
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemperamentDirectiveTest {

    private val state = EmotionalState(mood = Mood.CALM, energy = 0.7f, affection = 0.5f, stress = 0.2f)

    @Test
    fun `directive gives reserved and expressive characters different social cues`() {
        val reserved = TemperamentDirective.build(
            state,
            TemperamentProfile(expressiveness = 0.25f, shareImpulse = 0.2f, interruptionCost = 0.85f)
        )
        val expressive = TemperamentDirective.build(
            state,
            TemperamentProfile(expressiveness = 0.85f, shareImpulse = 0.9f, interruptionCost = 0.2f)
        )

        assertNotEquals(reserved, expressive)
        assertTrue(reserved.contains("不太主动") || reserved.contains("慢热"))
        assertTrue(expressive.contains("分享") || expressive.contains("展开"))
    }
}
