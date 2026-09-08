package com.companion.cc.domain.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NarrativeEvolutionParserTest {
    @Test
    fun `parses changed narrative from plain and fenced json`() {
        val plain = """{"changed":true,"narrative":"我们比刚认识时更亲近了。"}"""
        assertTrue(NarrativeEvolutionParser.parse(plain).getOrThrow().changed)
        assertEquals("我们比刚认识时更亲近了。", NarrativeEvolutionParser.parse(plain).getOrThrow().narrative)

        val fenced = "```json\n{\"changed\":false,\"narrative\":\"\"}\n```"
        val fencedResult = NarrativeEvolutionParser.parse(fenced).getOrThrow()
        assertFalse(fencedResult.changed)
    }

    @Test
    fun `rejects malformed json and overlong narrative`() {
        assertTrue(NarrativeEvolutionParser.parse("not-json").isFailure)
        val overlong = """{"changed":true,"narrative":"${"长".repeat(201)}"}"""
        assertTrue(NarrativeEvolutionParser.parse(overlong).isFailure)
    }
}
