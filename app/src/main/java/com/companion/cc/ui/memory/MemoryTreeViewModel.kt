package com.companion.cc.ui.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.data.local.dao.StatsDao
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import com.companion.cc.domain.model.MessagesByDate
import com.companion.cc.domain.identity.CurrentUserProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class MemoryTreeViewModel @Inject constructor(
    private val statsDao: StatsDao,
    private val settingsManager: SettingsManager,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {

    private val _messagesByDate = MutableStateFlow<List<MessagesByDate>>(emptyList())
    val messagesByDate: StateFlow<List<MessagesByDate>> = _messagesByDate.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _totalMessages = MutableStateFlow(0)
    val totalMessages: StateFlow<Int> = _totalMessages.asStateFlow()

    private val _totalDays = MutableStateFlow(0)
    val totalDays: StateFlow<Int> = _totalDays.asStateFlow()

    private var allMessages: List<MessagesByDate> = emptyList()
    private var currentFilter: String = "全部"
    private var currentSearchQuery: String = ""

    fun loadMessages() {
        viewModelScope.launch {
            _isLoading.value = true
            android.util.Log.d("MemoryTreeViewModel", "开始加载消息...")

            try {
                val userId = currentUserProvider.requireUserId()
                android.util.Log.d("MemoryTreeViewModel", "用户ID: $userId")

                // 获取统计信息
                _totalMessages.value = statsDao.getTotalMessages(userId)
                _totalDays.value = statsDao.getTotalDays(userId)
                android.util.Log.d("MemoryTreeViewModel", "总消息数: ${_totalMessages.value}, 总天数: ${_totalDays.value}")

                // 获取按日期分组的消息计数
                val dateCounts = statsDao.getMessageCountByDate(userId)
                android.util.Log.d("MemoryTreeViewModel", "日期分组数: ${dateCounts.size}")

                // 获取每个日期的消息
                val messagesByDate = dateCounts.map { dateCount ->
                    val messages = statsDao.getMessagesByDate(userId, dateCount.date)
                    android.util.Log.d("MemoryTreeViewModel", "日期 ${dateCount.date}: ${messages.size} 条消息")
                    MessagesByDate(
                        date = dateCount.date,
                        messages = messages.map { it.toDomain() },
                        count = dateCount.count
                    )
                }

                allMessages = messagesByDate
                _messagesByDate.value = messagesByDate
                android.util.Log.d("MemoryTreeViewModel", "加载完成，共 ${messagesByDate.size} 个日期组")

            } catch (e: Exception) {
                android.util.Log.e("MemoryTreeViewModel", "加载消息失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchMessages(query: String) {
        currentSearchQuery = query
        applyFilters()
    }

    fun filterMessages(filter: String) {
        currentFilter = filter
        applyFilters()
    }

    private fun applyFilters() {
        var filtered = allMessages

        // 应用搜索
        if (currentSearchQuery.isNotBlank()) {
            filtered = filtered.map { dateGroup ->
                val filteredMessages = dateGroup.messages.filter { message ->
                    message.content.contains(currentSearchQuery, ignoreCase = true)
                }
                dateGroup.copy(
                    messages = filteredMessages,
                    count = filteredMessages.size
                )
            }.filter { it.messages.isNotEmpty() }
        }

        // 应用过滤器
        filtered = when (currentFilter) {
            "user" -> filtered.map { dateGroup ->
                val filteredMessages = dateGroup.messages.filter { it.role == MessageRole.USER }
                dateGroup.copy(
                    messages = filteredMessages,
                    count = filteredMessages.size
                )
            }.filter { it.messages.isNotEmpty() }

            "assistant" -> filtered.map { dateGroup ->
                val filteredMessages = dateGroup.messages.filter { it.role == MessageRole.ASSISTANT }
                dateGroup.copy(
                    messages = filteredMessages,
                    count = filteredMessages.size
                )
            }.filter { it.messages.isNotEmpty() }

            "important" -> filtered.map { dateGroup ->
                val filteredMessages = dateGroup.messages.filter { it.importance > 80 }
                dateGroup.copy(
                    messages = filteredMessages,
                    count = filteredMessages.size
                )
            }.filter { it.messages.isNotEmpty() }

            else -> filtered
        }

        _messagesByDate.value = filtered
    }

    fun exportMemories(): String {
        val allMessagesFlat = _messagesByDate.value.flatMap { it.messages }
        return Json {
            prettyPrint = true
        }.encodeToString(allMessagesFlat)
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            try {
                val userId = settingsManager.userIdFlow.first()
                statsDao.deleteAllMessages(userId)
                _messagesByDate.value = emptyList()
                _totalMessages.value = 0
                _totalDays.value = 0
                android.util.Log.d("MemoryTreeViewModel", "已清理所有记忆")
            } catch (e: Exception) {
                android.util.Log.e("MemoryTreeViewModel", "清理记忆失败", e)
            }
        }
    }

    suspend fun importMemories(jsonContent: String): Result<Int> {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val messages = json.decodeFromString<List<Message>>(jsonContent)

            val userId = settingsManager.userIdFlow.first()

            // 转换为实体并保存
            val entities = messages.map { message ->
                com.companion.cc.data.local.entity.MessageEntity(
                    id = message.id,
                    userId = userId,  // 使用当前用户 ID
                    companionId = message.companionId,
                    role = if (message.role == MessageRole.USER) "user" else "assistant",
                    content = message.content,
                    timestamp = message.timestamp,
                    emotion = message.emotion,
                    mentionedOther = message.mentionedOther,
                    isDualConversation = message.isDualConversation,
                    replyToId = message.replyToId,
                    createdAt = message.createdAt,
                    importance = message.importance,
                    imageUrl = null  // 导入不包含图片
                )
            }

            // 批量插入数据库
            statsDao.insertMessages(entities)

            // 重新加载
            loadMessages()

            android.util.Log.d("MemoryTreeViewModel", "成功导入 ${messages.size} 条记忆")
            Result.success(messages.size)
        } catch (e: Exception) {
            android.util.Log.e("MemoryTreeViewModel", "导入记忆失败", e)
            Result.failure(e)
        }
    }

    private fun com.companion.cc.data.local.entity.MessageEntity.toDomain() = Message(
        id = id,
        userId = userId,
        companionId = companionId,
        role = if (role == "user") MessageRole.USER else MessageRole.ASSISTANT,
        content = content,
        timestamp = timestamp,
        emotion = emotion,
        mentionedOther = mentionedOther,
        isDualConversation = isDualConversation,
        replyToId = replyToId,
        createdAt = createdAt,
        importance = importance
    )
}
