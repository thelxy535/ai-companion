package com.companion.cc.domain.model

/**
 * Presentation metadata for a character used by chat and character detail screens.
 * It keeps custom characters independent from the built-in companion list.
 */
data class ChatCharacterDisplay(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val avatarUrl: String?
) {
    val avatarFallback: String
        get() = emoji
}

object ChatCharacterDisplayResolver {
    private const val CUSTOM_CHARACTER_EMOJI = "✨"

    fun resolve(
        requestedId: String,
        character: ChatCharacter?,
        builtIns: List<Companion> = companions
    ): ChatCharacterDisplay? {
        if (character == null || character.id != requestedId) return null

        val builtIn = builtIns.firstOrNull { it.id == requestedId }
        return when (character) {
            is ChatCharacter.BuiltIn -> ChatCharacterDisplay(
                id = character.id,
                name = builtIn?.name ?: character.name,
                emoji = builtIn?.emoji ?: CUSTOM_CHARACTER_EMOJI,
                description = builtIn?.description ?: character.description,
                avatarUrl = character.avatar ?: builtIn?.avatarUrl
            )
            is ChatCharacter.Custom -> ChatCharacterDisplay(
                id = character.id,
                name = character.name,
                emoji = CUSTOM_CHARACTER_EMOJI,
                description = character.description,
                avatarUrl = character.avatar
            )
        }
    }
}
