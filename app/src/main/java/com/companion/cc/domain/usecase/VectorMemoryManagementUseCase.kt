package com.companion.cc.domain.usecase

import com.companion.cc.data.local.dao.VectorMemoryDao
import com.companion.cc.data.local.entity.VectorMemoryEntity
import com.companion.cc.domain.model.MemorySearchResult
import com.companion.cc.domain.model.MemoryType
import com.companion.cc.domain.model.VectorMemory
import com.companion.cc.domain.service.SimpleEmbeddingService
import com.google.gson.Gson
import java.util.UUID
import javax.inject.Inject

/**
 * 向量记忆管理UseCase
 * 负责存储和检索向量化的记忆
 */
class VectorMemoryManagementUseCase @Inject constructor(
    private val vectorMemoryDao: VectorMemoryDao,
    private val embeddingService: SimpleEmbeddingService,
    private val extractMemoryUseCase: ExtractMemoryUseCase
) {
    private val gson = Gson()

    /**
     * 从对话中自动提取并保存记忆
     */
    suspend fun extractAndSaveMemories(
        userId: String,
        companionId: String,
        conversation: String
    ) {
        // 1. 提取记忆
        val extractedMemories = extractMemoryUseCase(conversation)

        if (extractedMemories.isEmpty()) {
            return
        }

        // 2. 向量化并保存
        val vectorMemories = extractedMemories.map { extracted ->
            val embedding = embeddingService.encode(extracted.content)

            VectorMemoryEntity(
                id = UUID.randomUUID().toString(),
                content = extracted.content,
                embedding = gson.toJson(embedding.toList()),
                type = extracted.type.name,
                timestamp = System.currentTimeMillis(),
                userId = userId,
                companionId = companionId,
                metadata = gson.toJson(emptyMap<String, String>())
            )
        }

        vectorMemoryDao.insertAll(vectorMemories)
    }

    /**
     * 根据查询文本检索相关记忆
     * @param query 查询文本
     * @param topK 返回前K个最相关的记忆
     * @return 相关记忆列表，按相似度排序
     */
    suspend fun searchMemories(
        userId: String,
        companionId: String,
        query: String,
        topK: Int = 5
    ): List<MemorySearchResult> {
        // 1. 获取所有记忆
        val allMemories = vectorMemoryDao.getAllMemories(userId, companionId)

        if (allMemories.isEmpty()) {
            return emptyList()
        }

        // 2. 向量化查询
        val queryEmbedding = embeddingService.encode(query)

        // 3. 计算相似度
        val results = allMemories.map { entity ->
            val memoryEmbedding = gson.fromJson(entity.embedding, FloatArray::class.java)
            val similarity = embeddingService.cosineSimilarity(queryEmbedding, memoryEmbedding)

            MemorySearchResult(
                memory = VectorMemory(
                    id = entity.id,
                    content = entity.content,
                    embedding = memoryEmbedding,
                    type = MemoryType.valueOf(entity.type),
                    timestamp = entity.timestamp,
                    userId = entity.userId,
                    companionId = entity.companionId,
                    metadata = gson.fromJson(entity.metadata, Map::class.java) as Map<String, String>
                ),
                similarity = similarity
            )
        }

        // 4. 排序并返回topK
        return results
            .sortedByDescending { it.similarity }
            .take(topK)
            .filter { it.similarity > 0.3f }  // 过滤掉相似度太低的
    }

    /**
     * 手动保存记忆
     */
    suspend fun saveMemory(
        userId: String,
        companionId: String,
        content: String,
        type: MemoryType,
        metadata: Map<String, String> = emptyMap()
    ) {
        val embedding = embeddingService.encode(content)

        val entity = VectorMemoryEntity(
            id = UUID.randomUUID().toString(),
            content = content,
            embedding = gson.toJson(embedding.toList()),
            type = type.name,
            timestamp = System.currentTimeMillis(),
            userId = userId,
            companionId = companionId,
            metadata = gson.toJson(metadata)
        )

        vectorMemoryDao.insert(entity)
    }

    /**
     * 获取记忆总数
     */
    suspend fun getMemoryCount(userId: String, companionId: String): Int {
        return vectorMemoryDao.getCount(userId, companionId)
    }

    /**
     * 删除所有记忆
     */
    suspend fun clearMemories(userId: String, companionId: String) {
        vectorMemoryDao.deleteAll(userId, companionId)
    }

    /**
     * 获取最近的记忆（不做向量检索）
     */
    suspend fun getRecentMemories(
        userId: String,
        companionId: String,
        limit: Int = 10
    ): List<VectorMemory> {
        return vectorMemoryDao.getRecentMemories(userId, companionId, limit).map { entity ->
            val memoryEmbedding = gson.fromJson(entity.embedding, FloatArray::class.java)
            VectorMemory(
                id = entity.id,
                content = entity.content,
                embedding = memoryEmbedding,
                type = MemoryType.valueOf(entity.type),
                timestamp = entity.timestamp,
                userId = entity.userId,
                companionId = entity.companionId,
                metadata = gson.fromJson(entity.metadata, Map::class.java) as Map<String, String>
            )
        }
    }
}
