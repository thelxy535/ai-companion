package com.companion.cc.domain.manager

import com.companion.cc.domain.model.MessageRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 对话上下文管理器
 *
 * 管理对话的上下文信息：
 * - 主题追踪：识别和跟踪对话主题
 * - 对话流管理：维护对话的连贯性
 * - 上下文窗口：限制传递给 LLM 的上下文大小
 * - 重要信息提取：识别关键信息
 */
@Singleton
class ContextManager @Inject constructor() {
    // 当前对话主题（每个用户独立）
    // Key: "userId:companionId"
    private val topicsMap = mutableMapOf<String, MutableList<String>>()

    private val _currentTopics = MutableStateFlow<List<String>>(emptyList())
    val currentTopics: StateFlow<List<String>> = _currentTopics.asStateFlow()

    // 对话轮次计数
    private val conversationTurnsMap = mutableMapOf<String, Int>()

    private val _conversationRounds = MutableStateFlow(0)
    val conversationRounds: StateFlow<Int> = _conversationRounds.asStateFlow()

    private var currentKey: String = ""

    /**
     * 设置当前会话
     */
    fun setCurrentSession(userId: String, companionId: String) {
        currentKey = getKey(userId, companionId)
        _currentTopics.value = topicsMap[currentKey]?.toList() ?: emptyList()
        _conversationRounds.value = conversationTurnsMap[currentKey] ?: 0
    }

    /**
     * 添加消息到上下文
     */
    fun addMessage(content: String, role: MessageRole) {
        if (role == MessageRole.USER && currentKey.isNotEmpty()) {
            // 更新轮次
            val currentRounds = conversationTurnsMap.getOrDefault(currentKey, 0)
            conversationTurnsMap[currentKey] = currentRounds + 1
            _conversationRounds.value = currentRounds + 1

            // 提取主题
            val topics = extractTopics(content)
            if (topics.isNotEmpty()) {
                val currentList = topicsMap.getOrPut(currentKey) { mutableListOf() }
                topics.forEach { topic ->
                    if (!currentList.contains(topic)) {
                        currentList.add(0, topic)
                    }
                }
                if (currentList.size > 5) {
                    currentList.subList(5, currentList.size).clear()
                }
                _currentTopics.value = currentList.toList()
            }
        }
    }

    /**
     * 重置对话上下文
     */
    fun reset(userId: String, companionId: String) {
        val key = getKey(userId, companionId)
        topicsMap.remove(key)
        conversationTurnsMap.remove(key)
        if (key == currentKey) {
            _currentTopics.value = emptyList()
            _conversationRounds.value = 0
        }
    }

    /**
     * 提取消息中的主题
     *
     * 简单的关键词提取（后续可替换为 NLP 模型）
     */
    private fun extractTopics(message: String): List<String> {
        val topics = mutableListOf<String>()

        val topicKeywords = mapOf(
            "工作" to listOf("工作", "上班", "项目", "任务", "会议", "同事", "老板"),
            "学习" to listOf("学习", "考试", "课程", "作业", "复习", "预习"),
            "生活" to listOf("生活", "日常", "吃饭", "睡觉", "购物", "家务"),
            "娱乐" to listOf("游戏", "电影", "音乐", "旅游", "运动", "健身"),
            "情感" to listOf("开心", "难过", "生气", "焦虑", "压力", "喜欢", "讨厌"),
            "健康" to listOf("健康", "身体", "医院", "医生", "药", "生病", "锻炼")
        )

        topicKeywords.forEach { (topic, keywords) ->
            if (keywords.any { message.contains(it) }) {
                topics.add(topic)
            }
        }

        return topics.take(3)
    }

    private fun getKey(userId: String, companionId: String): String {
        return "$userId:$companionId"
    }
}
