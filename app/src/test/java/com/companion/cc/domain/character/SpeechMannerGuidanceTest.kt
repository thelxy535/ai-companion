package com.companion.cc.domain.character

import com.companion.cc.domain.model.BehaviorRules
import com.companion.cc.domain.model.ExampleDialogue
import com.companion.cc.domain.model.PersonalityTraits
import com.companion.cc.domain.model.ResponseStyle
import com.companion.cc.domain.model.EmojiFrequency
import com.companion.cc.domain.model.FormalityLevel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechMannerGuidanceTest {

    @Test
    fun `reserved character speaks with measured language and fewer decorations`() {
        val guidance = SpeechMannerGuidance.build(
            personality = PersonalityTraits.default().copy(
                extraversion = 0.2f,
                customTraits = mapOf("人格底色" to "慢热，安静，不喜欢把心事说得太满")
            ),
            rules = BehaviorRules.default().copy(
                responseStyle = ResponseStyle.SERIOUS,
                emojiFrequency = EmojiFrequency.NONE,
                formalityLevel = FormalityLevel.NEUTRAL
            ),
            examples = emptyList()
        )

        assertTrue(guidance.contains("克制"))
        assertTrue(guidance.contains("少用表情"))
        assertFalse(guidance.contains("必须"))
    }

    @Test
    fun `playful character uses examples as a texture rather than a script`() {
        val guidance = SpeechMannerGuidance.build(
            personality = PersonalityTraits.default().copy(extraversion = 0.85f),
            rules = BehaviorRules.default().copy(
                responseStyle = ResponseStyle.PLAYFUL,
                emojiFrequency = EmojiFrequency.HIGH,
                formalityLevel = FormalityLevel.VERY_CASUAL
            ),
            examples = listOf(ExampleDialogue("我累了", "那先靠一会儿嘛～"))
        )

        assertTrue(guidance.contains("活泼"))
        assertTrue(guidance.contains("示例对白的语感"))
        assertTrue(guidance.contains("不要照抄"))
    }
}
