package com.companion.cc.data.repository

import com.companion.cc.data.local.dao.MessageDao
import com.companion.cc.data.local.entity.MessageEntity
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.times

/**
 * MessageRepositoryImpl 单元测试
 *
 * 测试消息仓库的核心功能
 */
class MessageRepositoryImplTest {

    @Mock
    private lateinit var messageDao: MessageDao

    private lateinit var repository: MessageRepositoryImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = MessageRepositoryImpl(messageDao)
    }

    @Test
    fun `getMessages should return mapped domain models`() = runTest {
        // Given
        val entities = listOf(
            createMessageEntity(id = "1", role = "user", content = "Hello"),
            createMessageEntity(id = "2", role = "assistant", content = "Hi there")
        )
        `when`(messageDao.getMessages("user1", "companion1", 50, 0))
            .thenReturn(flowOf(entities))

        // When
        val result = repository.getMessages("user1", "companion1", 50, 0).first()

        // Then
        assertEquals("应该返回 2 条消息", 2, result.size)
        assertEquals("第一条应该是用户消息", MessageRole.USER, result[0].role)
        assertEquals("第二条应该是助手消息", MessageRole.ASSISTANT, result[1].role)
        assertEquals("内容应该正确", "Hello", result[0].content)
    }

    @Test
    fun `saveMessage should call dao insertMessage`() = runTest {
        // Given
        val message = Message(
            id = "1",
            userId = "user1",
            companionId = "companion1",
            role = MessageRole.USER,
            content = "Test message",
            timestamp = 1000L
        )
        val captor = argumentCaptor<MessageEntity>()

        // When
        repository.saveMessage(message)

        // Then
        verify(messageDao, times(1)).insertMessage(captor.capture())
        assertEquals("消息ID应该正确", "1", captor.firstValue.id)
        assertEquals("内容应该正确", "Test message", captor.firstValue.content)
    }

    @Test
    fun `deleteMessage should call dao deleteMessage`() = runTest {
        // Given
        val messageId = "msg123"

        // When
        repository.deleteMessage(messageId)

        // Then
        verify(messageDao, times(1)).deleteMessage(messageId)
    }

    @Test
    fun `searchMessages should return filtered results`() = runTest {
        // Given
        val query = "hello"
        val entities = listOf(
            createMessageEntity(id = "1", content = "Hello world"),
            createMessageEntity(id = "2", content = "Hello there")
        )
        `when`(messageDao.searchMessages("user1", "companion1", query))
            .thenReturn(flowOf(entities))

        // When
        val result = repository.searchMessages("user1", "companion1", query).first()

        // Then
        assertEquals("应该返回 2 条匹配消息", 2, result.size)
        assertTrue("第一条应该包含搜索词", result[0].content.contains("Hello", ignoreCase = true))
    }

    @Test
    fun `searchMessages with null companionId should search all conversations`() = runTest {
        // Given
        val query = "test"
        val entities = listOf(
            createMessageEntity(id = "1", content = "test message")
        )
        `when`(messageDao.searchMessages("user1", null, query))
            .thenReturn(flowOf(entities))

        // When
        val result = repository.searchMessages("user1", null, query).first()

        // Then
        assertEquals("应该返回搜索结果", 1, result.size)
        verify(messageDao, times(1)).searchMessages("user1", null, query)
    }

    @Test
    fun `getMessageCount should return correct count`() = runTest {
        // Given
        `when`(messageDao.getMessageCount("user1")).thenReturn(42)

        // When
        val count = repository.getMessageCount("user1")

        // Then
        assertEquals("应该返回正确的消息数量", 42, count)
    }

    @Test
    fun `deleteAllMessages should call dao deleteAllMessages`() = runTest {
        // Given
        val userId = "user1"

        // When
        repository.deleteAllMessages(userId)

        // Then
        verify(messageDao, times(1)).deleteAllMessages(userId)
    }

    // Helper function to create MessageEntity
    private fun createMessageEntity(
        id: String = "1",
        userId: String = "user1",
        companionId: String = "companion1",
        role: String = "user",
        content: String = "Test",
        timestamp: Long = 1000L
    ) = MessageEntity(
        id = id,
        userId = userId,
        companionId = companionId,
        role = role,
        content = content,
        timestamp = timestamp,
        emotion = null,
        mentionedOther = false,
        isDualConversation = false,
        replyToId = null,
        createdAt = timestamp,
        importance = 0
    )
}
