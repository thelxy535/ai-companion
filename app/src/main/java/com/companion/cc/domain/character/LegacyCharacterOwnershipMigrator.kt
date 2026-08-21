package com.companion.cc.domain.character

import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.repository.CustomCharacterRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Migrates custom characters created before user-scoped storage was enforced.
 *
 * Early versions saved characters under userId="default". This migrator moves those
 * characters to the real user's ID when safe to do so (i.e., when the target user
 * has no characters yet, indicating a clean migration).
 */
@Singleton
class LegacyCharacterOwnershipMigrator @Inject constructor(
    private val users: CurrentUserProvider,
    private val repository: CustomCharacterRepository
) {
    /**
     * Migrates legacy characters from "default" to the current user if safe.
     *
     * @return Migration result indicating success, conflict, or no action needed
     */
    suspend fun migrateIfNeeded(): LegacyOwnershipMigrationResult {
        val currentUserId = users.requireUserId()
        val legacyCount = repository.getCharacterCount("default")

        if (legacyCount == 0) {
            return LegacyOwnershipMigrationResult.NoLegacyData
        }

        val targetCount = repository.getCharacterCount(currentUserId)

        // Only migrate if target user has no characters - ensures clean migration
        return if (targetCount == 0) {
            val migrated = repository.reassignCharacters("default", currentUserId)
            LegacyOwnershipMigrationResult.Migrated(migrated)
        } else {
            // Conflict: both legacy and target have characters
            LegacyOwnershipMigrationResult.Conflict(legacyCount, targetCount)
        }
    }
}
