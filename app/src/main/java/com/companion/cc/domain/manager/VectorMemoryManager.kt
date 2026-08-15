package com.companion.cc.domain.manager

import com.companion.cc.domain.model.MemoryVector
import com.companion.cc.domain.model.SimpleEmbeddingGenerator
import com.companion.cc.domain.model.VectorSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 向量记忆管理器
 *
 * 管理向量化的长期记忆：
 * - 存储：将文本转换为向量并存储
 * - 检索：根据查询向量搜索相似记忆
 * - 删除：清理旧记忆
 *
 * 注意：当前使用内存存储，生产环境应使用持久化存储
 * 可选方案：
 * - ChromaDB Android
 * - Qdrant
 * - SQLite + 自定义向量索引
 */
@Singleton
class VectorMemoryManager @Inject constructor() {
    // 内存存储（临时方案）
    // Key: "userId:companionId"
    private val memoryStore = ConcurrentHashMap<String, MutableList<MemoryVector>>()

    /**
     * 添加记忆
     *
     * @param userId 用户ID
     * @param companionId 伴侣ID
     * @param content 内容
     * @param importance 重要性 (0-1)
     * @param topics 主题标签
     * @param emotion 情感
     * @param timestamp 时间戳
     */
    suspend fun addMemory(
        userId: String,
        companionId: String,
        content: String,
        importance: Float,
        topics: List<String>,
        emotion: String?,
        timestamp: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.Default) {
        val key = getKey(userId, companionId)
        val embedding = SimpleEmbeddingGenerator.generate(content)

        val memory = MemoryVector(
            id = "${timestamp}_${content.hashCode()}",
            userId = userId,
            companionId = companionId,
            embedding = embedding,
            content = content,
            timestamp = timestamp,
            importance = importance,
            topics = topics,
            emotion = emotion
        )

        memoryStore.getOrPut(key) { mutableListOf() }.add(memory)

        // 限制记忆数量（保留最重要的1000条）
        val memories = memoryStore[key]!!
        if (memories.size > 1000) {
            memoryStore[key] = memories
                .sortedByDescending { it.importance }
                .take(1000)
                .toMutableList()
        }
    }

    /**
     * 搜索相似记忆
     *
     * @param userId 用户ID
     * @param companionId 伴侣ID
     * @param query 查询文本
     * @param topK 返回数量
     * @param threshold 相似度阈值
     * @return 搜索结果列表
     */
    suspend fun search(
        userId: String,
        companionId: String,
        query: String,
        topK: Int = 5,
        threshold: Float = 0.5f
    ): List<VectorSearchResult> = withContext(Dispatchers.Default) {
        val key = getKey(userId, companionId)
        val memories = memoryStore[key] ?: return@withContext emptyList()

        if (memories.isEmpty()) {
            return@withContext emptyList()
        }

        // 生成查询向量
        val queryEmbedding = SimpleEmbeddingGenerator.generate(query)

        // 计算所有记忆的相似度
        val results = memories.map { memory ->
            val similarity = memory.similarity(queryEmbedding)
            VectorSearchResult(
                vector = memory,
                similarity = similarity
            )
        }

        // 过滤并排序
        results
            .filter { it.similarity >= threshold }
            .sortedByDescending { it.similarity }
            .take(topK)
    }

    /**
     * 批量搜索
     *
     * @param queries 多个查询文本
     * @return 合并去重的结果
     */
    suspend fun searchBatch(
        userId: String,
        companionId: String,
        queries: List<String>,
        topK: Int = 5,
        threshold: Float = 0.5f
    ): List<VectorSearchResult> = withContext(Dispatchers.Default) {
        val allResults = mutableListOf<VectorSearchResult>()

        queries.forEach { query ->
            val results = search(userId, companionId, query, topK, threshold)
            allResults.addAll(results)
        }

        // 去重并重新排序
        allResults
            .distinctBy { it.vector.id }
            .sortedByDescending { it.similarity }
            .take(topK)
    }

    /**
     * 获取所有记忆
     */
    suspend fun getAllMemories(
        userId: String,
        companionId: String
    ): List<MemoryVector> = withContext(Dispatchers.Default) {
        val key = getKey(userId, companionId)
        memoryStore[key]?.toList() ?: emptyList()
    }

    /**
     * 获取记忆数量
     */
    fun getMemoryCount(userId: String, companionId: String): Int {
        val key = getKey(userId, companionId)
        return memoryStore[key]?.size ?: 0
    }

    /**
     * 清除记忆
     */
    suspend fun clear(userId: String, companionId: String) = withContext(Dispatchers.Default) {
        val key = getKey(userId, companionId)
        memoryStore.remove(key)
    }

    /**
     * 清除所有记忆
     */
    suspend fun clearAll() = withContext(Dispatchers.Default) {
        memoryStore.clear()
    }

    /**
     * 获取存储键
     */
    private fun getKey(userId: String, companionId: String): String {
        return "$userId:$companionId"
    }

    /**
     * 获取内存占用（估算）
     */
    fun getMemoryUsage(): Long {
        var totalSize = 0L
        memoryStore.values.forEach { memories ->
            memories.forEach { memory ->
                // 每个 float 4字节 + 其他字段
                totalSize += memory.embedding.size * 4L + 1000L  // 估算
            }
        }
        return totalSize
    }

    /**
     * 获取统计信息
     */
    fun getStats(): VectorMemoryStats {
        var totalMemories = 0
        var totalUsers = 0
        var avgImportance = 0f

        memoryStore.values.forEach { memories ->
            totalMemories += memories.size
            totalUsers++
            avgImportance += memories.sumOf { it.importance.toDouble() }.toFloat()
        }

        if (totalMemories > 0) {
            avgImportance /= totalMemories
        }

        return VectorMemoryStats(
            totalMemories = totalMemories,
            totalUsers = totalUsers,
            avgImportance = avgImportance,
            memoryUsageBytes = getMemoryUsage()
        )
    }
}

/**
 * 向量记忆统计信息
 */
data class VectorMemoryStats(
    val totalMemories: Int,
    val totalUsers: Int,
    val avgImportance: Float,
    val memoryUsageBytes: Long
)
