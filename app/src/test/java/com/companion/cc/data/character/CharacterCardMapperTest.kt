package com.companion.cc.data.character

import com.companion.cc.domain.model.CharacterCard
import com.companion.cc.domain.model.CharacterCardData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterCardMapperTest {
    @Test
    fun `maps card to owned character with bounded imported fields`() {
        val card = CharacterCard(data = CharacterCardData(
            name = "Rin",
            description = "A pilot",
            scenario = "On a ship",
            firstMessage = "Ready?",
            alternateGreetings = (1..8).map { "hello $it" },
            exampleMessages = "<START> user says\\nassistant answers",
            tags = (1..30).map { "tag$it" },
            systemPrompt = "stay in character",
            postHistoryInstructions = "be concise"
        ))

        val character = CharacterCardMapper.toCustomCharacter(card, "user-1")
        assertEquals("user-1", character.userId)
        assertNotEquals(card.data.name, character.id)
        assertEquals(5, character.alternateGreetings.size)
        assertEquals(20, character.tags.size)
        assertEquals("On a ship", character.scenario)
        assertEquals("stay in character", character.systemPromptOverride)
        assertTrue(character.exampleDialogues.isNotEmpty())
    }

    @Test
    fun `exports character card without ownership data and preserves fields`() {
        val character = CharacterCardMapper.toCustomCharacter(
            CharacterCard(data = CharacterCardData(
                name = "Exported",
                description = "desc",
                scenario = "scene",
                firstMessage = "hello",
                alternateGreetings = listOf("one"),
                tags = listOf("tag"),
                creator = "me",
                characterVersion = "2.0"
            )),
            "private-user"
        )
        val card = CharacterCardMapper.toCard(character)
        val json = CharacterCardJsonCodec.serialize(card)
        val parsed = CharacterCardJsonCodec.parse(json).getOrThrow()
        assertEquals("Exported", parsed.data.name)
        assertEquals("scene", parsed.data.scenario)
        assertEquals(listOf("tag"), parsed.data.tags)
        assertTrue(!json.contains("private-user"))
    }
}
