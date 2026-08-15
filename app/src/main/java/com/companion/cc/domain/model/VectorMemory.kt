package com.companion.cc.domain.model

/**
 * 向量化的记忆
 */
data class VectorMemory(
    val id: String,
    val content: String,              // 原始内容
    val embedding: FloatArray,        // 向量表示
    val type: MemoryType,            // 记忆类型
    val timestamp: Long,
    val userId: String,
    val companionId: String,
    val metadata: Map<String, String> = emptyMap()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VectorMemory

        if (id != other.id) return false
        if (content != other.content) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}

/**
 * 记忆类型
 */
enum class MemoryType {
    USER_INFO,      // 用户信息（姓名、喜好等）
    EVENT,          // 重要事件
    EMOTION,        // 情感记录
    CONVERSATION,   // 对话片段
    PREFERENCE      // 偏好和厌恶
}

/**
 * 记忆检索结果
 */
data class MemorySearchResult(
    val memory: VectorMemory,
    val similarity: Float  // 相似度分数 0-1
)
