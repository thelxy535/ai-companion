package com.companion.cc.domain.model

import kotlinx.serialization.Serializable

/**
 * 自定义角色配置
 */
@Serializable
data class CustomCharacter(
    val id: String,
    val userId: String,
    val name: String,
    val avatar: String?,
    val description: String,
    val personality: PersonalityTraits,
    val backstory: String,
    val greetingMessage: String,
    val exampleDialogues: List<ExampleDialogue>,
    val voiceConfig: VoiceConfig?,
    val behaviorRules: BehaviorRules?,
    val scenario: String = "",
    val alternateGreetings: List<String> = emptyList(),
    val creatorNotes: String = "",
    val creator: String = "",
    val characterVersion: String = "1.0",
    val tags: List<String> = emptyList(),
    val systemPromptOverride: String = "",
    val postHistoryInstructions: String = "",
    val characterBook: List<CharacterBookEntry> = emptyList(),
    val rhythm: com.companion.cc.domain.character.CompanionRhythm = com.companion.cc.domain.character.CompanionRhythm(),
    val isCustom: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 人格特质（基于五大人格理论 Big Five）
 */
@Serializable
data class PersonalityTraits(
    val openness: Float,              // 开放性 (0-1): 对新体验和想法的接受程度
    val conscientiousness: Float,     // 尽责性 (0-1): 负责任和自律程度
    val extraversion: Float,          // 外向性 (0-1): 社交活跃度和表达热情
    val agreeableness: Float,         // 宜人性 (0-1): 友好、合作和同理心程度
    val neuroticism: Float,           // 神经质 (0-1): 情绪不稳定和敏感程度
    val customTraits: Map<String, String> = emptyMap() // 自定义特质
) {
    companion object {
        const val NATURAL_DESCRIPTION_KEY = "人格底色"
        const val MEMORY_PREFERENCE_KEY = "记忆偏好"

        fun default() = PersonalityTraits(
            openness = 0.7f,
            conscientiousness = 0.6f,
            extraversion = 0.7f,
            agreeableness = 0.8f,
            neuroticism = 0.3f,
            customTraits = emptyMap()
        )
    }
}

fun PersonalityTraits.naturalDescription(): String =
    customTraits[PersonalityTraits.NATURAL_DESCRIPTION_KEY].orEmpty()

fun PersonalityTraits.withNaturalDescription(value: String): PersonalityTraits {
    val traits = customTraits.toMutableMap()
    if (value.isBlank()) traits.remove(PersonalityTraits.NATURAL_DESCRIPTION_KEY)
    else traits[PersonalityTraits.NATURAL_DESCRIPTION_KEY] = value.trim()
    return copy(customTraits = traits.toMap())
}

/**
 * 示例对话
 */
@Serializable
data class ExampleDialogue(
    val user: String,
    val assistant: String
)

/**
 * 语音配置
 */
@Serializable
data class VoiceConfig(
    val pitch: Float = 1.0f,          // 音调 (0.5-2.0)
    val speed: Float = 1.0f,          // 语速 (0.5-2.0)
    val volume: Float = 1.0f,         // 音量 (0.0-1.0)
    val voiceId: String? = null       // TTS引擎语音ID
) {
    companion object {
        fun default() = VoiceConfig()
    }
}

/**
 * 行为规则
 */
@Serializable
data class BehaviorRules(
    val responseStyle: ResponseStyle,      // 回复风格
    val emojiFrequency: EmojiFrequency,   // 表情符号频率
    val formalityLevel: FormalityLevel,   // 正式程度
    val topicPreferences: List<String>,   // 话题偏好
    val avoidTopics: List<String>         // 避免话题
) {
    companion object {
        fun default() = BehaviorRules(
            responseStyle = ResponseStyle.FRIENDLY,
            emojiFrequency = EmojiFrequency.MEDIUM,
            formalityLevel = FormalityLevel.CASUAL,
            topicPreferences = emptyList(),
            avoidTopics = emptyList()
        )
    }
}

/**
 * 回复风格
 */
@Serializable
enum class ResponseStyle(val displayName: String) {
    CASUAL("随意轻松"),
    PLAYFUL("活泼俏皮"),
    SERIOUS("严肃认真"),
    ROMANTIC("浪漫温柔"),
    FRIENDLY("友好亲切"),
    PROFESSIONAL("专业正式")
}

/**
 * 表情符号频率
 */
@Serializable
enum class EmojiFrequency(val displayName: String) {
    NONE("不使用"),
    LOW("偶尔使用"),
    MEDIUM("适度使用"),
    HIGH("频繁使用")
}

/**
 * 正式程度
 */
@Serializable
enum class FormalityLevel(val displayName: String) {
    VERY_FORMAL("非常正式"),
    FORMAL("正式"),
    NEUTRAL("中性"),
    CASUAL("随意"),
    VERY_CASUAL("非常随意")
}
