package com.companion.cc.data.remote.api

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamSseParserTest {
    @Test
    fun parseReportsServerErrorEvent() {
        val result = StreamSseParser.parse(
            listOf("data: {\"error\":{\"message\":\"provider unavailable\"}}")
        )

        assertEquals("provider unavailable", result.errorMessage)
    }

    @Test
    fun parseReportsJsonEventWithoutContentAsMalformed() {
        val result = StreamSseParser.parse(
            listOf("data: {\"unexpected\":true}")
        )

        assertEquals(1, result.malformedChunks)
    }
}
