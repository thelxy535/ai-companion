package com.companion.cc.domain.usecase

/**
 * 缪斯冷度值状态机
 * 冷度值范围：0-100
 * - 0-30：正常模式
 * - 31-60：略微冷淡
 * - 61-100：冷淡模式
 */
data class MuseMoodState(
    var coldness: Int = 0,              // 当前冷度值 0-100
    var relationshipLevel: Int = 1,     // 关系等级 1-5
    var lastInteractionTime: Long = 0,  // 上次互动时间戳
    var perfunctoryCount: Int = 0,      // 连续敷衍计数
    var deepTopicCount: Int = 0         // 深度话题累计
) {
    companion object {
        const val NORMAL_THRESHOLD = 30
        const val ANNOYED_THRESHOLD = 60
    }

    fun getMoodState(): String {
        return when {
            coldness >= ANNOYED_THRESHOLD -> "cold"
            coldness >= NORMAL_THRESHOLD -> "annoyed"
            else -> "normal"
        }
    }

    fun shouldEnableVerboseMode(userInput: String): Boolean {
        return relationshipLevel >= 3 && isDeepTopic(userInput)
    }

    fun shouldEnableColdMode(): Boolean {
        return coldness >= ANNOYED_THRESHOLD
    }

    private fun isDeepTopic(text: String): Boolean {
        val deepKeywords = listOf(
            "觉得", "认为", "怎么看", "如何看待", "观点",
            "哲学", "人生", "意义", "价值", "社会",
            "AI", "科技", "未来", "本质"
        )
        return deepKeywords.any { text.contains(it) } && text.length > 10
    }
}

/**
 * 小璨暖度值状态机
 * 暖度值范围：0-100
 * - 0-30：普通关心
 * - 31-70：主动关心
 * - 71-100：深度关心
 */
data class XiaoCanMoodState(
    var warmth: Int = 40,               // 当前暖度值 0-100（降低初始值）
    var relationshipLevel: Int = 2,     // 关系等级 1-5
    var lastInteractionTime: Long = 0,  // 上次互动时间戳
    var negativeCount: Int = 0,         // 连续消极计数
    var positiveEventCount: Int = 0     // 积极事件累计
) {
    fun getEmotionState(): String {
        return when {
            negativeCount >= 3 -> "concerned"
            warmth >= 70 -> "cheerful"
            else -> "normal"
        }
    }

    fun shouldEnableCareMode(): Boolean {
        return negativeCount >= 3  // 改为3次才触发（之前是2次）
    }

    fun getCareLevel(): Int {
        val bonus = (relationshipLevel - 1) * 10
        return minOf(100, warmth + bonus)
    }
}
