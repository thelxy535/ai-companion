package com.companion.cc.domain.character

import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Mood
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NaturalConversationGuidanceTest {

    @Test
    fun `reserved tired character is allowed to answer briefly without filling silence`() {
        val guidance = NaturalConversationGuidance.build(
            userMessage = "嗯",
            temperament = TemperamentProfile(
                expressiveness = 0.25f,
                interruptionCost = 0.8f
            ),
            emotionalState = EmotionalState.default().copy(mood = Mood.TIRED)
        )

        assertTrue(guidance.contains("不用把话说满"))
        assertTrue(guidance.contains("不要为了完整而总结"))
    }

    @Test
    fun `expressive character can follow a spark without turning every reply into an interview`() {
        val guidance = NaturalConversationGuidance.build(
            userMessage = "我刚看到一家很有意思的小店",
            temperament = TemperamentProfile(
                expressiveness = 0.85f,
                shareImpulse = 0.8f
            ),
            emotionalState = EmotionalState.default().copy(mood = Mood.HAPPY)
        )

        assertTrue(guidance.contains("顺着这个兴致多说一点"))
        assertTrue(guidance.contains("最多自然地问一个问题"))
        assertFalse(guidance.contains("必须"))
    }
}
