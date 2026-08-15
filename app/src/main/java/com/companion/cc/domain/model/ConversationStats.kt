package com.companion.cc.domain.model

data class ConversationStats(
    val totalMessages: Int,
    val userMessages: Int,
    val assistantMessages: Int,
    val totalDays: Int,
    val companionStats: Map<String, CompanionStat>
)

data class CompanionStat(
    val companionId: String,
    val messageCount: Int,
    val lastMessageTime: Long
)

data class MessagesByDate(
    val date: String, // YYYY-MM-DD
    val messages: List<Message>,
    val count: Int
)
