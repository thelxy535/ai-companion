package com.companion.cc.domain.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReflectionResponseParserTest {
    @Test
    fun `parses bounded candidates and markdown fenced json`() {
        val raw = """
            ```json
            {"candidates":[{"kind":"relationship_narrative","title":"关系变化","content":"我们开始更信任彼此。","confidence":0.8}]}
            ```
        """.trimIndent()
        val result = ReflectionResponseParser.parse(raw).getOrThrow()
        assertEquals(1, result.candidates.size)
        assertEquals("relationship_narrative", result.candidates.single().kind)
    }

    @Test
    fun `rejects unknown kinds and malformed output`() {
        assertTrue(ReflectionResponseParser.parse("not-json").isFailure)
        assertTrue(
            ReflectionResponseParser.parse(
                "{\"candidates\":[{\"kind\":\"unknown\",\"title\":\"x\",\"content\":\"y\"}]}"
            ).isFailure
        )
    }
}
