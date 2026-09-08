package com.companion.cc.domain.message

import com.companion.cc.domain.character.CompanionRhythm
import com.companion.cc.domain.character.InnerState
import com.companion.cc.domain.model.Attitude
import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Mood
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProactiveMindContextTest {
    private val state = EmotionalState(Mood.CALM, 0.5f, 0.5f, 0.2f, Attitude.NEUTRAL)

    @Test
    fun `low social battery holds an impulse even after a long absence`() {
        val decision = ProactiveEngine.decide(
            temperament = com.companion.cc.domain.character.TemperamentProfile(clinginess = 0.9f),
            state = state,
            sinceLastChatMs = 72 * 3_600_000L,
            nowHour = 14.0,
            rhythm = CompanionRhythm(socialBattery = 0.2f),
            innerState = InnerState(socialBattery = 0.12f)
        )

        assertFalse(decision.shouldReachOut)
        assertTrue(decision.reason.contains("电量") || decision.reason.contains("打扰"))
    }
}
