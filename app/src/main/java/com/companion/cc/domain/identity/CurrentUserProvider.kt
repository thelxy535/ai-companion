package com.companion.cc.domain.identity

import kotlinx.coroutines.flow.Flow

/**
 * Provides the current user's identity.
 *
 * This is the single source of truth for the active user ID across the application.
 * All user-scoped data access (custom characters, messages, memories) must use this provider
 * instead of hardcoded values or accepting user IDs from UI layers.
 */
interface CurrentUserProvider {
    /**
     * Flow of the current user ID, filtered to non-blank values with duplicates removed.
     * Emits whenever the user identity changes.
     */
    val userId: Flow<String>

    /**
     * Returns the current user ID synchronously.
     *
     * @return The current non-blank user ID
     * @throws IllegalStateException if no valid user ID is available
     */
    suspend fun requireUserId(): String
}
