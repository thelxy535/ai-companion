package com.companion.cc.domain.character

/**
 * Result of a legacy character ownership migration attempt.
 */
sealed interface LegacyOwnershipMigrationResult {
    /**
     * Successfully migrated [count] characters from "default" to the current user.
     */
    data class Migrated(val count: Int) : LegacyOwnershipMigrationResult

    /**
     * Migration skipped because the current user already owns [targetCount] characters
     * and [legacyCount] characters remain under "default".
     */
    data class Conflict(val legacyCount: Int, val targetCount: Int) : LegacyOwnershipMigrationResult

    /**
     * No legacy characters found to migrate.
     */
    data object NoLegacyData : LegacyOwnershipMigrationResult
}
