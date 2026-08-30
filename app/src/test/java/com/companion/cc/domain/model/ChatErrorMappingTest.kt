package com.companion.cc.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatErrorMappingTest {
    @Test
    fun streamRateLimitMapsToRateLimitError() {
        val error = StreamHttpException(429, "busy", 7)

        val mapped = error.toChatError()

        assertEquals(ChatError.RateLimitError(7), mapped)
    }
}
