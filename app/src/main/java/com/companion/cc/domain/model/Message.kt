package com.companion.cc.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val userId: String,
    val companionId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long,
    val emotion: String? = null,
    val mentionedOther: Boolean = false,
    val isDualConversation: Boolean = false,
    val replyToId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val importance: Int = 50, // 0-100, 重要程度
    val action: String? = null, // 动作描述（可观察的行为、表情、姿态）
    val isFavorited: Boolean = false, // 是否收藏

    // 视觉感知扩展（多模态支持）
    val imageUrl: String? = null,        // 图片 URL（可以是本地 file:// 或云端 https://）
    val imageAnalysis: String? = null,   // 视觉理解结果（结构化描述，由 VisionManager 填充）
    val origin: String = "chat"          // chat / proactive / system
)

@Serializable
enum class MessageRole {
    USER, ASSISTANT
}
