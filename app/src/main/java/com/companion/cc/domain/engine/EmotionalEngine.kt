package com.companion.cc.domain.engine

import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Mood
import com.companion.cc.domain.model.MoodTransitions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * 情感引擎
 *
 * 管理 AI 伴侣的情感状态：
 * - 情绪识别：分析用户消息的情感倾向
 * - 情绪转换：根据对话内容动态调整情绪
 * - 状态更新：更新精力、好感度、压力值
 * - 回复调整：根据情感状态调整回复语气
 *
 * 参考 Moeru-AI 的情感系统
 */
@Singleton
class EmotionalEngine @Inject constructor() {
    // 当前情感状态（每个伴侣独立）
    // Key: "userId:companionId"
    private val emotionalStates = mutableMapOf<String, EmotionalState>()

    // 当前状态的 StateFlow
    private val _currentState = MutableStateFlow(EmotionalState.default())
    val currentState: StateFlow<EmotionalState> = _currentState.asStateFlow()

    private var currentKey: String = ""

    /**
     * 设置当前会话
     */
    fun setCurrentSession(userId: String, companionId: String) {
        currentKey = getKey(userId, companionId)
        _currentState.value = getEmotionalState(userId, companionId)
    }

    /**
     * 检测情感（简化版）
     */
    fun detectEmotion(message: String) {
        if (currentKey.isEmpty()) return

        val sentiment = analyzeSentiment(message)
        val currentState = _currentState.value

        val newMood = calculateNewMood(currentState.mood, sentiment)
        val newAffection = adjustAffection(currentState.affection, sentiment)
        val newEnergy = adjustEnergy(currentState.energy)
        val newStress = adjustStress(currentState.stress, sentiment)

        val newState = EmotionalState(
            mood = newMood,
            energy = newEnergy,
            affection = newAffection,
            stress = newStress
        )

        emotionalStates[currentKey] = newState
        _currentState.value = newState
    }

    /**
     * 获取当前情感状态
     *
     * @param userId 用户ID
     * @param companionId 伴侣ID
     * @return 当前情感状态
     */
    fun getEmotionalState(userId: String, companionId: String): EmotionalState {
        val key = getKey(userId, companionId)
        return emotionalStates.getOrPut(key) { EmotionalState.default() }
    }

    /**
     * 处理用户消息，更新情感状态
     *
     * @param userId 用户ID
     * @param companionId 伴侣ID
     * @param userMessage 用户消息
     * @return 更新后的情感状态
     */
    fun processMessage(
        userId: String,
        companionId: String,
        userMessage: String
    ): EmotionalState {
        val currentState = getEmotionalState(userId, companionId)

        // 1. 分析消息情感
        val sentiment = analyzeSentiment(userMessage)

        // 2. 计算新的心情
        val newMood = calculateNewMood(currentState.mood, sentiment)

        // 3. 更新好感度
        val newAffection = adjustAffection(currentState.affection, sentiment)

        // 4. 更新精力（随时间自然恢复）
        val newEnergy = adjustEnergy(currentState.energy)

        // 5. 更新压力（根据对话强度）
        val newStress = adjustStress(currentState.stress, sentiment)

        // 6. 创建新状态
        val newState = EmotionalState(
            mood = newMood,
            energy = newEnergy,
            affection = newAffection,
            stress = newStress
        )

        // 7. 保存状态
        val key = getKey(userId, companionId)
        emotionalStates[key] = newState

        return newState
    }

    /**
     * 分析消息情感倾向
     *
     * @param message 消息文本
     * @return 情感分数 (-1到1，负数=消极，正数=积极)
     */
    private fun analyzeSentiment(message: String): Float {
        var score = 0f

        // 积极词汇
        val positiveWords = listOf(
            "开心", "高兴", "快乐", "喜欢", "爱", "好", "棒", "赞", "谢谢",
            "感谢", "优秀", "完美", "美好", "幸福", "满意", "哈哈", "😊", "😄", "❤️"
        )

        // 消极词汇
        val negativeWords = listOf(
            "难过", "伤心", "生气", "讨厌", "烦", "累", "疲惫", "压力", "焦虑",
            "害怕", "担心", "失望", "痛苦", "糟糕", "不好", "😢", "😔", "😰"
        )

        // 统计积极词汇
        positiveWords.forEach { word ->
            if (message.contains(word)) {
                score += 0.1f
            }
        }

        // 统计消极词汇
        negativeWords.forEach { word ->
            if (message.contains(word)) {
                score -= 0.1f
            }
        }

        // 问号（好奇/疑问）
        if (message.contains("？") || message.contains("?")) {
            score += 0.05f
        }

        // 感叹号（强烈情感）
        val exclamationCount = message.count { it == '！' || it == '!' }
        score += exclamationCount * 0.05f

        // 限制范围
        return score.coerceIn(-1f, 1f)
    }

    /**
     * 计算新的心情
     *
     * @param currentMood 当前心情
     * @param sentiment 情感分数
     * @return 新心情
     */
    private fun calculateNewMood(currentMood: Mood, sentiment: Float): Mood {
        // 根据情感分数决定转换
        return when {
            // 非常积极 → 兴奋
            sentiment > 0.5f -> {
                if (currentMood.isPositive()) Mood.EXCITED else Mood.HAPPY
            }

            // 积极 → 开心/平静
            sentiment > 0.2f -> {
                when (currentMood) {
                    Mood.SAD, Mood.ANXIOUS -> Mood.CALM
                    Mood.TIRED -> Mood.CALM
                    else -> Mood.HAPPY
                }
            }

            // 消极 → 难过/焦虑
            sentiment < -0.3f -> {
                if (sentiment < -0.6f) Mood.SAD else Mood.ANXIOUS
            }

            // 中性 → 平静/满足
            else -> {
                when (currentMood) {
                    Mood.EXCITED -> Mood.HAPPY
                    Mood.HAPPY -> if (Random.nextFloat() < 0.3f) Mood.CONTENT else Mood.HAPPY
                    Mood.TIRED -> Mood.CALM
                    else -> currentMood
                }
            }
        }
    }

    /**
     * 调整好感度
     *
     * @param current 当前好感度
     * @param sentiment 情感分数
     * @return 新好感度
     */
    private fun adjustAffection(current: Float, sentiment: Float): Float {
        // 积极互动增加好感度，消极互动减少好感度
        val delta = sentiment * 0.05f

        // 好感度变化有上限（越高越难增加）
        val adjusted = when {
            current > 0.8f -> delta * 0.5f  // 高好感度时增长慢
            current < 0.3f -> delta * 1.5f  // 低好感度时变化快
            else -> delta
        }

        return (current + adjusted).coerceIn(0f, 1f)
    }

    /**
     * 调整精力值
     *
     * @param current 当前精力
     * @return 新精力值
     */
    private fun adjustEnergy(current: Float): Float {
        // 每次对话消耗一点精力
        val consumed = 0.02f

        // 自然恢复（如果精力很低）
        val recovered = if (current < 0.3f) 0.05f else 0.01f

        return (current - consumed + recovered).coerceIn(0f, 1f)
    }

    /**
     * 调整压力值
     *
     * @param current 当前压力
     * @param sentiment 情感分数
     * @return 新压力值
     */
    private fun adjustStress(current: Float, sentiment: Float): Float {
        // 消极情感增加压力
        val delta = when {
            sentiment < -0.3f -> 0.1f   // 负面消息增加压力
            sentiment > 0.3f -> -0.05f  // 正面消息减少压力
            else -> -0.02f              // 中性消息略微减少压力
        }

        return (current + delta).coerceIn(0f, 1f)
    }

    /**
     * 根据情感状态调整回复
     *
     * @param baseResponse 基础回复
     * @param userId 用户ID
     * @param companionId 伴侣ID
     * @return 调整后的回复
     */
    fun adjustResponse(
        baseResponse: String,
        userId: String,
        companionId: String
    ): String {
        val state = getEmotionalState(userId, companionId)
        return state.adjustTone(baseResponse)
    }

    /**
     * 重置情感状态
     */
    fun resetEmotionalState(userId: String, companionId: String) {
        val key = getKey(userId, companionId)
        emotionalStates[key] = EmotionalState.default()
    }

    /**
     * 手动设置情感状态（用于测试或特殊场景）
     */
    fun setEmotionalState(
        userId: String,
        companionId: String,
        state: EmotionalState
    ) {
        val key = getKey(userId, companionId)
        emotionalStates[key] = state
    }

    /**
     * 获取所有情感状态（用于调试）
     */
    fun getAllEmotionalStates(): Map<String, EmotionalState> {
        return emotionalStates.toMap()
    }

    /**
     * 获取存储键
     */
    private fun getKey(userId: String, companionId: String): String {
        return "$userId:$companionId"
    }
}
