package com.companion.cc.domain.character

import com.companion.cc.domain.identity.CurrentUserProvider

sealed interface CharacterCleanupWorkResult {
    data object Success : CharacterCleanupWorkResult
    data object Retry : CharacterCleanupWorkResult
}

class CharacterCleanupWorkRunner(
    private val currentUserProvider: CurrentUserProvider,
    private val processPending: suspend (String) -> CharacterCleanupResult
) {
    suspend fun run(): CharacterCleanupWorkResult = runCatching {
        val result = processPending(currentUserProvider.requireUserId())
        if (result.isComplete) {
            CharacterCleanupWorkResult.Success
        } else {
            CharacterCleanupWorkResult.Retry
        }
    }.getOrDefault(CharacterCleanupWorkResult.Retry)
}
