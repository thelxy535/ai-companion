package com.companion.cc.domain.message

import com.companion.cc.domain.character.TemperamentProfile
import com.companion.cc.domain.model.Attitude
import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Mood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProactiveEngineTest {
    private val base = TemperamentProfile()
    private val defaultState = EmotionalState(
        mood = Mood.CALM,
        energy = 0.5f,
        affection = 0.5f,
        stress = 0.3f,
        attitude = Attitude.NEUTRAL
    )

    private fun decide(
        temperament: TemperamentProfile = base,
        state: EmotionalState = defaultState,
        sinceHours: Long = 24,
        hour: Double = 14.0
    ) = ProactiveEngine.decide(
        temperament = temperament,
        state = state,
        sinceLastChatMs = sinceHours * 3_600_000L,
        nowHour = hour
    )

    @Test
    fun `quiet hours suppress outreach even when clingy`() {
        val decision = decide(
            temperament = base.copy(clinginess = 0.9f),
            hour = 23.0
        )
        assertFalse(decision.shouldReachOut)
    }

    @Test
    fun `recent chat suppresses outreach to avoid clinginess`() {
        val decision = decide(sinceHours = 1)
        assertFalse(decision.shouldReachOut)
    }

    @Test
    fun `high clinginess and long absence triggers outreach`() {
        val decision = decide(
            temperament = base.copy(clinginess = 0.9f),
            sinceHours = 72
        )
        assertTrue(decision.shouldReachOut)
        assertTrue(decision.reason.isNotBlank())
    }

    @Test
    fun `excited mood boosts outreach`() {
        val decision = decide(
            state = defaultState.copy(mood = Mood.EXCITED),
            sinceHours = 72
        )
        assertTrue(decision.shouldReachOut)
        assertTrue(decision.reason.contains("兴奋"))
    }

    @Test
    fun `sad mood reasons want to hear user`() {
        val decision = decide(
            state = defaultState.copy(mood = Mood.SAD),
            sinceHours = 72
        )
        assertTrue(decision.shouldReachOut)
        assertTrue(decision.reason.contains("低落"))
    }

    @Test
    fun `cold attitude strongly suppresses outreach`() {
        val decision = decide(
            state = defaultState.copy(attitude = Attitude.COLD),
            temperament = base.copy(clinginess = 0.5f),
            sinceHours = 72
        )
        assertFalse(decision.shouldReachOut)
    }

    @Test
    fun `warm attitude and absence triggers with missing user reason`() {
        val decision = decide(
            state = defaultState.copy(attitude = Attitude.WARM),
            sinceHours = 72
        )
        assertTrue(decision.shouldReachOut)
        assertTrue(decision.reason.contains("想"))
    }

    @Test
    fun `intensity stays within zero to one`() {
        val decision = decide(
            temperament = base.copy(clinginess = 0.95f),
            state = defaultState.copy(mood = Mood.EXCITED, attitude = Attitude.WARM, stress = 0.9f),
            sinceHours = 72
        )
        assertTrue(decision.intensity in 0f..1f)
        assertEquals(0, decision.intensity.compareTo(decision.intensity.coerceIn(0f, 1f)))
    }

    @Test
    fun `reserved character is less likely to interrupt after a long absence`() {
        val reserved = decide(
            temperament = base.copy(
                clinginess = 0.55f,
                shareImpulse = 0.2f,
                interruptionCost = 0.9f
            ),
            sinceHours = 72
        )
        val sharing = decide(
            temperament = base.copy(
                clinginess = 0.55f,
                shareImpulse = 0.9f,
                interruptionCost = 0.2f
            ),
            sinceHours = 72
        )

        assertTrue(sharing.intensity > reserved.intensity)
    }

    @Test
    fun `unfinished thought becomes an explicit proactive motivation`() {
        val decision = decide(
            temperament = base.copy(clinginess = 0.45f),
            sinceHours = 24
        ).let {
            ProactiveEngine.decide(
                temperament = base.copy(clinginess = 0.45f),
                state = defaultState,
                sinceLastChatMs = 24 * 3_600_000L,
                nowHour = 14.0,
                innerState = com.companion.cc.domain.character.InnerState(
                    unfinishedThought = "刚才没说完的那件事"
                )
            )
        }

        assertTrue(decision.motivation.contains("没说完"))
        assertTrue(decision.reason.contains("没说完"))
    }

    @Test
    fun `decision exposes bounded interruption cost`() {
        val decision = ProactiveEngine.decide(
            temperament = base.copy(interruptionCost = 0.9f),
            state = defaultState,
            sinceLastChatMs = 24 * 3_600_000L,
            nowHour = 14.0
        )

        assertTrue(decision.interruptionCost in 0f..1f)
        assertTrue(decision.interruptionCost >= 0.9f)
    }
}
