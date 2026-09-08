package com.companion.cc.domain.memory

import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.data.local.entity.MemoryReviewEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryConflictDetectorTest {
    @Test
    fun `opposite preference with shared topic is marked as conflict`() {
        val existing = node("喜欢咖啡", "用户喜欢咖啡")
        val candidate = review("不喜欢咖啡了", "用户最近不再喜欢咖啡")

        val result = MemoryConflictDetector.detect(candidate, listOf(existing))

        assertTrue(result is MemoryConflictResult.Conflict)
        assertEquals(existing.id, (result as MemoryConflictResult.Conflict).existing.id)
    }

    @Test
    fun `different topic or same polarity is not marked as conflict`() {
        assertEquals(
            MemoryConflictResult.None,
            MemoryConflictDetector.detect(review("喜欢茶", "用户喜欢茶"), listOf(node("喜欢咖啡", "用户喜欢咖啡")))
        )
        assertEquals(
            MemoryConflictResult.None,
            MemoryConflictDetector.detect(review("喜欢咖啡", "用户还是喜欢咖啡"), listOf(node("喜欢咖啡", "用户喜欢咖啡")))
        )
    }

    @Test
    fun `scene markers explain that conflicting memories may both be valid`() {
        val result = MemoryConflictDetector.detect(
            review("周末不喜欢咖啡", "用户周末不喜欢咖啡"),
            listOf(node("喜欢咖啡", "用户平时喜欢咖啡"))
        )

        assertTrue((result as MemoryConflictResult.Conflict).reason.contains("不同场景"))
    }

    private fun review(title: String, content: String) = MemoryReviewEntity(
        id = "review", scopeKey = "scope", kind = "preference", title = title,
        content = content, confidence = 0.9, proposalHash = title, createdAt = 1
    )

    private fun node(title: String, content: String) = MemoryNodeEntity(
        id = "node", scopeKey = "scope", kind = "preference", subjectRole = "user", subjectKey = "user",
        title = title, content = content, confidence = 0.9, validFrom = 1, createdAt = 1, updatedAt = 1
    )
}
