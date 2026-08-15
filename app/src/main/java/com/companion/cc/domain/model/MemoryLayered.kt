package com.companion.cc.domain.model

/**
 * 分层记忆基类
 *
 * 支持4种记忆层级：
 * - 短期记忆（会话内，最近100条）
 * - 中期记忆（最近7天的摘要）
 * - 长期记忆（向量检索，永久保存）
 * - 永久记忆（核心人格特质）
 */
sealed class MemoryLayered {
    abstract val id: String
    abstract val userId: String
    abstract val timestamp: Long
    abstract val importance: Float  // 0-1 范围

    /**
     * 短期记忆（会话内）
     * 存储最近的对话消息，用于维持对话连贯性
     */
    data class ShortTerm(
        override val id: String,
        override val userId: String,
        override val timestamp: Long,
        override val importance: Float,
        val message: Message
    ) : MemoryLayered()

    /**
     * 中期记忆（最近N天）
     * 存储最近几天的对话摘要，用于回忆最近的事件
     */
    data class MidTerm(
        override val id: String,
        override val userId: String,
        override val timestamp: Long,
        override val importance: Float,
        val summary: String,                    // 摘要文本
        val dateRange: LongRange,               // 时间范围
        val messageIds: List<String>,           // 原始消息ID
        val topics: List<String>,               // 主题标签
        val emotion: String?                    // 整体情感
    ) : MemoryLayered()

    /**
     * 长期记忆（向量检索）
     * 使用向量嵌入存储重要对话，支持语义搜索
     */
    data class LongTerm(
        override val id: String,
        override val userId: String,
        override val timestamp: Long,
        override val importance: Float,
        val embedding: FloatArray,              // 向量嵌入
        val content: String,                    // 原始内容
        val topics: List<String>,               // 主题标签
        val emotion: String?,                   // 情感
        val metadata: Map<String, Any> = emptyMap()
    ) : MemoryLayered() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as LongTerm

            if (id != other.id) return false
            if (userId != other.userId) return false
            if (!embedding.contentEquals(other.embedding)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + userId.hashCode()
            result = 31 * result + embedding.contentHashCode()
            return result
        }
    }

    /**
     * 永久记忆（核心人格）
     * 存储伴侣的核心人格特质，不会被遗忘
     */
    data class Permanent(
        override val id: String,
        override val userId: String,
        override val timestamp: Long,
        override val importance: Float = 1.0f,  // 永久记忆总是最重要
        val category: PersonalityCategory,       // 人格类别
        val trait: String,                       // 特质描述
        val examples: List<String> = emptyList() // 示例行为
    ) : MemoryLayered()
}

/**
 * 人格类别
 */
enum class PersonalityCategory {
    CORE_VALUES,        // 核心价值观
    INTERESTS,          // 兴趣爱好
    SPEAKING_STYLE,     // 说话风格
    BEHAVIOR_PATTERN,   // 行为模式
    RELATIONSHIP,       // 关系定位
    BACKGROUND          // 背景故事
}
