package com.companion.cc.domain.model

/**
 * 情感状态
 *
 * 记录 AI 伴侣的情感状态，影响回复语气和行为
 * 参考 Moeru-AI 的情感系统
 */
data class EmotionalState(
    val mood: Mood,                             // 当前心情
    val energy: Float,                          // 精力值 (0-1)
    val affection: Float,                       // 好感度 (0-1)
    val stress: Float,                          // 压力值 (0-1)
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 根据情感状态调整回复语气
     *
     * @param baseResponse 基础回复
     * @return 调整后的回复
     */
    fun adjustTone(baseResponse: String): String {
        return when {
            // 兴奋且精力充沛
            mood == Mood.EXCITED && energy > 0.7f -> {
                "$baseResponse！"
            }

            // 疲惫时语气更平淡
            mood == Mood.TIRED && energy < 0.3f -> {
                baseResponse.replace("！", "。")
            }

            // 压力大时犹豫
            stress > 0.7f -> {
                "嗯... $baseResponse"
            }

            // 好感度高时更亲密
            affection > 0.8f -> {
                when (mood) {
                    Mood.HAPPY -> "亲爱的，$baseResponse"
                    Mood.CONTENT -> "$baseResponse～"
                    else -> baseResponse
                }
            }

            // 难过时语气低沉
            mood == Mood.SAD -> {
                baseResponse.replace("！", "...")
            }

            // 焦虑时不确定
            mood == Mood.ANXIOUS -> {
                "$baseResponse 吧..."
            }

            else -> baseResponse
        }
    }

    /**
     * 计算情感状态的总体分数
     * 用于判断整体情绪是否健康
     */
    fun overallScore(): Float {
        return (
            (if (mood.isPositive()) 0.3f else -0.3f) +
            (energy * 0.2f) +
            (affection * 0.3f) +
            ((1f - stress) * 0.2f)
        ).coerceIn(-1f, 1f)
    }

    companion object {
        /**
         * 默认情感状态（平静、适中）
         */
        fun default(): EmotionalState {
            return EmotionalState(
                mood = Mood.CALM,
                energy = 0.7f,
                affection = 0.5f,
                stress = 0.3f
            )
        }

        /**
         * 开心状态
         */
        fun happy(): EmotionalState {
            return EmotionalState(
                mood = Mood.HAPPY,
                energy = 0.8f,
                affection = 0.7f,
                stress = 0.2f
            )
        }

        /**
         * 疲惫状态
         */
        fun tired(): EmotionalState {
            return EmotionalState(
                mood = Mood.TIRED,
                energy = 0.2f,
                affection = 0.5f,
                stress = 0.5f
            )
        }
    }
}

/**
 * 心情枚举
 */
enum class Mood {
    HAPPY,      // 开心 😊
    SAD,        // 难过 😢
    CALM,       // 平静 😌
    EXCITED,    // 兴奋 🤩
    TIRED,      // 疲惫 😴
    ANXIOUS,    // 焦虑 😰
    CONTENT;    // 满足 😊

    /**
     * 判断是否为积极情绪
     */
    fun isPositive(): Boolean {
        return this in listOf(HAPPY, EXCITED, CALM, CONTENT)
    }

    /**
     * 判断是否为消极情绪
     */
    fun isNegative(): Boolean {
        return this in listOf(SAD, ANXIOUS, TIRED)
    }

    /**
     * 获取 Emoji 表情
     */
    fun emoji(): String {
        return when (this) {
            HAPPY -> "😊"
            SAD -> "😢"
            CALM -> "😌"
            EXCITED -> "🤩"
            TIRED -> "😴"
            ANXIOUS -> "😰"
            CONTENT -> "😊"
        }
    }
}

/**
 * 情感转换规则
 *
 * 定义情绪之间如何转换
 */
data class MoodTransition(
    val from: Mood,
    val to: Mood,
    val probability: Float,         // 转换概率 (0-1)
    val trigger: String             // 触发条件描述
)

/**
 * 预定义的情绪转换规则
 */
object MoodTransitions {
    val rules = listOf(
        // 开心 → 兴奋
        MoodTransition(Mood.HAPPY, Mood.EXCITED, 0.3f, "收到正面反馈"),

        // 开心 → 平静
        MoodTransition(Mood.HAPPY, Mood.CALM, 0.4f, "对话平稳进行"),

        // 难过 → 平静
        MoodTransition(Mood.SAD, Mood.CALM, 0.5f, "获得安慰"),

        // 疲惫 → 平静
        MoodTransition(Mood.TIRED, Mood.CALM, 0.3f, "得到休息"),

        // 焦虑 → 平静
        MoodTransition(Mood.ANXIOUS, Mood.CALM, 0.4f, "问题得到解决"),

        // 兴奋 → 开心
        MoodTransition(Mood.EXCITED, Mood.HAPPY, 0.5f, "兴奋逐渐平复"),

        // 平静 → 满足
        MoodTransition(Mood.CALM, Mood.CONTENT, 0.3f, "一切顺利")
    )

    /**
     * 获取可能的转换
     */
    fun getPossibleTransitions(currentMood: Mood): List<MoodTransition> {
        return rules.filter { it.from == currentMood }
    }
}
