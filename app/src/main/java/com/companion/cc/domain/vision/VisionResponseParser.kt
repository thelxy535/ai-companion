package com.companion.cc.domain.vision

import com.companion.cc.domain.model.VisionAnalysis

/**
 * 解析视觉模型约定的六行结构化文本。
 *
 * Gemini、OpenAI 兼容服务和自建网关共用同一解析规则，避免服务切换后
 * 图片分析结果的字段行为不一致。
 */
object VisionResponseParser {
    private const val EMPTY_VALUE = "无"

    fun parse(rawResponse: String, confidence: Float): VisionAnalysis {
        val lines = rawResponse.lines().map { it.trim() }

        fun extractValue(key: String): String? {
            val keyPattern = Regex("^${Regex.escape(key)}\\s*[:：]\\s*(.*)$")
            return lines.firstNotNullOfOrNull { line ->
                keyPattern.matchEntire(line)
                    ?.groupValues
                    ?.get(1)
                    ?.trim()
                    ?.takeIf { it.isNotBlank() && it != EMPTY_VALUE }
            }
        }

        fun extractList(key: String): List<String> = extractValue(key)
            ?.split("、", ",", "，")
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?: emptyList()

        return VisionAnalysis(
            mainSubjects = extractList("主要对象"),
            environment = extractValue("环境"),
            actions = extractList("动作"),
            textContent = extractValue("文字"),
            mood = extractValue("氛围"),
            contextualMeaning = extractValue("语境理解"),
            confidence = confidence.coerceIn(0f, 1f),
            rawResponse = rawResponse
        )
    }
}
