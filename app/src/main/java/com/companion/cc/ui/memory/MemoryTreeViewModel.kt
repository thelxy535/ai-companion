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
    private var currentCompanionId: String? = null

    fun loadMessages(companionId: String? = currentCompanionId) {
        currentCompanionId = companionId
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val userId = currentUserProvider.requireUserId()

                // 获取统计信息
                _totalMessages.value = companionId?.let {
                    statsDao.getTotalMessagesForCompanion(userId, it)
                } ?: statsDao.getTotalMessages(userId)
                _totalDays.value = companionId?.let {
                    statsDao.getTotalDaysForCompanion(userId, it)
                } ?: statsDao.getTotalDays(userId)

                // 获取按日期分组的消息计数
                val dateCounts = companionId?.let {
                    statsDao.getMessageCountByDateForCompanion(userId, it)
                } ?: statsDao.getMessageCountByDate(userId)

                // 获取每个日期的消息
                val messagesByDate = dateCounts.map { dateCount ->
                    val messages = companionId?.let {
                        statsDao.getMessagesByDateForCompanion(userId, it, dateCount.date)
                    } ?: statsDao.getMessagesByDate(userId, dateCount.date)
                    MessagesByDate(
                        date = dateCount.date,
                        messages = messages.map { it.toDomain() },
                        count = dateCount.count
                    )
                }

                allMessages = messagesByDate
                applyFilters()

            } catch (_: Exception) {
                // Keep the previous content visible when a reload fails.
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
                statsDao.deleteAllMessagesForCompanion(
                    userId,
                    currentCompanionId ?: return@launch
                )
                _messagesByDate.value = emptyList()
                _totalMessages.value = 0
                _totalDays.value = 0
            } catch (_: Exception) {
                // The UI owns the operation feedback; do not claim success here.
            }
        }
    }

    suspend fun importMemories(jsonContent: String): Result<Int> {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val messages = json.decodeFromString<List<Message>>(jsonContent)

            val userId = settingsManager.userIdFlow.first()
            val companionId = currentCompanionId
                ?: return Result.failure(IllegalStateException("缺少角色范围"))
            val scopedMessages = messages.filter { it.companionId == companionId }

            // 转换为实体并保存
            val entities = scopedMessages.map { message ->
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

            // 重新加载并保留当前角色范围
            loadMessages(currentCompanionId)

            android.util.Log.d("MemoryTreeViewModel", "成功导入 ${messages.size} 条记忆")
            Result.success(scopedMessages.size)
            } catch (_: Exception) {
                Result.failure(Exception("导入记忆失败"))
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
        importance = importance,
        isFavorited = isFavorited
    )
}
