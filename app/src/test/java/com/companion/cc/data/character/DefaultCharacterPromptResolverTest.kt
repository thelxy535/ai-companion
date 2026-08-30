package com.companion.cc.data.character

import com.companion.cc.data.local.CompanionConfigLoader
import com.companion.cc.domain.character.UnknownCharacterException
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.model.CustomCharacter
import com.companion.cc.domain.model.ExampleDialogue
import com.companion.cc.domain.model.PersonalityTraits
import com.companion.cc.domain.model.ResponseStyle
import com.companion.cc.domain.model.EmojiFrequency
import com.companion.cc.domain.model.FormalityLevel
import com.companion.cc.domain.model.BehaviorRules
import com.companion.cc.domain.repository.CustomCharacterRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DefaultCharacterPromptResolverTest {
    private val loader = mock<CompanionConfigLoader>()
    private val users = mock<CurrentUserProvider>()
    private val repository = mock<CustomCharacterRepository>()

    @Test
    fun `custom prompt is resolved from current persisted data on every call`() = runTest {
        whenever(users.requireUserId()).thenReturn("user-real")
        whenever(loader.getCompanionConfigStrict("custom-1")).thenReturn(null)
        whenever(repository.getCharacterByIdForUser("custom-1", "user-real"))
            .thenReturn(fullCharacter(name = "Before"))
            .thenReturn(fullCharacter(name = "After"))

        val resolver = DefaultCharacterPromptResolver(loader, users, repository)

        val first = resolver.resolve("custom-1")
        val second = resolver.resolve("custom-1")

        assertEquals("Before", first.config.name)
        assertEquals("After", second.config.name)
        assertTrue(first.config.prompts.system.contains("PERSISTED_BACKSTORY"))
        assertTrue(first.config.prompts.system.contains("PERSISTED_EXAMPLE"))
        verify(repository, org.mockito.kotlin.times(2))
            .getCharacterByIdForUser("custom-1", "user-real")
    }

    @Test
    fun `built in prompt uses exact config and never queries custom repository`() = runTest {
        val builtIn = testCompanionConfig(id = "muse", name = "Configured Muse")
        whenever(loader.getCompanionConfigStrict("muse")).thenReturn(builtIn)

        val resolver = DefaultCharacterPromptResolver(loader, users, repository)
        val resolved = resolver.resolve("muse")

        assertEquals(builtIn, resolved.config)
        verify(repository, never()).getCharacterByIdForUser(any(), any())
        verify(users, never()).requireUserId()
    }

    @Test(expected = UnknownCharacterException::class)
    fun `unknown character does not fall back to legacy default`() = runTest {
        whenever(loader.getCompanionConfigStrict("missing")).thenReturn(null)
        whenever(users.requireUserId()).thenReturn("user-real")
        whenever(repository.getCharacterByIdForUser("missing", "user-real")).thenReturn(null)

        DefaultCharacterPromptResolver(loader, users, repository).resolve("missing")
    }

    @Test
    fun `custom lookup is user scoped`() = runTest {
        whenever(loader.getCompanionConfigStrict("custom-1")).thenReturn(null)
        whenever(users.requireUserId()).thenReturn("user-real")
        whenever(repository.getCharacterByIdForUser("custom-1", "user-real"))
            .thenReturn(fullCharacter())

        DefaultCharacterPromptResolver(loader, users, repository).resolve("custom-1")

        verify(repository).getCharacterByIdForUser("custom-1", "user-real")
    }

    private fun fullCharacter(name: String = "Persisted Character") = CustomCharacter(
        id = "custom-1",
        userId = "user-real",
        name = name,
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
        exampleDialogues = listOf(ExampleDialogue("PERSISTED_USER", "PERSISTED_EXAMPLE")),
        voiceConfig = null,
        behaviorRules = BehaviorRules(
            responseStyle = ResponseStyle.FRIENDLY,
            emojiFrequency = EmojiFrequency.MEDIUM,
            formalityLevel = FormalityLevel.CASUAL,
            topicPreferences = listOf("PREFERRED_TOPIC"),
            avoidTopics = listOf("AVOID_TOPIC")
        )
    )

    private fun testCompanionConfig(
        id: String,
        name: String
    ) = com.companion.cc.domain.model.CompanionConfig(
        id = id,
        name = name,
        emoji = "🎭",
        avatar = "",
        enabled = true,
        personality = com.companion.cc.domain.model.PersonalityConfig(
            coreTraits = emptyList(),
            background = "background",
            speakingStyle = com.companion.cc.domain.model.SpeakingStyle(
                tone = "calm",
                vocabularyLevel = "daily",
                sentenceLength = "medium",
                useEmoji = false,
                useExclamation = false,
                formality = "formal"
            ),
            interests = emptyList(),
            values = emptyList(),
            relationship = com.companion.cc.domain.model.Relationship(
                role = "companion",
                distance = "moderate",
                interactionStyle = "supportive",
                addressUser = "you"
            ),
            behaviorPatterns = emptyList()
        ),
        prompts = com.companion.cc.domain.model.Prompts(
            system = "EXACT_BUILT_IN_SYSTEM",
            greeting = listOf("EXACT_BUILT_IN_GREETING"),
            farewell = emptyList(),
            fallback = emptyList()
        ),
        emotionalModel = com.companion.cc.domain.model.EmotionalModelConfig(
            defaultMood = com.companion.cc.domain.model.Mood.CALM,
            moodStability = 0.8f,
            energyRecoveryRate = 0.1f,
            stressThreshold = 0.7f,
            affectionGrowthRate = 0.05f,
            moodTransitions = emptyMap()
        ),
        memoryPreferences = com.companion.cc.domain.model.MemoryPreferences(
            importanceThreshold = 0.5f,
            summaryFrequency = "daily",
            rememberTopics = emptyList(),
            forgetTopics = emptyList()
        )
    )
}
