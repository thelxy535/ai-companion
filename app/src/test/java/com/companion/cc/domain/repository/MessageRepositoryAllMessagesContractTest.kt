package com.companion.cc.domain.repository

import com.companion.cc.domain.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageRepositoryAllMessagesContractTest {
    @Test
    fun allMessagesQueryIsScopedToTheRequestedUser() = runTest {
        val repository = object : MessageRepository {
            override fun getMessages(userId: String, companionId: String, limit: Int, offset: Int): Flow<List<Message>> = flowOf(emptyList())
            override fun getAllMessages(userId: String): Flow<List<Message>> = flowOf(listOf(Message(id = "one", userId = userId, companionId = "custom", role = com.companion.cc.domain.model.MessageRole.USER, content = "saved", timestamp = 1L)))
            override suspend fun saveMessage(message: Message) = Unit
            override suspend fun saveMessages(messages: List<Message>) = Unit
            override suspend fun getMessageCount(userId: String) = 0
            override fun observeMessageCount(userId: String): Flow<Int> = flowOf(0)
            override suspend fun deleteAllMessages(userId: String) = Unit
            override suspend fun deleteMessage(messageId: String) = Unit
            override suspend fun deleteMessagesByCompanion(userId: String, companionId: String) = Unit
            override suspend fun updateMessageImportance(messageId: String, importance: Int) = Unit
            override suspend fun toggleMessageFavorite(messageId: String, isFavorited: Boolean) = Unit
            override fun getFavoritedMessages(userId: String): Flow<List<Message>> = flowOf(emptyList())
            override fun searchMessages(userId: String, companionId: String?, query: String): Flow<List<Message>> = flowOf(emptyList())
            override fun observeLatestMessage(userId: String, companionId: String): Flow<Message?> = flowOf(null)
        }

        assertEquals("user-1", repository.getAllMessages("user-1").first().single().userId)
    }
}
