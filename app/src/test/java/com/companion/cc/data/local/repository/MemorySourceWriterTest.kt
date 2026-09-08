package com.companion.cc.data.local.repository

import com.companion.cc.data.local.dao.MemorySourceDao
import com.companion.cc.data.local.entity.MemorySourceEntity
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class MemorySourceWriterTest {
    private val sourceDao = mock<MemorySourceDao>()
    private val writer = MemorySourceWriter(sourceDao)

    @Test
    fun `retain is idempotent for the same scope and content`() = runTest {
        whenever(sourceDao.findByContentHashInScope(any(), any())).thenReturn(null)

        val first = writer.retain(message(companionId = "companion-a"), now = 100L)
        whenever(sourceDao.findByContentHashInScope(first.scopeKey, first.contentHash)).thenReturn(first)
        val second = writer.retain(message(companionId = "companion-a"), now = 200L)

        assertEquals(first, second)
        assertEquals("user:user-1:companion:companion-a", first.scopeKey)
        verify(sourceDao).insert(first)

    }

    @Test
    fun `scope is part of hash so companions cannot share a source identity`() = runTest {
        whenever(sourceDao.findByContentHashInScope(any(), any())).thenReturn(null)

        val first = writer.retain(message(companionId = "companion-a"), now = 100L)
        val second = writer.retain(message(companionId = "companion-b"), now = 100L)

        assertNotEquals(first.scopeKey, second.scopeKey)
        assertNotEquals(first.contentHash, second.contentHash)
        assertNotEquals(first.id, second.id)
    }

    @Test
    fun `image messages retain multimodal evidence`() = runTest {
        val message = message(
            companionId = "companion-a",
            content = "这是什么？",
            imageUrl = "content://image/1",
            imageAnalysis = "一杯茶"
        )
        whenever(sourceDao.findByContentHashInScope(any(), any())).thenReturn(null)

        val source = writer.retain(message, now = 123L)

        assertEquals("message_image", source.sourceType)
        assertTrue(source.contentSnapshot.contains("[image] content://image/1"))
        assertTrue(source.contentSnapshot.contains("[image-analysis] 一杯茶"))
        assertEquals(123L, source.createdAt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank user id is rejected before touching the dao`() = runTest {
        writer.retain(message(userId = " ", companionId = "companion-a"))
    }

    private fun message(
        userId: String = "user-1",
        companionId: String,
        content: String = "记住我喜欢茶",
        imageUrl: String? = null,
        imageAnalysis: String? = null
    ) = Message(
        id = "message-1-$companionId",
        userId = userId,
        companionId = companionId,
        role = MessageRole.USER,
        content = content,
        timestamp = 10L,
        imageUrl = imageUrl,
        imageAnalysis = imageAnalysis
    )
}
