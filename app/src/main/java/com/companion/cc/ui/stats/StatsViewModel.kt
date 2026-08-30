package com.companion.cc.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.data.local.dao.VectorMemoryDao
import com.companion.cc.data.local.dao.MemoryDao
import com.companion.cc.data.local.dao.MemoryNodeDao
import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Mood
import com.companion.cc.domain.repository.MessageRepository
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.ui.chat.ConversationStats
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.memory.MemoryScopeKey
import com.companion.cc.domain.usage.UsageSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val memoryDao: MemoryDao,
    private val memoryNodeDao: MemoryNodeDao,
    private val vectorMemoryDao: VectorMemoryDao,
    private val settingsManager: SettingsManager,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {

    private var statsJob: Job? = null

    private val _stats = MutableStateFlow(ConversationStats())
    val stats: StateFlow<ConversationStats> = _stats.asStateFlow()

    private val _usage = MutableStateFlow(UsageSummary())
    val usage: StateFlow<UsageSummary> = _usage.asStateFlow()

    private val _emotionalHistory = MutableStateFlow<List<EmotionalState>>(emptyList())
    val emotionalHistory: StateFlow<List<EmotionalState>> = _emotionalHistory.asStateFlow()

    private val _totalMemoryCount = MutableStateFlow(0)
    val totalMemoryCount: StateFlow<Int> = _totalMemoryCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadStats(companionId: String) {
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = currentUserProvider.requireUserId()
                launch {
                    runCatching { settingsManager.usageSummaryFlow(userId, companionId) }
                        .getOrNull()
                        ?.collectLatest { _usage.value = it }
                }
                combine(
                    messageRepository.getMessages(userId, companionId, limit = 1000),
                    memoryDao.observeMemoryCount(userId),
                    memoryNodeDao.observeFiltered(
                        MemoryScopeKey.forCharacter(userId, companionId)
                    ),
                    vectorMemoryDao.observeCount(userId, companionId)
                ) { messages, legacyCount, nodes, vectorCount ->
                    StatsSnapshot(messages, legacyCount + nodes.size, vectorCount)
                }.collectLatest { snapshot ->
                val messages = snapshot.messages
                _isLoading.value = false

                // 计算统计数据
                val userMessages = messages.count { it.role == com.companion.cc.domain.model.MessageRole.USER }
                val assistantMessages = messages.count { it.role == com.companion.cc.domain.model.MessageRole.ASSISTANT }
                val rounds = minOf(userMessages, assistantMessages)

                // 提取主题（从最近的消息中）
                val topics = messages
                    .filter { it.role == com.companion.cc.domain.model.MessageRole.ASSISTANT }
                    .takeLast(10)
                    .map { it.content.take(20) + "..." }
                    .distinct()
                    .take(5)

                // 计算情感评分
                val emotionSum = messages.mapNotNull { msg ->
                    when (msg.emotion?.lowercase()) {
                        "happy", "excited", "joy" -> 1f
                        "sad", "anxious", "angry" -> -1f
                        else -> 0f
                    }
                }.sum()
                val emotionalScore = if (messages.isNotEmpty()) emotionSum / messages.size else 0f

                // 构建情感历史（最近20条消息）
                val emotionHistory = messages
                    .takeLast(20)
                    .mapNotNull { msg ->
                        val mood = when (msg.emotion?.lowercase()) {
                            "happy" -> Mood.HAPPY
                            "excited" -> Mood.EXCITED
                            "sad" -> Mood.SAD
                            "anxious" -> Mood.ANXIOUS
                            "calm" -> Mood.CALM
                            "tired" -> Mood.TIRED
                            "content" -> Mood.CONTENT
                            else -> null
                        }
                        mood?.let {
                            EmotionalState(
                                mood = it,
                                energy = 0.5f,
                                affection = 0.5f,
                                stress = 0.3f,
                                timestamp = msg.timestamp
                            )
                        }
                    }
                _emotionalHistory.value = emotionHistory

                // 提取主要特质（从用户消息中提取关键词）
                val topTraits = extractTopTraits(messages.filter {
                    it.role == com.companion.cc.domain.model.MessageRole.USER
                })

                // 获取记忆统计
                _totalMemoryCount.value = snapshot.memoryCount

                // 获取向量记忆数量
                _stats.value = ConversationStats(
                    totalMessages = messages.size,
                    conversationRounds = rounds,
                    currentTopics = topics,
                    topTraits = topTraits,
                    sessionStartTime = messages.firstOrNull()?.timestamp ?: System.currentTimeMillis(),
                    vectorMemoryCount = snapshot.vectorMemoryCount,
                    emotionalScore = emotionalScore
                )
                    }
            } catch (e: Exception) {
                android.util.Log.e("StatsViewModel", "加载统计失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private data class StatsSnapshot(
        val messages: List<com.companion.cc.domain.model.Message>,
        val memoryCount: Int,
        val vectorMemoryCount: Int
    )

    /**
     * 从用户消息中提取主要特质关键词
     */
    private fun extractTopTraits(userMessages: List<com.companion.cc.domain.model.Message>): List<String> {
        if (userMessages.isEmpty()) return emptyList()

        // 关键词频率统计
        val keywordPattern = """[喜欢|讨厌|爱好|兴趣|工作|学习|运动|音乐|电影|游戏|美食|旅行|阅读|编程|设计|写作]""".toRegex()
        val keywords = mutableMapOf<String, Int>()

        userMessages.forEach { msg ->
            keywordPattern.findAll(msg.content).forEach { match ->
                val keyword = match.value
                keywords[keyword] = keywords.getOrDefault(keyword, 0) + 1
            }
        }

        // 返回前5个高频关键词
        val topKeywords = keywords.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { "${it.key} (${it.value}次提及)" }

        // 如果没有关键词，返回一些基础统计
        return if (topKeywords.isNotEmpty()) {
            topKeywords
        } else {
            val avgLength = userMessages.map { it.content.length }.average().toInt()
            listOf(
                "平均消息长度: $avgLength 字",
                "活跃对话者",
                "总计 ${userMessages.size} 条用户消息"
            )
        }
    }

    fun refreshStats(companionId: String) {
        loadStats(companionId)
    }
}
