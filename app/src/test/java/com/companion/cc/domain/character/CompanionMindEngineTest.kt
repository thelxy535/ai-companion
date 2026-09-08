package com.companion.cc.domain.character

import com.companion.cc.domain.model.Attitude
import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Mood
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionMindEngineTest {
    private val calm = EmotionalState(
        mood = Mood.CALM,
        energy = 0.6f,
        affection = 0.6f,
        stress = 0.2f,
        attitude = Attitude.NEUTRAL
    )

    @Test
    fun `night owl keeps more energy and social battery late at night`() {
        val result = CompanionMindEngine.advance(
            previous = InnerState(),
            emotionalState = calm,
            rhythm = CompanionRhythm.nightOwl(),
            nowHour = 23.0,
            elapsedMs = 6 * 60 * 60 * 1000L
        )

        assertTrue(result.state.energy > 0.35f)
        assertTrue(result.cue.contains("夜"))
    }

    @Test
    fun `early riser naturally slows down at night without claiming to sleep`() {
        val result = CompanionMindEngine.advance(
            previous = InnerState(),
            emotionalState = calm,
            rhythm = CompanionRhythm.earlyRiser(),
            nowHour = 23.0,
            elapsedMs = 6 * 60 * 60 * 1000L
        )

        assertTrue(result.state.energy < 0.55f)
        assertTrue(result.cue.contains("慢") || result.cue.contains("安静"))
    }

    @Test
    fun `memory resonance is a gentle cue instead of a forced topic`() {
        val result = MemoryResonanceEngine.resonate(
            previous = InnerState(),
            memory = "用户上周说想去看海",
            relevance = 0.86f,
            rhythm = CompanionRhythm(memoryStickiness = 0.9f)
        )

        assertTrue(result.state.caringAbout.contains("看海"))
        assertTrue(result.cue.contains("想起"))
    }

    @Test
    fun `memory resonance wording follows how strongly the character holds on to memories`() {
        val sticky = MemoryResonanceEngine.resonate(
            previous = InnerState(),
            memory = "我们上次在雨里买了热饮",
            relevance = 0.9f,
            rhythm = CompanionRhythm(memoryStickiness = 0.9f)
        )
        val light = MemoryResonanceEngine.resonate(
            previous = InnerState(),
            memory = "我们上次在雨里买了热饮",
            relevance = 0.9f,
            rhythm = CompanionRhythm(memoryStickiness = 0.1f)
        )

        assertTrue(sticky.cue.contains("还在心里") || sticky.cue.contains("想起"))
        assertTrue(light.cue.contains("联想到") || light.cue.contains("想起"))
        assertTrue(sticky.state.lastMemoryResonance.contains("热饮"))
    }
}
