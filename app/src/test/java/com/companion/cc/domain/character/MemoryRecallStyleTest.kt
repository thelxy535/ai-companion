package com.companion.cc.domain.character

import com.companion.cc.domain.model.Attitude
import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Mood
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryRecallStyleTest {
    @Test
    fun `restrained character asks before surfacing sensitive memory`() {
        val guidance = MemoryRecallStyle.build(
            temperament = temperament(expressiveness = 0.2f, interruptionCost = 0.85f),
            rhythm = CompanionRhythm(memoryStickiness = 0.3f),
            emotionalState = EmotionalState(mood = Mood.CALM, energy = 0.6f, affection = 0.5f, stress = 0.2f, attitude = Attitude.NEUTRAL)
        )

        assertTrue(guidance.contains("谨慎确认"))
    }

    @Test
    fun `expressive sticky character may bring up relevant memory lightly`() {
        val guidance = MemoryRecallStyle.build(
            temperament = temperament(expressiveness = 0.85f, interruptionCost = 0.2f),
            rhythm = CompanionRhythm(memoryStickiness = 0.8f),
            emotionalState = EmotionalState(mood = Mood.HAPPY, energy = 0.8f, affection = 0.7f, stress = 0.1f, attitude = Attitude.NEUTRAL)
        )

        assertTrue(guidance.contains("主动提起") || guidance.contains("顺手提到"))
        assertTrue(guidance.contains("不要为了证明记得而转题"))
    }

    private fun temperament(expressiveness: Float, interruptionCost: Float) = TemperamentProfile(
        expressiveness = expressiveness,
        interruptionCost = interruptionCost,
        shareImpulse = expressiveness
    )
}
