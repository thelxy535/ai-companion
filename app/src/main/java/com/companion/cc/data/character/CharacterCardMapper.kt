package com.companion.cc.data.character

import com.companion.cc.domain.model.BehaviorRules
import com.companion.cc.domain.model.CustomCharacter
import com.companion.cc.domain.model.ExampleDialogue
import com.companion.cc.domain.model.PersonalityTraits
import com.companion.cc.domain.model.ResponseStyle
import com.companion.cc.domain.model.EmojiFrequency
import com.companion.cc.domain.model.FormalityLevel
import java.util.UUID

/** Converts an external card into an owned app character with explicit defaults. */
object CharacterCardMapper {
    fun toCard(character: CustomCharacter): com.companion.cc.domain.model.CharacterCard {
        val personalityText = listOf(
            "开放性 ${(character.personality.openness * 100).toInt()}%",
            "尽责性 ${(character.personality.conscientiousness * 100).toInt()}%",
            "外向性 ${(character.personality.extraversion * 100).toInt()}%",
            "宜人性 ${(character.personality.agreeableness * 100).toInt()}%",
            "神经质 ${(character.personality.neuroticism * 100).toInt()}%"
        ).joinToString("，")
        val examples = character.exampleDialogues.joinToString("\n<START>\n") {
            "${it.user}\n${it.assistant}"
        }
        return com.companion.cc.domain.model.CharacterCard(
            data = com.companion.cc.domain.model.CharacterCardData(
                name = character.name,
                description = character.description,
                personality = personalityText,
                scenario = character.scenario,
                firstMessage = character.greetingMessage,
                alternateGreetings = character.alternateGreetings,
                exampleMessages = examples,
                creatorNotes = character.creatorNotes,
                systemPrompt = character.systemPromptOverride,
                postHistoryInstructions = character.postHistoryInstructions,
                tags = character.tags,
                creator = character.creator,
                characterVersion = character.characterVersion
                ,rhythm = character.rhythm
            )
        )
    }

    fun toCustomCharacter(card: com.companion.cc.domain.model.CharacterCard, userId: String): CustomCharacter {
        val data = card.data
        return CustomCharacter(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = data.name,
            avatar = null,
            description = data.description.ifBlank { data.personality },
            personality = PersonalityTraits.default(),
            backstory = data.description,
            greetingMessage = data.firstMessage.ifBlank { "你好，很高兴见到你！" },
            exampleDialogues = parseExamples(data.exampleMessages),
            voiceConfig = null,
            behaviorRules = BehaviorRules.default(),
            scenario = data.scenario,
            alternateGreetings = data.alternateGreetings.take(5),
            creatorNotes = data.creatorNotes,
            creator = data.creator,
            characterVersion = data.characterVersion,
            tags = data.tags.take(20),
            systemPromptOverride = data.systemPrompt,
            postHistoryInstructions = data.postHistoryInstructions,
            characterBook = data.characterBook
            ,rhythm = data.rhythm
        )
    }

    private fun parseExamples(raw: String): List<ExampleDialogue> {
        val normalized = raw
            .replace("\\\\r\\\\n", "\n")
            .replace("\\\\n", "\n")
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
        if (normalized.isBlank()) return emptyList()
        return normalized.split("<START>", ignoreCase = true)
            .flatMap { block ->
                val lines = block.lines().map(String::trim).filter(String::isNotBlank)
                if (lines.size < 2) emptyList()
                else listOf(ExampleDialogue(user = lines.first(), assistant = lines.drop(1).joinToString(" ")))
            }
            .take(5)
    }
}
