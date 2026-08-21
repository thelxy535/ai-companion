package com.companion.cc.domain.character

import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.repository.CustomCharacterRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class LegacyCharacterOwnershipMigratorTest {
    private val users = mock<CurrentUserProvider>()
    private val repository = mock<CustomCharacterRepository>()

    @Test
    fun `moves legacy rows when real user has no characters`() = runTest {
        whenever(users.requireUserId()).thenReturn("user-real")
        whenever(repository.getCharacterCount("user-real")).thenReturn(0)
        whenever(repository.getCharacterCount("default")).thenReturn(2)
        whenever(repository.reassignCharacters("default", "user-real")).thenReturn(2)

        val result = LegacyCharacterOwnershipMigrator(users, repository).migrateIfNeeded()

        assertEquals(LegacyOwnershipMigrationResult.Migrated(2), result)
    }

    @Test
    fun `keeps legacy rows when target already owns characters`() = runTest {
        whenever(users.requireUserId()).thenReturn("user-real")
        whenever(repository.getCharacterCount("user-real")).thenReturn(1)
        whenever(repository.getCharacterCount("default")).thenReturn(2)

        val result = LegacyCharacterOwnershipMigrator(users, repository).migrateIfNeeded()

        assertEquals(LegacyOwnershipMigrationResult.Conflict(2, 1), result)
        verify(repository, never()).reassignCharacters("default", "user-real")
    }
}
