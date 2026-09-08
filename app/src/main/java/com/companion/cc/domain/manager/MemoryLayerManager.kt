package com.companion.cc.domain.manager

import com.companion.cc.domain.model.*
import com.companion.cc.domain.repository.MessageRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 记忆层管理器
 *
 * 统一管理4层记忆系统：
 * - 短期记忆（Short-term）：会话内的最近消息
 * - 中期记忆（Mid-term）：最近几天的摘要
 * - 长期记忆（Long-term）：向量检索的重要记忆
 * - 永久记忆（Permanent）：核心人格特质
 *
 * 参考 Moeru-AI 的多层记忆架构
 */
@Singleton
class MemoryLayerManager @Inject constructor(
    private val messageRepository: MessageRepository,
    private val vectorMemoryManager: VectorMemoryManager,
    private val midTermMemoryManager: MidTermMemoryManager,
    private val personalityManager: PersonalityManager,
    private val config: MemoryLayerConfig = MemoryLayerConfig()
) {
    /**
     * 永久记忆缓存
     * Key: "permanent-companionId"
     * Value: 永久记忆列表（来自静态配置，只需计算一次）
     */
    private val permanentMemoryCache = ConcurrentHashMap<String, List<MemoryLayered.Permanent>>()

    /**
     * 获取完整的记忆上下文
     *
     * @param userId 用户ID
     * @param companionId 伴侣ID
     * @param currentMessage 当前消息（用于向量检索）
     * @return 完整的记忆上下文
     */
    suspend fun getMemoryContext(
        userId: String,
        companionId: String,
        currentMessage: String
    ): CompleteMemoryContext {
        return CompleteMemoryContext(
            shortTerm = getShortTermMemory(userId, companionId),
            midTerm = getMidTermMemory(userId, companionId),
            longTerm = getLongTermMemory(userId, companionId, currentMessage),
            permanent = getPermanentMemory(userId, companionId)
        )
    }

    /**
     * 获取短期记忆（会话内最近消息）
     */
    private suspend fun getShortTermMemory(
        userId: String,
        companionId: String
    ): List<MemoryLayered.ShortTerm> {
        val messages = messageRepository.getLatestMessages(
            userId = userId,
            companionId = companionId,
            limit = config.shortTermCapacity
        ).first()

        return messages.map { message ->
            MemoryLayered.ShortTerm(
                id = message.id,
                userId = message.userId,
                timestamp = message.timestamp,
                importance = message.importance / 100f,  // 转换为 0-1 范围
                message = message
            )
        }
    }

    /**
     * 获取中期记忆（最近N天的摘要）
     */
    private suspend fun getMidTermMemory(
        userId: String,
        companionId: String
    ): List<MemoryLayered.MidTerm> {
        return midTermMemoryManager.getMidTermMemories(
            userId = userId,
            companionId = companionId,
            days = config.midTermDays
        )
    }

    /**
     * 获取长期记忆（向量检索）
     *
     * @param query 查询文本
     * @return 长期记忆列表
     */
    private suspend fun getLongTermMemory(
        userId: String,
        companionId: String,
        query: String
    ): List<MemoryLayered.LongTerm> {
        if (query.isBlank()) {
            return emptyList()
        }

        // 使用向量检索
        val searchResults = vectorMemoryManager.search(
            userId = userId,
            companionId = companionId,
            query = query,
            topK = config.searchTopK,
            threshold = config.similarityThreshold
        )

        return searchResults.map { result ->
            MemoryLayered.LongTerm(
                id = result.vector.id,
                userId = userId,
                timestamp = result.vector.timestamp,
                importance = result.vector.importance,
                embedding = result.vector.embedding,
                content = result.vector.content,
                topics = result.vector.topics,
                emotion = result.vector.emotion,
                metadata = result.vector.metadata
            )
        }
    }

    /**
     * 获取永久记忆（核心人格）
     *
     * 从人格配置动态生成而非硬编码，新增角色自动适配；
     * 配置是静态的，结果缓存后只需计算一次
     *
     * @param userId 用户ID
     * @param companionId 伴侣ID
     * @return 永久记忆列表
     */
    private fun getPermanentMemory(
        userId: String,
        companionId: String
    ): List<MemoryLayered.Permanent> {
        val cacheKey = "permanent-$userId-$companionId"

        // 先检查缓存
        permanentMemoryCache[cacheKey]?.let { return it }

        // 从 PersonalityManager 获取人格配置
        val companion = personalityManager.getCompanionConfig(companionId) ?: return emptyList()
        val timestamp = System.currentTimeMillis()

        val memories = mutableListOf<MemoryLayered.Permanent>()

        // 核心特质 - 从 coreTraits 列表提取
        companion.personality.coreTraits.forEachIndexed { index, trait ->
            memories.add(
                MemoryLayered.Permanent(
                    id = "${companionId}_core_${index + 1}",
                    userId = userId,
                    timestamp = timestamp,
                    category = PersonalityCategory.CORE_VALUES,
                    trait = trait
                )
            )
        }

        // 兴趣爱好
        if (companion.personality.interests.isNotEmpty()) {
            memories.add(
                MemoryLayered.Permanent(
                    id = "${companionId}_interest_1",
                    userId = userId,
                    timestamp = timestamp,
                    category = PersonalityCategory.INTERESTS,
                    trait = companion.personality.interests.joinToString("、")
                )
            )
        }

        // 说话风格
        memories.add(
            MemoryLayered.Permanent(
                id = "${companionId}_style_1",
                userId = userId,
                timestamp = timestamp,
                category = PersonalityCategory.SPEAKING_STYLE,
                trait = "${companion.personality.speakingStyle.tone}，${companion.personality.speakingStyle.vocabularyLevel}"
            )
        )

        // 背景故事
        if (companion.personality.background.isNotBlank()) {
            memories.add(
                MemoryLayered.Permanent(
                    id = "${companionId}_background_1",
                    userId = userId,
                    timestamp = timestamp,
                    category = PersonalityCategory.RELATIONSHIP,
                    trait = companion.personality.background
                )
            )
        }

        // 保存到缓存
        permanentMemoryCache[cacheKey] = memories

        return memories
    }

    fun invalidateCharacter(userId: String, companionId: String) {
        permanentMemoryCache.remove("permanent-$userId-$companionId")
    }

    /**
     * 保存新记忆到长期记忆
     *
     * @param userId 用户ID
     * @param companionId 伴侣ID
     * @param message 消息对象
     */
    suspend fun saveToLongTermMemory(
        userId: String,
        companionId: String,
        message: Message
    ) {
        // 只保存重要性高于阈值的消息
        if (message.importance >= (config.longTermThreshold * 100).toInt()) {
            vectorMemoryManager.addMemory(
                userId = userId,
                companionId = companionId,
                content = message.content,
                importance = message.importance / 100f,
                topics = emptyList(),  // TODO: 提取主题
                emotion = message.emotion,
                timestamp = message.timestamp
            )
        }
    }
}
