package com.companion.cc.domain.manager

import com.companion.cc.domain.model.*
import com.companion.cc.domain.repository.CustomCharacterRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 角色自定义管理器
 *
 * 负责：
 * 1. 创建、更新、删除自定义角色
 * 2. 生成动态 System Prompt
 * 3. 集成到现有 PersonalityManager
 */
@Singleton
class CharacterCustomizationManager @Inject constructor(
    private val characterRepository: CustomCharacterRepository,
    private val personalityManager: PersonalityManager
) {

    /**
     * 创建新的自定义角色
     */
    suspend fun createCharacter(
        userId: String,
        name: String,
        description: String,
        personality: PersonalityTraits,
        backstory: String,
        greetingMessage: String = "你好，我是 $name",
        exampleDialogues: List<ExampleDialogue> = emptyList(),
        voiceConfig: VoiceConfig? = null,
        behaviorRules: BehaviorRules? = null
    ): CustomCharacter {
        val characterId = UUID.randomUUID().toString()
        val character = CustomCharacter(
            id = characterId,
            userId = userId,
            name = name,
            avatar = null,
            description = description,
            personality = personality,
            backstory = backstory,
            greetingMessage = greetingMessage,
            exampleDialogues = exampleDialogues,
            voiceConfig = voiceConfig,
            behaviorRules = behaviorRules
        )

        // 保存到数据库
        characterRepository.saveCharacter(character)

        // 注册到 PersonalityManager
        registerCharacterToPersonalityManager(character)

        return character
    }

    /**
     * 更新角色
     */
    suspend fun updateCharacter(character: CustomCharacter): CustomCharacter {
        characterRepository.updateCharacter(character)
        registerCharacterToPersonalityManager(character)
        return character
    }

    /**
     * 删除角色
     */
    suspend fun deleteCharacter(userId: String, characterId: String) {
        characterRepository.deleteCharacter(characterId)
        personalityManager.removeCompanion(characterId)
    }

    /**
     * 获取角色
     */
    suspend fun getCharacter(userId: String, characterId: String): CustomCharacter? {
        return characterRepository.getCharacterById(characterId)?.takeIf { it.userId == userId }
    }

    /**
     * 获取用户的所有角色
     */
    fun getUserCharacters(userId: String): Flow<List<CustomCharacter>> {
        return characterRepository.getCharactersByUser(userId)
    }

    /**
     * 注册角色到 PersonalityManager
     */
    private fun registerCharacterToPersonalityManager(character: CustomCharacter) {
        val companionPersonality = buildCompanionPersonality(character)
        personalityManager.registerCompanion(companionPersonality)
    }

    /**
     * 根据自定义角色构建 CompanionPersonality
     */
    private fun buildCompanionPersonality(character: CustomCharacter): CompanionPersonality {
        return CompanionPersonality(
            id = character.id,
            name = character.name,
            greeting = character.greetingMessage,
            systemPrompt = buildSystemPrompt(character),
            traits = extractTraits(character.personality),
            responseStyle = buildResponseStyle(character),
            apiParameters = buildApiParameters(character.personality)
        )
    }

    /**
     * 构建 System Prompt
     */
    private fun buildSystemPrompt(character: CustomCharacter): String {
        val personality = character.personality
        val rules = character.behaviorRules

        return buildString {
            appendLine("# 角色设定")
            appendLine("你是 ${character.name}。${character.description}")
            appendLine()

            appendLine("## 背景故事")
            appendLine(character.backstory)
            appendLine()

            appendLine("## 人格特质")
            appendLine("- 开放性: ${formatPercentage(personality.openness)} - ${interpretOpenness(personality.openness)}")
            appendLine("- 尽责性: ${formatPercentage(personality.conscientiousness)} - ${interpretConscientiousness(personality.conscientiousness)}")
            appendLine("- 外向性: ${formatPercentage(personality.extraversion)} - ${interpretExtraversion(personality.extraversion)}")
            appendLine("- 宜人性: ${formatPercentage(personality.agreeableness)} - ${interpretAgreeableness(personality.agreeableness)}")
            appendLine("- 情绪稳定性: ${formatPercentage(1f - personality.neuroticism)} - ${interpretNeuroticism(personality.neuroticism)}")
            appendLine()

            if (rules != null) {
                appendLine("## 行为风格")
                appendLine("- 回复风格: ${rules.responseStyle.displayName}")
                appendLine("- 表情符号使用: ${rules.emojiFrequency.displayName}")
                appendLine("- 正式程度: ${rules.formalityLevel.displayName}")

                if (rules.topicPreferences.isNotEmpty()) {
                    appendLine("- 偏好话题: ${rules.topicPreferences.joinToString(", ")}")
                }

                if (rules.avoidTopics.isNotEmpty()) {
                    appendLine("- 避免话题: ${rules.avoidTopics.joinToString(", ")}")
                }
                appendLine()
            }

            if (character.exampleDialogues.isNotEmpty()) {
                appendLine("## 示例对话")
                character.exampleDialogues.take(5).forEach { dialogue ->
                    appendLine("用户: ${dialogue.user}")
                    appendLine("${character.name}: ${dialogue.assistant}")
                    appendLine()
                }
            }

            appendLine("# 对话指南")
            appendLine("请基于以上设定与用户进行自然对话。保持角色一致性，展现出你独特的人格特质。")
        }
    }

    /**
     * 提取特质列表
     */
    private fun extractTraits(personality: PersonalityTraits): List<String> {
        val traits = mutableListOf<String>()

        // 基于人格特质生成描述
        if (personality.openness > 0.7f) traits.add("富有创造力")
        if (personality.conscientiousness > 0.7f) traits.add("负责可靠")
        if (personality.extraversion > 0.7f) traits.add("外向活泼")
        if (personality.agreeableness > 0.7f) traits.add("友善体贴")
        if (personality.neuroticism < 0.3f) traits.add("情绪稳定")

        return traits
    }

    /**
     * 构建回复风格
     */
    private fun buildResponseStyle(character: CustomCharacter): String {
        val rules = character.behaviorRules ?: return "friendly"

        return when (rules.responseStyle) {
            ResponseStyle.CASUAL -> "casual"
            ResponseStyle.PLAYFUL -> "playful"
            ResponseStyle.SERIOUS -> "serious"
            ResponseStyle.ROMANTIC -> "romantic"
            ResponseStyle.FRIENDLY -> "friendly"
            ResponseStyle.PROFESSIONAL -> "professional"
        }
    }

    /**
     * 构建 API 参数
     */
    private fun buildApiParameters(personality: PersonalityTraits): Map<String, Any> {
        // 根据人格特质调整 temperature 和其他参数
        val temperature = when {
            personality.openness > 0.8f -> 0.9f
            personality.openness > 0.6f -> 0.8f
            personality.openness > 0.4f -> 0.7f
            else -> 0.6f
        }

        return mapOf(
            "temperature" to temperature,
            "top_p" to 0.9f
        )
    }

    // 人格特质解释辅助函数
    private fun formatPercentage(value: Float) = "${(value * 100).toInt()}%"

    private fun interpretOpenness(value: Float) = when {
        value > 0.7f -> "喜欢探索新事物，富有想象力"
        value > 0.5f -> "对新体验保持开放态度"
        value > 0.3f -> "倾向于熟悉的事物"
        else -> "喜欢传统和常规"
    }

    private fun interpretConscientiousness(value: Float) = when {
        value > 0.7f -> "做事有条理，目标明确"
        value > 0.5f -> "较为负责和自律"
        value > 0.3f -> "比较随性灵活"
        else -> "自由随意，不拘小节"
    }

    private fun interpretExtraversion(value: Float) = when {
        value > 0.7f -> "外向开朗，喜欢社交"
        value > 0.5f -> "适度外向，社交活跃"
        value > 0.3f -> "比较内向安静"
        else -> "内向沉静，喜欢独处"
    }

    private fun interpretAgreeableness(value: Float) = when {
        value > 0.7f -> "友善温和，富有同理心"
        value > 0.5f -> "善于合作，容易相处"
        value > 0.3f -> "直率坦诚"
        else -> "独立自主，直言不讳"
    }

    private fun interpretNeuroticism(value: Float) = when {
        value > 0.7f -> "情绪敏感，容易焦虑"
        value > 0.5f -> "情绪有一定波动"
        value > 0.3f -> "情绪较为稳定"
        else -> "情绪稳定，心态平和"
    }
}
