package com.companion.cc.domain.model

import kotlin.math.sqrt

/**
 * 向量记忆
 *
 * 使用向量嵌入存储记忆，支持语义相似度搜索
 * 参考 Moeru-AI 的向量记忆系统
 */
data class MemoryVector(
    val id: String,
    val userId: String,
    val companionId: String,
    val embedding: FloatArray,                  // 向量嵌入（默认384维）
    val content: String,                        // 原始内容
    val timestamp: Long,
    val importance: Float,                      // 重要性 (0-1)
    val topics: List<String>,                   // 主题标签
    val emotion: String?,                       // 情感
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * 计算与另一个向量的余弦相似度
     *
     * @param other 另一个向量
     * @return 相似度 (0-1)，1表示完全相同，0表示完全不同
     */
    fun cosineSimilarity(other: MemoryVector): Float {
        require(embedding.size == other.embedding.size) {
            "向量维度不匹配: ${embedding.size} vs ${other.embedding.size}"
        }

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in embedding.indices) {
            dotProduct += embedding[i] * other.embedding[i]
            normA += embedding[i] * embedding[i]
            normB += other.embedding[i] * other.embedding[i]
        }

        return if (normA == 0f || normB == 0f) {
            0f
        } else {
            dotProduct / (sqrt(normA) * sqrt(normB))
        }
    }

    /**
     * 计算与查询向量的相似度
     */
    fun similarity(queryEmbedding: FloatArray): Float {
        require(embedding.size == queryEmbedding.size) {
            "向量维度不匹配"
        }

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in embedding.indices) {
            dotProduct += embedding[i] * queryEmbedding[i]
            normA += embedding[i] * embedding[i]
            normB += queryEmbedding[i] * queryEmbedding[i]
        }

        return if (normA == 0f || normB == 0f) {
            0f
        } else {
            dotProduct / (sqrt(normA) * sqrt(normB))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemoryVector

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
 * 向量搜索结果
 */
data class VectorSearchResult(
    val vector: MemoryVector,
    val similarity: Float           // 相似度分数 (0-1)
)

/**
 * 简单的向量嵌入生成器（用于测试/简单场景）
 *
 * 注意：生产环境应使用真实的嵌入模型，如：
 * - sentence-transformers (paraphrase-multilingual-MiniLM-L12-v2)
 * - OpenAI embeddings API
 * - 本地部署的模型
 */
object SimpleEmbeddingGenerator {
    /**
     * 生成简单的词袋模型嵌入（仅用于测试）
     *
     * 实际使用时应替换为真实的嵌入模型
     */
    fun generate(text: String, dimension: Int = 384): FloatArray {
        // 简单的哈希向量化（仅用于演示）
        val embedding = FloatArray(dimension)

        // 使用文本的字符和词汇生成向量
        text.split("\\s+".toRegex()).forEachIndexed { index, word ->
            val hash = word.hashCode()
            val pos = (hash % dimension + dimension) % dimension
            embedding[pos] += 1f / (index + 1)
        }

        // 归一化
        val norm = sqrt(embedding.sumOf { (it * it).toDouble() }).toFloat()
        if (norm > 0f) {
            for (i in embedding.indices) {
                embedding[i] /= norm
            }
        }

        return embedding
    }

    /**
     * 批量生成嵌入
     */
    fun generateBatch(texts: List<String>, dimension: Int = 384): List<FloatArray> {
        return texts.map { generate(it, dimension) }
    }
}
