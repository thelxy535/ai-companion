package com.companion.cc.domain.character

import com.companion.cc.domain.model.ChatCharacter
import kotlinx.coroutines.flow.Flow

/**
 * Unified catalog of all available characters (built-in and custom).
 *
 * This is the single source of truth for character discovery and retrieval.
 * It merges built-in characters with the current user's custom characters.
 */
interface CharacterCatalog {
    /**
     * Observes all available characters for the current user.
     *
     * Emits a new list whenever:
     * - The current user changes
     * - Custom characters are added/updated/deleted
     * - Built-in character configuration changes
     *
     * @return Flow of all available characters, ordered with built-in characters first
     */
    fun observeCharacters(): Flow<List<ChatCharacter>>

    /**
     * Gets a specific character by ID for the current user.
     *
     * Returns:
     * - Built-in character if the ID matches a built-in character
     * - Custom character if the ID matches one owned by the current user
     * - null if the character doesn't exist or is owned by another user
     *
     * @param characterId The character's unique identifier
     * @return The character, or null if not found or not accessible
     */
    suspend fun getCharacter(characterId: String): ChatCharacter?
}
