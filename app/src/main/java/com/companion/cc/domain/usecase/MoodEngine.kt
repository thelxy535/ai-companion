package com.companion.cc.domain.usecase

import com.companion.cc.domain.model.Message
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户行为类型
 */
enum class BehaviorType {
    PERFUNCTORY,      // 敷衍
    COMMANDING,       // 命令式
    APOLOGY,          // 道歉
    DEEP_TOPIC,       // 深度话题
    SELF_DEPRECATING, // 自我贬低
    NEGATIVE_EMOTION, // 负面情绪
    LONG_ABSENCE,     // 长期未出现
    NORMAL            // 正常对话
}

data class UserBehavior(
    val type: BehaviorType
)

/**
 * 缪斯冷度值引擎
 */
@Singleton
class MuseColdnessEngine @Inject constructor() {

    fun detectBehavior(userInput: String, history: List<Message>): UserBehavior {
        val trimmed = userInput.trim()

        if (isPerfunctory(trimmed)) {
            return UserBehavior(BehaviorType.PERFUNCTORY)
        }
        if (isCommanding(trimmed)) {
            return UserBehavior(BehaviorType.COMMANDING)
        }
        if (isApology(trimmed)) {
            return UserBehavior(BehaviorType.APOLOGY)
        }
        if (isDeepTopic(trimmed)) {
            return UserBehavior(BehaviorType.DEEP_TOPIC)
        }

        return UserBehavior(BehaviorType.NORMAL)
    }

    fun updateColdness(
        state: MuseMoodState,
        userInput: String,
        userBehavior: UserBehavior
    ): MuseMoodState {
        val now = System.currentTimeMillis()

        when (userBehavior.type) {
            BehaviorType.PERFUNCTORY -> {
                state.perfunctoryCount++
                if (state.perfunctoryCount >= 3) {
                    state.coldness = minOf(100, state.coldness + 20)
                } else {
                    state.coldness = minOf(100, state.coldness + 5)
                }
            }
            BehaviorType.COMMANDING -> {
                state.coldness = minOf(100, state.coldness + 15)
            }
            BehaviorType.APOLOGY -> {
                state.coldness = maxOf(0, state.coldness - 30)
                state.perfunctoryCount = 0
            }
            BehaviorType.DEEP_TOPIC -> {
                state.coldness = maxOf(0, state.coldness - 5)
                state.deepTopicCount++
                state.perfunctoryCount = 0
                if (state.deepTopicCount >= 10 && state.relationshipLevel < 5) {
                    state.relationshipLevel++
                    state.deepTopicCount = 0
                }
            }
            BehaviorType.NORMAL -> {
                state.coldness = maxOf(0, state.coldness - 2)
            }
            else -> {}
        }

        state.lastInteractionTime = now
        return state
    }

    private fun isPerfunctory(text: String): Boolean {
        val perfunctoryWords = setOf("哦", "嗯", "好", "行", "ok", "OK", "哈哈", "呵呵")
        return text in perfunctoryWords && text.length <= 3
    }

    private fun isCommanding(text: String): Boolean {
        val commandingWords = listOf("给我", "帮我", "你必须", "快点", "赶紧")
        return commandingWords.any { text.contains(it) }
    }

    private fun isApology(text: String): Boolean {
        val apologyWords = listOf("对不起", "抱歉", "不好意思", "我错了", "sorry")
        return apologyWords.any { text.contains(it) }
    }

    private fun isDeepTopic(text: String): Boolean {
        val deepKeywords = listOf(
            "觉得", "认为", "怎么看", "如何看待", "观点",
            "哲学", "人生", "意义", "价值", "社会"
        )
        return deepKeywords.any { text.contains(it) } && text.length > 10
    }
}

/**
 * 小璨暖度值引擎
 */
@Singleton
class XiaoCanWarmthEngine @Inject constructor() {

    fun detectBehavior(userInput: String, history: List<Message>): UserBehavior {
        val trimmed = userInput.trim()

        if (isSelfDeprecating(trimmed)) {
            return UserBehavior(BehaviorType.SELF_DEPRECATING)
        }
        if (isNegativeEmotion(trimmed)) {
            return UserBehavior(BehaviorType.NEGATIVE_EMOTION)
        }
        if (isApology(trimmed)) {
            return UserBehavior(BehaviorType.APOLOGY)
        }

        return UserBehavior(BehaviorType.NORMAL)
    }

    fun updateWarmth(
        state: XiaoCanMoodState,
        userInput: String,
        userBehavior: UserBehavior
    ): XiaoCanMoodState {
        val now = System.currentTimeMillis()

        when (userBehavior.type) {
            BehaviorType.SELF_DEPRECATING -> {
                state.warmth = minOf(100, state.warmth + 10)
                state.negativeCount++
            }
            BehaviorType.NEGATIVE_EMOTION -> {
                state.warmth = minOf(100, state.warmth + 8)
                state.negativeCount++
            }
            BehaviorType.APOLOGY -> {
                state.warmth = minOf(100, state.warmth + 5)
                state.negativeCount = 0
            }
            BehaviorType.NORMAL -> {
                state.warmth = minOf(100, state.warmth + 2)
                if (state.negativeCount > 0) {
                    state.negativeCount = maxOf(0, state.negativeCount - 1)
                }
            }
            else -> {}
        }

        state.warmth = maxOf(30, state.warmth)
        state.lastInteractionTime = now

        return state
    }

    private fun isSelfDeprecating(text: String): Boolean {
        val negativeSelfWords = listOf("我好笨", "我很差", "我没用", "我不行", "我失败")
        return negativeSelfWords.any { text.contains(it) }
    }

    private fun isNegativeEmotion(text: String): Boolean {
        val negativeWords = listOf("难过", "痛苦", "绝望", "累了", "难受")
        return negativeWords.any { text.contains(it) }
    }

    private fun isApology(text: String): Boolean {
        val apologyWords = listOf("对不起", "抱歉", "不好意思", "我错了")
        return apologyWords.any { text.contains(it) }
    }
}
