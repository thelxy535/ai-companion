package com.companion.cc.domain.manager

import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MemoryLayered
import com.companion.cc.domain.repository.MessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 中期记忆管理器
 *
 * 负责生成和管理中期记忆（最近N天的摘要）：
 * - 按天分组消息
 * - 生成每日摘要
 * - 缓存摘要结果
 * - 提取关键主题和情感
 */
@Singleton
class MidTermMemoryManager @Inject constructor(
    private val messageRepository: MessageRepository
) {
    // 摘要缓存
    // Key: "userId:companionId:date"
    private val summaryCache = mutableMapOf<String, MemoryLayered.MidTerm>()

    /**
     * 获取中期记忆（最近N天的摘要）
     *
     * @param userId 用户ID
     * @param companionId 伴侣ID
     * @param days 天数（默认7天）
     * @return 中期记忆列表
     */
    suspend fun getMidTermMemories(
        userId: String,
        companionId: String,
        days: Int = 7
    ): List<MemoryLayered.MidTerm> = withContext(Dispatchers.Default) {
        val memories = mutableListOf<MemoryLayered.MidTerm>()

        // 获取最近N天的日期
        val dateRanges = getDateRanges(days)

        dateRanges.forEach { (date, startTime, endTime) ->
            // 检查缓存
            val cacheKey = getCacheKey(userId, companionId, date)
            val cached = summaryCache[cacheKey]

            val memory = if (cached != null) {
                cached
            } else {
                // 生成新摘要
                generateDailySummary(
                    userId = userId,
                    companionId = companionId,
                    date = date,
                    startTime = startTime,
                    endTime = endTime
                )?.also {
                    // 缓存结果
                    summaryCache[cacheKey] = it
                }
            }

            memory?.let { memories.add(it) }
        }

        memories
    }

    /**
     * 生成每日摘要
     */
    private suspend fun generateDailySummary(
        userId: String,
        companionId: String,
        date: String,
        startTime: Long,
        endTime: Long
    ): MemoryLayered.MidTerm? {
        // 获取这一天的所有消息
        val messages = messageRepository.getMessages(
            userId = userId,
            companionId = companionId,
            limit = 1000,
            offset = 0
        ).first().filter { message ->
            message.timestamp in startTime..endTime
        }

        if (messages.isEmpty()) {
            return null
        }

        // 提取主题
        val topics = extractTopics(messages)

        // 提取情感
        val emotions = extractEmotions(messages)
        val dominantEmotion = emotions.maxByOrNull { it.value }?.key

        // 生成摘要
        val summary = generateSummary(messages, topics, emotions)

        // 计算重要性
        val importance = calculateImportance(messages, topics)

        return MemoryLayered.MidTerm(
            id = "midterm_${userId}_${companionId}_${date}",
            userId = userId,
            timestamp = startTime,
            importance = importance,
            summary = summary,
            dateRange = startTime..endTime,
            messageIds = messages.map { it.id },
            topics = topics,
            emotion = dominantEmotion
        )
    }

    /**
     * 提取主题
     */
    private fun extractTopics(messages: List<Message>): List<String> {
        val topicCounts = mutableMapOf<String, Int>()

        val topicKeywords = mapOf(
            "工作" to listOf("工作", "加班", "项目", "开会", "同事", "领导"),
            "学习" to listOf("学习", "考试", "作业", "课程", "老师"),
            "感情" to listOf("男朋友", "女朋友", "喜欢", "爱", "恋爱"),
            "家庭" to listOf("父母", "爸爸", "妈妈", "家人", "孩子"),
            "健康" to listOf("身体", "健康", "生病", "医院", "运动"),
            "情绪" to listOf("开心", "难过", "生气", "焦虑", "压力"),
            "兴趣" to listOf("电影", "音乐", "游戏", "旅行", "美食"),
            "哲学" to listOf("哲学", "思考", "意义", "真理", "存在"),
            "科学" to listOf("科学", "物理", "数学", "技术", "AI")
        )

        messages.forEach { message ->
            topicKeywords.forEach { (topic, keywords) ->
                if (keywords.any { message.content.contains(it) }) {
                    topicCounts[topic] = topicCounts.getOrDefault(topic, 0) + 1
                }
            }
        }

        // 返回出现次数最多的前3个主题
        return topicCounts.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
    }

    /**
     * 提取情感
     */
    private fun extractEmotions(messages: List<Message>): Map<String, Int> {
        val emotionCounts = mutableMapOf<String, Int>()

        messages.forEach { message ->
            message.emotion?.let { emotion ->
                emotionCounts[emotion] = emotionCounts.getOrDefault(emotion, 0) + 1
            }
        }

        return emotionCounts
    }

    /**
     * 生成摘要文本
     *
     * 简单的基于规则的摘要生成
     * TODO: 后续可以使用 LLM 生成更智能的摘要
     */
    private fun generateSummary(
        messages: List<Message>,
        topics: List<String>,
        emotions: Map<String, Int>
    ): String {
        val parts = mutableListOf<String>()

        // 基本统计
        val totalMessages = messages.size
        val userMessages = messages.count { it.role == com.companion.cc.domain.model.MessageRole.USER }
        val aiMessages = totalMessages - userMessages

        // 主题描述
        if (topics.isNotEmpty()) {
            parts.add("主要讨论了${topics.joinToString("、")}等话题")
        }

        // 对话量描述
        parts.add("共进行了${totalMessages}轮对话")

        // 情感描述
        val dominantEmotion = emotions.maxByOrNull { it.value }?.key
        if (dominantEmotion != null) {
            val emotionText = when (dominantEmotion) {
                "HAPPY" -> "整体气氛较为愉快"
                "SAD" -> "有一些难过的时刻"
                "EXCITED" -> "充满了兴奋和期待"
                "CALM" -> "保持了平静的状态"
                "ANXIOUS" -> "有些焦虑和担心"
                "TIRED" -> "感觉有些疲惫"
                else -> "情绪较为平稳"
            }
            parts.add(emotionText)
        }

        // 重要信息提取（高重要性的消息）
        val importantMessages = messages.filter { it.importance >= 80 }
        if (importantMessages.isNotEmpty()) {
            parts.add("记录了${importantMessages.size}条重要信息")
        }

        return parts.joinToString("，") + "。"
    }

    /**
     * 计算重要性
     */
    private fun calculateImportance(
        messages: List<Message>,
        topics: List<String>
    ): Float {
        var importance = 0.3f  // 基础重要性

        // 消息数量影响
        val messageCount = messages.size
        importance += (messageCount / 50f).coerceIn(0f, 0.3f)

        // 主题多样性
        importance += (topics.size * 0.1f)

        // 高重要性消息的比例
        val highImportanceRatio = messages.count { it.importance >= 80 } / messages.size.toFloat()
        importance += highImportanceRatio * 0.3f

        return importance.coerceIn(0f, 1f)
    }

    /**
     * 获取日期范围
     */
    private fun getDateRanges(days: Int): List<Triple<String, Long, Long>> {
        val ranges = mutableListOf<Triple<String, Long, Long>>()
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (i in 0 until days) {
            // 设置为当天的开始
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis

            // 设置为当天的结束
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endTime = calendar.timeInMillis

            val date = dateFormat.format(calendar.time)
            ranges.add(Triple(date, startTime, endTime))

            // 前一天
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }

        return ranges
    }

    /**
     * 获取缓存键
     */
    private fun getCacheKey(userId: String, companionId: String, date: String): String {
        return "$userId:$companionId:$date"
    }

    /**
     * 清除缓存
     */
    fun clearCache(userId: String, companionId: String) {
        val prefix = "$userId:$companionId:"
        summaryCache.keys.removeAll { it.startsWith(prefix) }
    }

    /**
     * 清除所有缓存
     */
    fun clearAllCache() {
        summaryCache.clear()
    }
}
