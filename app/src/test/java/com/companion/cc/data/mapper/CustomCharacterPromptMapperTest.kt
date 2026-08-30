package com.companion.cc.data.mapper

import com.companion.cc.domain.model.BehaviorRules
import com.companion.cc.domain.model.CustomCharacter
import com.companion.cc.domain.model.EmojiFrequency
import com.companion.cc.domain.model.ExampleDialogue
import com.companion.cc.domain.model.FormalityLevel
import com.companion.cc.domain.model.PersonalityTraits
import com.companion.cc.domain.model.ResponseStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomCharacterPromptMapperTest {
    @Test
    fun `maps all persisted prompt fields without losing data`() {
        val character = CustomCharacter(
            id = "custom-1",
            userId = "user-real",
            name = "Persisted Name",
            avatar = "avatar.png",
            description = "PERSISTED_DESCRIPTION",
            personality = PersonalityTraits(
                openness = 0.91f,
                conscientiousness = 0.82f,
                extraversion = 0.73f,
                agreeableness = 0.64f,
                neuroticism = 0.15f,
                customTraits = mapOf("trait" to "PERSISTED_TRAIT")
            ),
            backstory = "PERSISTED_BACKSTORY",
            greetingMessage = "PERSISTED_GREETING",
            exampleDialogues = listOf(ExampleDialogue("PERSISTED_USER", "PERSISTED_ASSISTANT")),
            voiceConfig = null,
            behaviorRules = BehaviorRules(
                responseStyle = ResponseStyle.PROFESSIONAL,
                emojiFrequency = EmojiFrequency.NONE,
                formalityLevel = FormalityLevel.FORMAL,
                topicPreferences = listOf("PREFERRED_TOPIC"),
                avoidTopics = listOf("AVOID_TOPIC")
            )
        )

        val config = CustomCharacterPromptMapper.toCompanionConfig(character)
        val prompt = config.prompts.system

        assertEquals("Persisted Name", config.name)
        assertEquals("PERSISTED_GREETING", config.prompts.greeting.single())
        assertTrue(prompt.contains("PERSISTED_DESCRIPTION"))
        assertTrue(prompt.contains("PERSISTED_BACKSTORY"))
        assertTrue(prompt.contains("PERSISTED_TRAIT"))
        assertTrue(prompt.contains("PREFERRED_TOPIC"))
        assertTrue(prompt.contains("AVOID_TOPIC"))
        assertTrue(prompt.contains("PERSISTED_USER"))
        assertTrue(prompt.contains("PERSISTED_ASSISTANT"))
        assertEquals(prompt, CustomCharacterPromptMapper.toCompanionConfig(character).prompts.system)
    }
}
