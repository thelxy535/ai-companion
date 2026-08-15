package com.companion.cc.domain.model

import java.time.LocalDateTime

/**
 * 用户事件
 *
 * 记录用户的行为事件
 */
data class UserEvent(
    val id: String,
    val userId: String,
    val companionId: String,
    val eventType: EventType,
    val timestamp: LocalDateTime,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 事件类型
 */
enum class EventType(val displayName: String) {
    // 消息事件
    MESSAGE_SENT("发送消息"),
    MESSAGE_WITH_IMAGE("发送带图片的消息"),
    MESSAGE_VOICE("发送语音消息"),

    // 应用事件
    APP_OPENED("打开应用"),
    APP_CLOSED("关闭应用"),
    COMPANION_SWITCHED("切换角色"),

    // 行为事件
    TYPING_STARTED("开始打字"),
    TYPING_PAUSED("停止打字"),
    LONG_READ("长时间阅读"),
    QUICK_REPLY("快速回复"),

    // 交互事件
    MESSAGE_DELETED("删除消息"),
    MESSAGE_EDITED("编辑消息"),
    CONVERSATION_CLEARED("清空对话"),

    // AI回复事件
    AI_REPLY_RECEIVED("收到AI回复"),
    AI_REPLY_INTERRUPTED("中断AI回复")
}

/**
 * 消息节奏分析
 */
data class MessageRhythm(
    val recentMessageCount: Int,           // 最近N分钟的消息数
    val averageInterval: Long,              // 平均回复间隔（秒）
    val isRapidFire: Boolean,              // 是否连续快速发送
    val isPaused: Boolean,                 // 是否停顿很久
    val urgencyLevel: UrgencyLevel         // 急迫程度
) {
    fun toHumanReadable(): String {
        return when {
            isRapidFire -> "连续快速发送（${recentMessageCount}条/分钟）"
            isPaused -> "停顿较久（平均${averageInterval}秒/条）"
            else -> "正常节奏（平均${averageInterval}秒/条）"
        }
    }
}

/**
 * 急迫程度
 */
enum class UrgencyLevel(val displayName: String) {
    RELAXED("放松"),       // 慢速回复
    NORMAL("正常"),        // 正常
    EAGER("积极"),         // 快速回复
    URGENT("急迫")         // 连续快速发送
}

/**
 * 用户状态
 */
data class UserState(
    val isActive: Boolean,                 // 是否活跃
    val urgencyLevel: UrgencyLevel,        // 急迫程度
    val attentionLevel: AttentionLevel     // 专注程度
) {
    fun toHumanReadable(): String {
        val parts = mutableListOf<String>()

        if (!isActive) {
            parts.add("不活跃")
        }

        parts.add("急迫程度：${urgencyLevel.displayName}")
        parts.add("专注程度：${attentionLevel.displayName}")

        return parts.joinToString("，")
    }
}

/**
 * 专注程度
 */
enum class AttentionLevel(val displayName: String) {
    DISTRACTED("分心"),    // 快速切换、中断多
    CASUAL("随意"),        // 偶尔回复
    FOCUSED("专注"),       // 持续对话
    IMMERSED("沉浸")       // 深度交流
}

/**
 * 事件上下文
 */
data class EventContext(
    val recentEvents: List<UserEvent>,     // 最近的事件
    val messageRhythm: MessageRhythm,      // 消息节奏
    val userState: UserState               // 用户状态
) {
    fun toHumanReadable(): String {
        val sb = StringBuilder()

        sb.append("消息节奏：${messageRhythm.toHumanReadable()}\n")
        sb.append("用户状态：${userState.toHumanReadable()}")

        return sb.toString()
    }
}
