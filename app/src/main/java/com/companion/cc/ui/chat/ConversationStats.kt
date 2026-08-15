package com.companion.cc.ui.chat

/**
 * 对话统计数据
 */
data class ConversationStats(
    val totalMessages: Int = 0,              // 总消息数
    val conversationRounds: Int = 0,          // 对话轮次
    val currentTopics: List<String> = emptyList(), // 当前主题
    val topTraits: List<String> = emptyList(),     // 主要特质
    val sessionStartTime: Long = System.currentTimeMillis(), // 会话开始时间
    val vectorMemoryCount: Int = 0,           // 向量记忆数量
    val emotionalScore: Float = 0f            // 情感评分 (-1.0 到 1.0)
)
