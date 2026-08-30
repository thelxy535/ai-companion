package com.companion.cc.domain.character

import com.companion.cc.data.local.entity.CharacterMemoryCapsuleEntity
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class CharacterMemoryCapsuleRepositoryRestoreTest {
    @Test
    fun markRestoredPersistsNewCharacterIdAndConsumedStatus() = runTest {
        val dao = mock<com.companion.cc.data.local.dao.CharacterMemoryCapsuleDao>()
        val repository = com.companion.cc.data.repository.RoomCharacterMemoryCapsuleRepository(dao)
        val capsule = CharacterMemoryCapsuleEntity(
            id = "capsule-1", userId = "user-1", sourceCharacterId = "character-1",
            schemaVersion = 1, revivalTokenHash = "hash", encryptedRevivalToken = "token",
            encryptedPayload = "payload", createdAt = 1L, deletedAt = 2L
        )

        repository.markRestored(capsule, "new-character", 3L)

        verify(dao).update(capsule.copy(
            status = "restored",
            restoredAt = 3L,
            restoredCharacterId = "new-character"
        ))
    }
}
