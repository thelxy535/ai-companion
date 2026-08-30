package com.companion.cc.domain.character

import com.companion.cc.data.local.entity.CharacterMemoryCapsuleEntity
import com.companion.cc.domain.memory.MemoryCapsuleNode
import com.companion.cc.domain.memory.MemoryCapsuleV2
import com.companion.cc.domain.model.CustomCharacter
import com.companion.cc.domain.model.PersonalityTraits
import com.companion.cc.domain.repository.CustomCharacterRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CharacterDeletionServiceTest {
    private lateinit var characters: CustomCharacterRepository
    private lateinit var transaction: CharacterDeletionTransaction
    private lateinit var service: CharacterDeletionService

    @Before
    fun setUp() {
        characters = mock()
        transaction = mock()
        service = CharacterDeletionService(
            characterRepository = characters,
            transaction = transaction,
            capsuleBuilder = CharacterMemoryCapsuleBuilder(),
            tokenSource = CharacterMemoryCapsuleTokenSource { "token-1" },
            cipher = CharacterMemoryCapsuleCipher { value -> "encrypted:$value" }
        )
    }

    @Test
    fun missingCharacterDoesNotStartDeletion() = runTest {
        whenever(characters.getCharacterByIdForUser("character-1", "user-1"))
            .thenReturn(null)

        val result = service.delete("user-1", "character-1", now = 123L)

        assertTrue(result.isFailure)
        assertFalse(result.exceptionOrNull() is NullPointerException)
        verify(transaction, org.mockito.kotlin.never()).deleteAtomically(
            org.mockito.kotlin.any(),
            org.mockito.kotlin.any(),
            org.mockito.kotlin.any(),
            org.mockito.kotlin.any()
        )
    }

    @Test
    fun successfulDeletionBuildsCapsuleAndDelegatesOneAtomicOperation() = runTest {
        val character = character()
        whenever(characters.getCharacterByIdForUser("character-1", "user-1"))
            .thenReturn(character)
        whenever(transaction.deleteAtomically(
            userId = eq("user-1"),
            characterId = eq("character-1"),
            capsuleFactory = org.mockito.kotlin.any(),
            cleanupTask = org.mockito.kotlin.any()
        )).thenReturn(true)

        val result = service.delete("user-1", "character-1", now = 123L)

        assertTrue(result.isSuccess)
        assertEquals("token-1", result.getOrThrow().revivalToken)
        verify(transaction).deleteAtomically(
            userId = eq("user-1"),
            characterId = eq("character-1"),
            capsuleFactory = org.mockito.kotlin.any(),
            cleanupTask = org.mockito.kotlin.any()
        )
    }

    @Test
    fun successfulDeletionStoresMemorySnapshotInCapsulePayload() = runTest {
        val character = character()
        val snapshot = memorySnapshot()
        whenever(characters.getCharacterByIdForUser("character-1", "user-1"))
            .thenReturn(character)
        whenever(transaction.deleteAtomically(
            userId = eq("user-1"),
            characterId = eq("character-1"),
            capsuleFactory = org.mockito.kotlin.any(),
            cleanupTask = org.mockito.kotlin.any()
        )).thenReturn(true)
        val snapshotService = CharacterDeletionService(
            characterRepository = characters,
            transaction = transaction,
            capsuleBuilder = CharacterMemoryCapsuleBuilder(),
            tokenSource = CharacterMemoryCapsuleTokenSource { "token-1" },
            cipher = CharacterMemoryCapsuleCipher { value -> value },
            memorySnapshotProvider = CharacterMemoryCapsuleMemoryProvider { _, _ -> snapshot }
        )

        val result = snapshotService.delete("user-1", "character-1", now = 123L)

        assertTrue(result.isSuccess)
        val capsuleFactory = org.mockito.kotlin.argumentCaptor<suspend () -> CharacterMemoryCapsuleEntity>().run {
            verify(transaction).deleteAtomically(
                userId = eq("user-1"),
                characterId = eq("character-1"),
                capsuleFactory = capture(),
                cleanupTask = org.mockito.kotlin.any()
            )
            firstValue
        }
        val capsule = capsuleFactory()
        assertTrue(capsule.schemaVersion >= 2)
        assertTrue(capsule.encryptedPayload.contains("node-1"))
    }

    private fun memorySnapshot() = MemoryCapsuleV2(
        scopeKey = "user:user-1:companion:character-1",
        nodes = listOf(
            MemoryCapsuleNode(
                id = "node-1",
                scopeKey = "user:user-1:companion:character-1",
                title = "Favorite tree",
                content = "The old oak",
                validFrom = 1L,
                createdAt = 1L,
                updatedAt = 1L
            )
        )
    )

    private fun character() = CustomCharacter(
        id = "character-1",
        userId = "user-1",
        name = "Moss",
        avatar = null,
        description = "quiet",
        personality = PersonalityTraits.default(),
        backstory = "story",
        greetingMessage = "hello",
        exampleDialogues = emptyList(),
        voiceConfig = null,
        behaviorRules = null
    )
}
