package com.companion.cc.ui.character

/**
 * Represents the state of a character save operation.
 */
sealed interface CharacterSaveState {
    /**
     * No save operation in progress.
     */
    data object Idle : CharacterSaveState

    /**
     * Save operation is in progress.
     */
    data object Saving : CharacterSaveState

    /**
     * Save operation completed successfully.
     * @param characterId The ID of the saved character
     */
    data class Success(val characterId: String) : CharacterSaveState

    /**
     * Save operation failed.
     * @param message Error message describing what went wrong
     */
    data class Failure(val message: String) : CharacterSaveState
}
