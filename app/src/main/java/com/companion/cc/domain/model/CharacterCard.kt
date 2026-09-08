package com.companion.cc.domain.model

import com.companion.cc.domain.character.CompanionRhythm
import kotlinx.serialization.Serializable

/** External Character Card V2/V3 representation. Kept separate from the Room/domain model. */
@Serializable
data class CharacterCard(
    val spec: String = "chara_card_v2",
    val specVersion: String = "2.0",
    val data: CharacterCardData
)

@Serializable
data class CharacterCardData(
    val name: String,
    val description: String = "",
    val personality: String = "",
    val scenario: String = "",
    val firstMessage: String = "",
    val alternateGreetings: List<String> = emptyList(),
    val exampleMessages: String = "",
    val creatorNotes: String = "",
    val systemPrompt: String = "",
    val postHistoryInstructions: String = "",
    val tags: List<String> = emptyList(),
    val creator: String = "",
    val characterVersion: String = "1.0",
    val characterBook: List<CharacterBookEntry> = emptyList()
    ,val rhythm: CompanionRhythm = CompanionRhythm()
)

@Serializable
data class CharacterBookEntry(
    val keys: List<String>,
    val content: String,
    val enabled: Boolean = true,
    val constant: Boolean = false,
    val priority: Int = 0,
    val insertionOrder: Int = 0
)
