package com.companion.cc.domain.character

import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Mood
import org.junit.Assert.assertTrue
import org.junit.Test

class InnerStateTest {

    @Test
    fun `tired state raises hesitation without replacing the character`() {
        val before = InnerState(shareImpulse = 0.7f, hesitation = 0.2f)
        val after = InnerStateTransition.afterInteraction(
            before,
            TemperamentProfile(expressiveness = 0.8f, shareImpulse = 0.7f),
            EmotionalState(mood = Mood.TIRED, energy = 0.2f, affection = 0.5f, stress = 0.4f),
            "今天有点累"
        )

        assertTrue(after.hesitation > before.hesitation)
        assertTrue(after.shareImpulse in 0f..1f)
        assertTrue(after.hesitation - before.hesitation < 0.5f)
    }

    @Test
    fun `positive interaction leaves a small meaningful aftertaste`() {
        val after = InnerStateTransition.afterInteraction(
            InnerState(),
            TemperamentProfile(),
            EmotionalState(mood = Mood.HAPPY, energy = 0.8f, affection = 0.8f, stress = 0.1f),
            "和你聊天很开心"
        )

        assertTrue(after.lastInteractionMeaning.isNotBlank())
        assertTrue(after.emotionalAftertaste.isNotBlank())
    }
}
