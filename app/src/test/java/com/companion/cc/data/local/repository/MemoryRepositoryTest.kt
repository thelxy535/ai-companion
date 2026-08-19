package com.companion.cc.data.local.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryRepositoryTest {
    @Test
    fun proposalStatusDefaultsToPending() {
        val review = MemoryReviewDraft(
            id = "review-1",
            scopeKey = "companion:a",
            kind = "preference",
            title = "喜欢茶",
            content = "用户喜欢茶",
            confidence = 0.8,
            proposalHash = "hash-1"
        )
        assertEquals("pending", review.status)
    }
}
