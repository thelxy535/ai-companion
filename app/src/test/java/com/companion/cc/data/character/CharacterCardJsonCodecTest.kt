package com.companion.cc.data.character

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterCardJsonCodecTest {
    @Test
    fun `round trips v2 card metadata greetings and book`() {
        val json = """
            {
              "spec": "chara_card_v2",
              "spec_version": "2.0",
              "data": {
                "name": "Mira",
                "description": "A curious guide",
                "scenario": "At a quiet station",
                "first_mes": "你好",
                "alternate_greetings": ["早上好", "晚上好"],
                "tags": ["guide", "calm"],
                "creator": "author",
                "character_version": "2.1",
                "character_book": {"entries": [{"keys":["station"],"content":"There is a clock.","constant":true}]}
              }
            }
        """.trimIndent()

        val card = CharacterCardJsonCodec.parse(json).getOrThrow()
        assertEquals("Mira", card.data.name)
        assertEquals(listOf("早上好", "晚上好"), card.data.alternateGreetings)
        assertEquals(listOf("guide", "calm"), card.data.tags)
        assertEquals("There is a clock.", card.data.characterBook.single().content)

        val reparsed = CharacterCardJsonCodec.parse(CharacterCardJsonCodec.serialize(card)).getOrThrow()
        assertEquals(card, reparsed)
    }

    @Test
    fun `accepts legacy flat card and rejects missing name`() {
        val legacy = """{"name":"Legacy","first_mes":"Hi","description":"old"}"""
        assertEquals("Legacy", CharacterCardJsonCodec.parse(legacy).getOrThrow().data.name)
        val failure = CharacterCardJsonCodec.parse("{}")
        assertTrue(failure.isFailure)
    }
}
