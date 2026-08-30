package com.companion.cc.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamHttpExceptionTest {
    @Test
    fun `authentication and client errors are not retryable`() {
        assertFalse(StreamHttpException(400, "bad request").isRetryable())
        assertFalse(StreamHttpException(401, "unauthorized").isRetryable())
        assertFalse(StreamHttpException(403, "forbidden").isRetryable())
        assertFalse(StreamHttpException(404, "missing").isRetryable())
    }

    @Test
    fun `rate limit and server errors are retryable`() {
        assertTrue(StreamHttpException(429, "rate limited").isRetryable())
        assertTrue(StreamHttpException(500, "server error").isRetryable())
        assertTrue(StreamHttpException(503, "unavailable").isRetryable())
    }
}
