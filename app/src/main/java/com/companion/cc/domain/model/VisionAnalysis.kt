package com.companion.cc.domain.model

/**
 * 视觉理解结果（结构化）
 *
 * 设计目标：
 * - 只提取与对话有价值的信息
 * - 避免冗长的完整图片描述
 * - 方便主对话模型理解和引用
 */
data class VisionAnalysis(
    val mainSubjects: List<String> = emptyList(),  // 主要对象（人物、物品、动物等）
    val environment: String? = null,                // 环境/场景（室内、户外、地点等）
    val actions: List<String> = emptyList(),        // 正在发生的动作
    val textContent: String? = null,                // 图片中的文字（OCR）
    val mood: String? = null,                       // 图片整体氛围（欢乐、严肃、温馨等）
    val contextualMeaning: String? = null,          // 结合用户文字的语境理解
    val confidence: Float = 0f,                     // 理解置信度 (0-1)
    val rawResponse: String? = null                 // 原始 API 响应（调试用）
) {
    /**
     * 转换为自然语言描述（供主对话模型使用）
     */
    fun toNaturalLanguage(): String {
        val parts = mutableListOf<String>()

        // 优先呈现语境理解（最有价值）
        contextualMeaning?.let { parts.add("理解：$it") }

        // 主要内容
        if (mainSubjects.isNotEmpty()) {
            parts.add("看到：${mainSubjects.joinToString("、")}")
        }

        // 动作
        if (actions.isNotEmpty()) {
            parts.add("动作：${actions.joinToString("、")}")
        }

        // 环境
        environment?.let { parts.add("场景：$it") }

        // 文字内容
        textContent?.let { parts.add("文字：$it") }

        // 氛围
        mood?.let { parts.add("氛围：$it") }

        return if (parts.isEmpty()) {
            "（图片理解结果为空）"
        } else {
            parts.joinToString(" | ")
        }
    }
}

/**
 * 视觉分析错误
 */
sealed class VisionError : Exception() {
    data class NetworkError(override val message: String) : VisionError()
    data class ApiError(override val message: String, val code: String? = null) : VisionError()
    data class ImageTooLarge(val size: Long, val maxSize: Long) : VisionError()
    data class UnsupportedFormat(val format: String) : VisionError()
    data class Timeout(override val message: String) : VisionError()
    data class Unknown(override val message: String, override val cause: Throwable? = null) : VisionError()
}
