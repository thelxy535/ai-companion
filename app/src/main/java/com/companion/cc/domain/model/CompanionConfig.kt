package com.companion.cc.domain.model

/**
 * 完整的伴侣人格配置
 *
 * 从 companions.json 加载
 */
data class PersonalityConfig(
    val coreTraits: List<String>,              // 核心特质
    val background: String,                     // 背景故事
    val speakingStyle: SpeakingStyle,          // 说话风格
    val interests: List<String>,                // 兴趣爱好
    val values: List<String>,                   // 价值观
    val relationship: Relationship,             // 关系定位
    val behaviorPatterns: List<String>          // 行为模式
)

/**
 * 说话风格
 */
data class SpeakingStyle(
    val tone: String,                           // 语气（冷静、温柔、活泼）
    val vocabularyLevel: String,                // 词汇水平（学术、日常）
    val sentenceLength: String,                 // 句子长度（短句、中等、长句）
    val useEmoji: Boolean,                      // 是否使用emoji
    val useExclamation: Boolean,                // 是否使用感叹号
    val formality: String                       // 正式程度（正式、非正式）
)

/**
 * 关系定位
 */
data class Relationship(
    val role: String,                           // 角色（引导者、陪伴者）
    val distance: String,                       // 距离感（适度、亲密）
    val interactionStyle: String,               // 互动风格（引导式、关怀式）
    val addressUser: String                     // 称呼用户（你、亲爱的）
)

/**
 * 提示词配置
 */
data class Prompts(
    val system: String,                         // System Prompt
    val greeting: List<String>,                 // 打招呼语
    val farewell: List<String>,                 // 告别语
    val fallback: List<String>                  // 兜底回复
)

/**
 * 情感模型配置
 */
data class EmotionalModelConfig(
    val defaultMood: Mood,                      // 默认心情
    val moodStability: Float,                   // 心情稳定性 (0-1)
    val energyRecoveryRate: Float,              // 精力恢复速度
    val stressThreshold: Float,                 // 压力阈值
    val affectionGrowthRate: Float,             // 好感度增长速度
    val moodTransitions: Map<Mood, List<Mood>>  // 心情转换规则
)

/**
 * 记忆偏好配置
 */
data class MemoryPreferences(
    val importanceThreshold: Float,             // 重要性阈值
    val summaryFrequency: String,               // 摘要频率（daily, weekly）
    val rememberTopics: List<String>,           // 记住的主题
    val forgetTopics: List<String>              // 忘记的主题
)

/**
 * API 参数配置
 */
data class ApiParametersConfig(
    val temperature: Float = 0.8f,
    val topP: Float = 0.9f,
    val frequencyPenalty: Float = 0.0f,
    val presencePenalty: Float = 0.0f,
    val maxTokens: Int = 500
)

/**
 * 完整的伴侣配置
 */
data class CompanionConfig(
    val id: String,
    val name: String,
    val emoji: String,
    val avatar: String,
    val enabled: Boolean,
    val personality: PersonalityConfig,
    val prompts: Prompts,
    val emotionalModel: EmotionalModelConfig,
    val memoryPreferences: MemoryPreferences,
    val apiParameters: ApiParametersConfig = ApiParametersConfig()
)

/**
 * companions.json 的根对象
 */
data class CompanionsConfig(
    val version: String,
    val companions: List<CompanionConfig>
)
