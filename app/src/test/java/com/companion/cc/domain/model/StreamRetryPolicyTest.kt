package com.companion.cc.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamRetryPolicyTest {
    @Test
    fun `allows at most two retries for transient failures`() {
        val error = StreamHttpException(503, "unavailable")

        assertTrue(StreamRetryPolicy.shouldRetry(error, attempt = 0))
        assertTrue(StreamRetryPolicy.shouldRetry(error, attempt = 1))
        assertFalse(StreamRetryPolicy.shouldRetry(error, attempt = 2))
    }

    @Test
    fun `uses retry after for rate limits and bounded fallback`() {
        assertEquals(7_000L, StreamRetryPolicy.delayMillis(StreamHttpException(429, "limited", 7)))
        assertEquals(1_000L, StreamRetryPolicy.delayMillis(StreamHttpException(500, "error")))
        assertEquals(30_000L, StreamRetryPolicy.delayMillis(StreamHttpException(500, "error", 60)))
    }
}
