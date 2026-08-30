package com.companion.cc.domain.character

import com.companion.cc.domain.model.CompanionConfig

sealed interface CharacterPromptSource {
    data object BuiltIn : CharacterPromptSource
    data object Custom : CharacterPromptSource
}

data class ResolvedCharacterPrompt(
    val config: CompanionConfig,
    val source: CharacterPromptSource = CharacterPromptSource.Custom,
)

class UnknownCharacterException(val characterId: String) :
    NoSuchElementException("Character not found: $characterId")

interface CharacterPromptResolver {
    suspend fun resolve(characterId: String): ResolvedCharacterPrompt
}
