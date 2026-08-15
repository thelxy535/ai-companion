package com.companion.cc.domain.model

/**
 * 记忆条目
 */
data class Memory(
    val id: String,
    val userId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long,
    val date: String, // YYYY-MM-DD
    val importance: Int, // 0-100
    val emotion: String?,
    val topics: List<String> = emptyList()
)

/**
 * 用户画像
 */
data class UserProfile(
    val userId: String,
    val basicInfo: BasicInfo = BasicInfo(),
    val personality: Personality = Personality(),
    val interests: List<String> = emptyList(),
    val relationships: List<String> = emptyList(),
    val importantEvents: List<String> = emptyList(),
    val habits: List<String> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class BasicInfo(
    val name: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val occupation: String? = null,
    val location: String? = null
)

data class Personality(
    val traits: List<String> = emptyList(), // 性格特点
    val values: List<String> = emptyList(), // 价值观
    val communicationStyle: String? = null // 沟通风格
)

/**
 * 记忆检索结果
 */
data class MemoryContext(
    val shortTermMemories: List<Memory> = emptyList(), // 最近10条
    val userProfile: UserProfile? = null,
    val relevantMemories: List<Memory> = emptyList() // 相关的历史记忆
)
