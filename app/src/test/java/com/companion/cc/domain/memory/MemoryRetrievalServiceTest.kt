package com.companion.cc.domain.memory

import com.companion.cc.data.local.entity.MemoryNodeEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryRetrievalServiceTest {
    @Test
    fun rankingPrefersTitleMatch() {
        val now = 1000L
        val title = node("a", "茶偏好", "用户喜欢喝茶")
        val other = node("b", "旅行", "用户去过很多地方")
        val result = MemoryRetrievalService.rank("茶", listOf(other, title), now)
        assertEquals("a", result.first().id)
    }

    private fun node(id: String, title: String, content: String) = MemoryNodeEntity(id, "companion:a", "preference", "user", "user", title, content, validFrom = 0, createdAt = 0, updatedAt = 0)
}
