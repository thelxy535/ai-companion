package com.companion.cc.data.mapper

import com.companion.cc.domain.model.ApiParametersConfig
import com.companion.cc.domain.model.BehaviorRules
import com.companion.cc.domain.model.CompanionConfig
import com.companion.cc.domain.model.CustomCharacter
import com.companion.cc.domain.model.EmotionalModelConfig
import com.companion.cc.domain.model.FormalityLevel
import com.companion.cc.domain.model.MemoryPreferences
import com.companion.cc.domain.model.Mood
import com.companion.cc.domain.model.PersonalityConfig
import com.companion.cc.domain.model.Prompts
import com.companion.cc.domain.model.Relationship
import com.companion.cc.domain.model.SpeakingStyle
import com.companion.cc.domain.character.HumanLikeCharacterGuidance

object CustomCharacterPromptMapper {
    fun toCompanionConfig(character: CustomCharacter): CompanionConfig = CompanionConfig(
        id = character.id,
        name = character.name,
        emoji = "✨",
        avatar = character.avatar.orEmpty(),
        enabled = true,
        prompts = Prompts(
            system = buildSystemPrompt(character),
            greeting = (listOf(character.greetingMessage) + character.alternateGreetings).distinct(),
            farewell = listOf("再见"),
            fallback = listOf("我不太明白你的意思，能再说一遍吗？"),
        ),
        personality = buildPersonalityConfig(character),
        emotionalModel = EmotionalModelConfig(
            defaultMood = Mood.CALM,
            moodStability = (1f - character.personality.neuroticism).coerceIn(0f, 1f),
            energyRecoveryRate = 0.1f,
            stressThreshold = 0.7f,
            affectionGrowthRate = 0.05f,
            moodTransitions = emptyMap(),
        ),
        memoryPreferences = MemoryPreferences(
            importanceThreshold = 0.5f,
            summaryFrequency = "daily",
            rememberTopics = character.behaviorRules?.topicPreferences.orEmpty(),
            forgetTopics = character.behaviorRules?.avoidTopics.orEmpty(),
        ),
        apiParameters = ApiParametersConfig(
            temperature = when {
                character.personality.openness > 0.8f -> 0.9f
                character.personality.openness > 0.6f -> 0.8f
                character.personality.openness > 0.4f -> 0.7f
                else -> 0.6f
            },
            topP = 0.9f,
            maxTokens = 2000,
        ),
    )

    private fun buildPersonalityConfig(character: CustomCharacter): PersonalityConfig {
        val rules = character.behaviorRules ?: BehaviorRules.default()
        val traits = buildList {
            if (character.personality.openness > 0.7f) add("富有创造力")
            if (character.personality.conscientiousness > 0.7f) add("负责可靠")
            if (character.personality.extraversion > 0.7f) add("外向活泼")
            if (character.personality.agreeableness > 0.7f) add("友善体贴")
            if (character.personality.neuroticism < 0.3f) add("情绪稳定")
            addAll(character.personality.customTraits.values)
        }.distinct()
        return PersonalityConfig(
            coreTraits = traits,
            background = character.backstory,
            speakingStyle = SpeakingStyle(
                tone = rules.responseStyle.displayName,
                vocabularyLevel = "日常",
                sentenceLength = "中等",
                useEmoji = rules.emojiFrequency.name != "NONE",
                useExclamation = rules.responseStyle.name != "SERIOUS",
                formality = rules.formalityLevel.displayName,
            ),
            interests = rules.topicPreferences,
            values = character.personality.customTraits.values.toList(),
            relationship = Relationship(
                role = "陪伴者",
                distance = "适度",
                interactionStyle = rules.responseStyle.displayName,
                addressUser = "你",
            ),
            behaviorPatterns = listOf(
                "偏好话题：${rules.topicPreferences.joinToString("、")}",
                "避免话题：${rules.avoidTopics.joinToString("、")}",
            ).filterNot { it.endsWith("：") },
        )
    }

    private fun buildSystemPrompt(character: CustomCharacter): String = buildString {
        appendLine("# 角色设定")
        appendLine("你是 ${character.name}。${character.description}")
        appendLine()
        appendLine("## 背景故事")
        appendLine(character.backstory)
        if (character.scenario.isNotBlank()) {
            appendLine()
            appendLine("## 当前场景")
            appendLine(character.scenario)
        }
        if (character.creatorNotes.isNotBlank()) {
            appendLine()
            appendLine("## 创作者备注")
            appendLine(character.creatorNotes)
        }
        if (character.tags.isNotEmpty()) {
            appendLine()
            appendLine("## 角色标签")
            appendLine(character.tags.joinToString("、"))
        }
        appendLine()
        appendLine("## 人格特质")
        appendLine("- 开放性：${(character.personality.openness * 100).toInt()}%")
        appendLine("- 尽责性：${(character.personality.conscientiousness * 100).toInt()}%")
        appendLine("- 外向性：${(character.personality.extraversion * 100).toInt()}%")
        appendLine("- 宜人性：${(character.personality.agreeableness * 100).toInt()}%")
        appendLine("- 神经质：${(character.personality.neuroticism * 100).toInt()}%")
        character.personality.customTraits.forEach { (name, value) ->
            appendLine("- $name：$value")
        }
        character.behaviorRules?.let { rules ->
            appendLine()
            appendLine("## 行为规则")
            appendLine("- 回复风格：${rules.responseStyle.displayName}")
            appendLine("- 表情频率：${rules.emojiFrequency.displayName}")
            appendLine("- 正式程度：${rules.formalityLevel.displayName}")
            if (rules.topicPreferences.isNotEmpty()) appendLine("- 偏好话题：${rules.topicPreferences.joinToString("、")}")
            if (rules.avoidTopics.isNotEmpty()) appendLine("- 避免话题：${rules.avoidTopics.joinToString("、")}")
        }
        if (character.exampleDialogues.isNotEmpty()) {
            appendLine()
            appendLine("## 示例对话")
            character.exampleDialogues.take(5).forEach { dialogue ->
                appendLine("用户：${dialogue.user}")
                appendLine("${character.name}：${dialogue.assistant}")
            }
        }
        appendLine()
        appendLine("## 初次问候")
        appendLine(character.greetingMessage)
        if (character.systemPromptOverride.isNotBlank()) {
            appendLine()
            appendLine("## 高级系统指令")
            appendLine(character.systemPromptOverride)
        }
        if (character.postHistoryInstructions.isNotBlank()) {
            appendLine()
            appendLine("## 历史消息后指令")
            appendLine(character.postHistoryInstructions)
        }
        appendLine()
        appendLine(HumanLikeCharacterGuidance.TEXT)
        appendLine()
        appendLine("请始终依据以上设定自然回应，并保持角色一致性。")
    }
}
