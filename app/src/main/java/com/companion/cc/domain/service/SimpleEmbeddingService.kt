package com.companion.cc.domain.service

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * 简单的向量化服务
 * 使用TF-IDF算法将文本转换为向量
 *
 * 注意：这是简化实现，生产环境建议使用专业的embedding模型
 */
@Singleton
class SimpleEmbeddingService @Inject constructor() {

    // 词汇表（在实际使用中会动态构建）
    private val vocabulary = mutableSetOf<String>()
    private val idfScores = mutableMapOf<String, Double>()

    /**
     * 将文本转换为向量
     * @param text 输入文本
     * @param vectorSize 向量维度（默认128）
     * @return 向量表示
     */
    fun encode(text: String, vectorSize: Int = 128): FloatArray {
        // 1. 分词（简单空格分割）
        val words = tokenize(text)

        // 2. 更新词汇表
        vocabulary.addAll(words)

        // 3. 计算TF（词频）
        val tf = calculateTF(words)

        // 4. 使用hash技巧将词映射到固定维度
        val vector = FloatArray(vectorSize) { 0f }

        words.forEach { word ->
            val hash = word.hashCode()
            val index = (hash % vectorSize + vectorSize) % vectorSize
            vector[index] += tf[word]?.toFloat() ?: 0f
        }

        // 5. 归一化
        return normalize(vector)
    }

    /**
     * 计算两个向量的余弦相似度
     * @return 相似度分数 0-1
     */
    fun cosineSimilarity(vector1: FloatArray, vector2: FloatArray): Float {
        require(vector1.size == vector2.size) { "向量维度必须相同" }

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in vector1.indices) {
            dotProduct += vector1[i] * vector2[i]
            norm1 += vector1[i] * vector1[i]
            norm2 += vector2[i] * vector2[i]
        }

        if (norm1 == 0f || norm2 == 0f) {
            return 0f
        }

        return dotProduct / (sqrt(norm1) * sqrt(norm2))
    }

    /**
     * 分词
     */
    private fun tokenize(text: String): List<String> {
        // 简单实现：按空格、标点分割，转小写
        return text
            .replace(Regex("[，。！？、；：（）【】《》\\s]+"), " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .map { it.lowercase() }
    }

    /**
     * 计算词频（TF）
     */
    private fun calculateTF(words: List<String>): Map<String, Double> {
        val wordCount = words.size
        if (wordCount == 0) return emptyMap()

        val frequency = mutableMapOf<String, Int>()
        words.forEach { word ->
            frequency[word] = frequency.getOrDefault(word, 0) + 1
        }

        return frequency.mapValues { (_, count) ->
            count.toDouble() / wordCount
        }
    }

    /**
     * 向量归一化
     */
    private fun normalize(vector: FloatArray): FloatArray {
        val norm = sqrt(vector.sumOf { (it * it).toDouble() }).toFloat()
        if (norm == 0f) return vector

        return FloatArray(vector.size) { i ->
            vector[i] / norm
        }
    }

    /**
     * 批量编码
     */
    fun encodeBatch(texts: List<String>, vectorSize: Int = 128): List<FloatArray> {
        return texts.map { encode(it, vectorSize) }
    }
}
