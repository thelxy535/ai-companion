package com.companion.cc.domain.usecase

import javax.inject.Inject
import kotlin.math.min

/**
 * 规则情绪识别器（零成本）
 * 完全照搬原Web版 utils/emotionDetector.js
 */
class DetectEmotionUseCase @Inject constructor() {

    // 情绪模式配置（完全来自 config/optimization.js EMOTION_PATTERNS）
    private val emotionPatterns = mapOf(
        "happy" to EmotionPattern(
            keywords = listOf("开心", "高兴", "哈哈", "嘿嘿", "棒", "不错", "太好了", "真棒", "喜欢", "爱", "幸福", "快乐"),
            emojis = listOf("😊", "😄", "😃", "🎉", "👍", "💕"),
            score = 0.8f
        ),
        "sad" to EmotionPattern(
            keywords = listOf("难过", "伤心", "哭", "难受", "痛苦", "失落", "沮丧", "郁闷", "不开心", "心情不好", "难熬", "煎熬", "崩溃"),
            emojis = listOf("😢", "😭", "😔", "😞", "💔"),
            score = 0.8f
        ),
        "lonely" to EmotionPattern(
            keywords = listOf("孤独", "寂寞", "孤单", "没人陪", "一个人", "好孤单", "没人理", "冷清"),
            emojis = listOf("😔", "🥺"),
            score = 0.9f
        ),
        "hurt" to EmotionPattern(
            keywords = listOf("委屈", "受伤", "难受", "不公平", "没人理解", "被误解", "心里难受"),
            emojis = listOf("😢", "🥺", "💔"),
            score = 0.9f
        ),
        "angry" to EmotionPattern(
            keywords = listOf("生气", "愤怒", "烦", "气死了", "讨厌", "不爽", "火大", "怒"),
            emojis = listOf("😠", "😡", "💢"),
            score = 0.8f
        ),
        "tired" to EmotionPattern(
            keywords = listOf("累", "疲惫", "困", "好累", "精疲力尽", "筋疲力尽", "累死了", "撑不住"),
            emojis = listOf("😫", "😩", "😴"),
            score = 0.8f
        ),
        "excited" to EmotionPattern(
            keywords = listOf("激动", "兴奋", "哇", "太棒了", "amazing", "awesome", "太好了", "不敢相信"),
            emojis = listOf("🤩", "😍", "🎉", "✨"),
            score = 0.8f
        ),
        "confused" to EmotionPattern(
            keywords = listOf("困惑", "不懂", "搞不清", "不明白", "为什么", "怎么回事", "奇怪", "疑惑"),
            emojis = listOf("😕", "🤔", "❓"),
            score = 0.7f
        )
    )

    /**
     * 检测消息情绪
     * 完全照搬原版算法
     */
    operator fun invoke(message: String): EmotionDetection {
        val scores = mutableMapOf<String, Float>()

        // 1. 关键词匹配（完全照搬）
        for ((emotion, pattern) in emotionPatterns) {
            var score = 0f

            // 检查关键词
            for (keyword in pattern.keywords) {
                if (message.contains(keyword)) {
                    score += pattern.score
                }
            }

            // 检查 emoji
            for (emoji in pattern.emojis) {
                if (message.contains(emoji)) {
                    score += 0.5f
                }
            }

            // 特殊规则加权（完全照搬原版）
            if (emotion == "sad" && message.contains("不想") && message.contains("了")) {
                score += 0.3f
            }

            if (emotion == "lonely" && (message.contains("没人") || message.contains("一个人"))) {
                score += 0.2f
            }

            scores[emotion] = score
        }

        // 2. 找到最高分（完全照搬）
        var maxScore = 0f
        var maxEmotion = "neutral"

        for ((emotion, score) in scores) {
            if (score > maxScore) {
                maxScore = score
                maxEmotion = emotion
            }
        }

        // 3. 返回结果（完全照搬置信度计算公式）
        return EmotionDetection(
            emotion = when (maxEmotion) {
                "happy" -> EmotionType.HAPPY
                "sad" -> EmotionType.SAD
                "lonely" -> EmotionType.LONELY
                "hurt" -> EmotionType.HURT
                "angry" -> EmotionType.ANGRY
                "tired" -> EmotionType.TIRED
                "excited" -> EmotionType.EXCITED
                "confused" -> EmotionType.CONFUSED
                else -> EmotionType.NEUTRAL
            },
            confidence = min(maxScore / 2f, 1f),  // 归一化到0-1（完全照搬）
            allScores = scores
        )
    }

    /**
     * 获取情绪指导语（完全照搬原版）
     */
    fun getGuidance(emotion: EmotionType): String {
        return when (emotion) {
            EmotionType.HAPPY -> "用户情绪不错，可以更活泼地回应"
            EmotionType.SAD -> "用户情绪低落，多倾听少建议，给予温暖支持"
            EmotionType.TIRED -> "用户很疲惫，表达关心，鼓励休息"
            EmotionType.ANGRY -> "用户有些生气，允许情绪发泄，避免说教"
            EmotionType.CONFUSED -> "用户有些困惑，用温和的引导性问题帮助理清思路"
            EmotionType.EXCITED -> "用户很兴奋，分享这份喜悦"
            EmotionType.LONELY -> "用户感到孤独，温暖陪伴"
            EmotionType.HURT -> "用户感到委屈，倾听和理解"
            EmotionType.NEUTRAL -> "正常对话"
        }
    }
}

/**
 * 情绪模式（来自原版）
 */
data class EmotionPattern(
    val keywords: List<String>,
    val emojis: List<String>,
    val score: Float
)

/**
 * 情绪类型
 */
enum class EmotionType {
    HAPPY,      // 开心
    SAD,        // 难过
    LONELY,     // 孤独
    HURT,       // 委屈
    ANGRY,      // 生气
    TIRED,      // 疲惫
    EXCITED,    // 兴奋
    CONFUSED,   // 困惑
    NEUTRAL     // 中性
}

/**
 * 情绪检测结果
 */
data class EmotionDetection(
    val emotion: EmotionType,
    val confidence: Float,  // 0.0 - 1.0
    val allScores: Map<String, Float> = emptyMap()
)
