package com.companion.cc.domain.model

/**
 * 记忆层级
 *
 * 定义了记忆系统的4个层级，从短期到永久
 */
enum class MemoryLayer {
    /**
     * 短期记忆（Short-term Memory）
     * - 容量：100条消息
     * - 时长：当前会话
     * - 用途：维持对话连贯性
     */
    SHORT_TERM,

    /**
     * 中期记忆（Mid-term Memory）
     * - 容量：最近N天的摘要
     * - 时长：7天（默认）
     * - 用途：回忆最近的事件
     */
    MID_TERM,

    /**
     * 长期记忆（Long-term Memory）
     * - 容量：无限制（向量数据库）
     * - 时长：永久
     * - 用途：语义搜索、知识积累
     */
    LONG_TERM,

    /**
     * 永久记忆（Permanent Memory）
     * - 容量：固定（人格特质）
     * - 时长：永久
     * - 用途：核心人格、不可遗忘的特质
     */
    PERMANENT
}

/**
 * 记忆层配置
 *
 * 控制各层记忆的行为参数
 */
data class MemoryLayerConfig(
    // 短期记忆配置
    val shortTermCapacity: Int = 100,          // 短期记忆容量

    // 中期记忆配置
    val midTermDays: Int = 7,                  // 中期记忆天数
    val summaryFrequencyHours: Int = 24,       // 摘要生成频率（小时）
    val summaryMinMessages: Int = 10,          // 最少消息数才生成摘要

    // 长期记忆配置
    val longTermThreshold: Float = 0.7f,       // 长期记忆重要性阈值
    val vectorDimension: Int = 384,            // 向量维度（默认384，适合sentence-transformers）
    val searchTopK: Int = 5,                   // 搜索返回数量
    val similarityThreshold: Float = 0.5f,     // 相似度阈值

    // 永久记忆配置
    val permanentMemoryEnabled: Boolean = true // 是否启用永久记忆
)

/**
 * 记忆查询结果
 *
 * 包含记忆内容和相似度分数
 */
data class MemoryLayerSearchResult(
    val memory: MemoryLayered,
    val score: Float,               // 相似度分数 (0-1)
    val layer: MemoryLayer          // 来自哪个层级
)

/**
 * 完整记忆上下文
 *
 * 整合所有层级的记忆，提供给 LLM 使用
 */
data class CompleteMemoryContext(
    val shortTerm: List<MemoryLayered.ShortTerm>,      // 短期记忆
    val midTerm: List<MemoryLayered.MidTerm>,          // 中期记忆
    val longTerm: List<MemoryLayered.LongTerm>,        // 长期记忆
    val permanent: List<MemoryLayered.Permanent>       // 永久记忆
) {
    /**
     * 格式化为 Prompt 文本
     */
    fun toPromptText(): String {
        val parts = mutableListOf<String>()

        // 永久记忆（核心人格）
        if (permanent.isNotEmpty()) {
            parts.add("# 核心人格")
            permanent.groupBy { it.category }.forEach { (category, traits) ->
                parts.add("## ${category.displayName()}")
                traits.forEach { trait ->
                    parts.add("- ${trait.trait}")
                }
            }
            parts.add("")
        }

        // 长期记忆（重要知识）
        if (longTerm.isNotEmpty()) {
            parts.add("# 重要记忆")
            longTerm.sortedByDescending { it.importance }.take(5).forEach { memory ->
                parts.add("- ${memory.content}")
            }
            parts.add("")
        }

        // 中期记忆（最近事件）
        if (midTerm.isNotEmpty()) {
            parts.add("# 最近事件")
            midTerm.sortedByDescending { it.timestamp }.forEach { memory ->
                parts.add("- ${memory.summary}")
            }
            parts.add("")
        }

        // 短期记忆（当前对话）
        if (shortTerm.isNotEmpty()) {
            parts.add("# 当前对话")
            shortTerm.takeLast(10).forEach { memory ->
                val role = if (memory.message.role == MessageRole.USER) "用户" else "AI"
                parts.add("$role: ${memory.message.content}")
            }
        }

        return parts.joinToString("\n")
    }
}

/**
 * 人格类别显示名称
 */
private fun PersonalityCategory.displayName(): String {
    return when (this) {
        PersonalityCategory.CORE_VALUES -> "核心价值观"
        PersonalityCategory.INTERESTS -> "兴趣爱好"
        PersonalityCategory.SPEAKING_STYLE -> "说话风格"
        PersonalityCategory.BEHAVIOR_PATTERN -> "行为模式"
        PersonalityCategory.RELATIONSHIP -> "关系定位"
        PersonalityCategory.BACKGROUND -> "背景故事"
    }
}
