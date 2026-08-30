package com.companion.cc.domain.character

import com.companion.cc.data.local.entity.CharacterMemoryCapsuleEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CharacterMemoryCapsuleRepositoryTest {
    private lateinit var dao: com.companion.cc.data.local.dao.CharacterMemoryCapsuleDao
    private lateinit var repository: CharacterMemoryCapsuleRepository

    @Before
    fun setUp() {
        dao = mock()
        repository = com.companion.cc.data.repository.RoomCharacterMemoryCapsuleRepository(dao)
    }

    @Test
    fun blankTokenDoesNotQueryDatabase() = runTest {
        assertNull(repository.findByToken("user-1", " "))
        verify(dao, never()).findByTokenHash(org.mockito.kotlin.any(), org.mockito.kotlin.any())
    }

    @Test
    fun restoredCapsuleCannotBeFoundForAnotherRestore() = runTest {
        val capsule = capsule(status = "restored")
        whenever(dao.findByTokenHash(eq("user-1"), org.mockito.kotlin.any())).thenReturn(capsule)

        assertNull(repository.findByToken("user-1", "token"))
    }

    private fun capsule(status: String) = CharacterMemoryCapsuleEntity(
        id = "capsule-1",
        userId = "user-1",
        sourceCharacterId = "character-1",
        schemaVersion = 1,
        revivalTokenHash = CharacterMemoryCapsuleToken.hash("token"),
        encryptedRevivalToken = "encrypted-token",
        encryptedPayload = "encrypted-payload",
        status = status,
        createdAt = 1L,
        deletedAt = 2L
    )
}
