package com.companion.cc.domain.character

import com.companion.cc.data.local.entity.CharacterMemoryCapsuleEntity
import com.companion.cc.domain.memory.MemoryCapsuleNode
import com.companion.cc.domain.memory.MemoryCapsuleV2
import com.companion.cc.domain.model.CustomCharacter
import com.companion.cc.domain.model.PersonalityTraits
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CharacterRevivalServiceTest {
    private lateinit var capsules: CharacterMemoryCapsuleRepository
    private lateinit var transaction: CharacterRevivalTransaction
    private lateinit var capsuleBuilder: CharacterMemoryCapsuleBuilder
    private lateinit var service: CharacterRevivalService

    @Before
    fun setUp() {
        capsules = mock()
        transaction = mock()
        capsuleBuilder = mock()
        service = CharacterRevivalService(
            capsules = capsules,
            transaction = transaction,
            capsuleBuilder = capsuleBuilder,
            decryptor = CharacterMemoryCapsuleDecryptor { "payload" },
            now = { 3L }
        )
    }

    @Test
    fun invalidTokenDoesNotStartRevivalTransaction() = runTest {
        whenever(capsules.findByToken("user-1", "bad-token")).thenReturn(null)

        val result = service.revive("user-1", "bad-token", CharacterRevivalMode.REINTRODUCTION)

        assertTrue(result.isFailure)
        verify(transaction, never()).reviveAtomically(any(), any(), any(), any())
    }

    @Test
    fun withMemoriesRejectsLegacyCapsuleWithoutMemorySnapshot() = runTest {
        val legacyBuilder = CharacterMemoryCapsuleBuilder()
        val legacyCapsule = capsule().copy(
            encryptedPayload = legacyBuilder.build(character("old-id")).serialized
        )
        whenever(capsules.findByToken("user-1", "token")).thenReturn(legacyCapsule)
        whenever(transaction.reviveAtomically(eq(legacyCapsule), any(), eq(3L), any())).thenReturn(true)
        val legacyService = CharacterRevivalService(
            capsules = capsules,
            transaction = transaction,
            capsuleBuilder = legacyBuilder,
            decryptor = CharacterMemoryCapsuleDecryptor { it },
            now = { 3L }
        )

        val result = legacyService.revive("user-1", "token", CharacterRevivalMode.WITH_MEMORIES)

        assertTrue(result.isFailure)
        verify(transaction, never()).reviveAtomically(eq(legacyCapsule), any(), any(), any())
    }

    @Test
    fun validRevivalCreatesFreshCharacterIdAndCommitsTransaction() = runTest {
        val capsule = capsule()
        whenever(capsules.findByToken("user-1", "token")).thenReturn(capsule)
        whenever(capsuleBuilder.restorePayload(eq("payload"), eq("user-1"), any())).thenAnswer { invocation ->
            CharacterMemoryCapsuleRestored(
                character = character(invocation.getArgument(2)),
                memorySnapshot = memorySnapshot()
            )
        }
        whenever(transaction.reviveAtomically(eq(capsule), any(), eq(3L), isNull())).thenReturn(true)

        val result = service.revive("user-1", "token", CharacterRevivalMode.REINTRODUCTION)

        assertTrue(result.isSuccess)
        assertNotEquals(capsule.sourceCharacterId, result.getOrThrow().id)
        verify(transaction).reviveAtomically(capsule, result.getOrThrow(), 3L, null)
    }

    @Test
    fun payloadForDifferentSourceCharacterDoesNotCommitRevival() = runTest {
        val capsule = capsule()
        whenever(capsules.findByToken("user-1", "token")).thenReturn(capsule)
        whenever(capsuleBuilder.restorePayload(eq("payload"), eq("user-1"), any())).thenReturn(
            CharacterMemoryCapsuleRestored(
                character = character("new-character"),
                memorySnapshot = null,
                sourceCharacterId = "different-character"
            )
        )

        val result = service.revive("user-1", "token", CharacterRevivalMode.REINTRODUCTION)

        assertTrue(result.isFailure)
        verify(transaction, never()).reviveAtomically(any(), any(), any(), any())
    }

    @Test
    fun withMemoriesPassesSnapshotToAtomicTransaction() = runTest {
        val capsule = capsule()
        val snapshot = memorySnapshot()
        whenever(capsules.findByToken("user-1", "token")).thenReturn(capsule)
        whenever(capsuleBuilder.restorePayload(eq("payload"), eq("user-1"), any())).thenAnswer { invocation ->
            CharacterMemoryCapsuleRestored(
                character = character(invocation.getArgument(2)),
                memorySnapshot = snapshot
            )
        }
        whenever(transaction.reviveAtomically(eq(capsule), any(), eq(3L), eq(snapshot))).thenReturn(true)

        val result = service.revive("user-1", "token", CharacterRevivalMode.WITH_MEMORIES)

        assertTrue(result.isSuccess)
        verify(transaction).reviveAtomically(capsule, result.getOrThrow(), 3L, snapshot)
    }

    private fun capsule() = CharacterMemoryCapsuleEntity(
        id = "capsule-1", userId = "user-1", sourceCharacterId = "old-id",
        schemaVersion = 1, revivalTokenHash = "hash", encryptedRevivalToken = "token",
        encryptedPayload = "encrypted", createdAt = 1L, deletedAt = 2L
    )

    private fun memorySnapshot() = MemoryCapsuleV2(
        scopeKey = "user:user-1:companion:old-id",
        nodes = listOf(
            MemoryCapsuleNode(
                id = "node-1",
                scopeKey = "user:user-1:companion:old-id",
                title = "Favorite tree",
                content = "The old oak",
                validFrom = 1L,
                createdAt = 1L,
                updatedAt = 1L
            )
        )
    )

    private fun character(id: String) = CustomCharacter(
        id = id, userId = "user-1", name = "Moss", avatar = null,
        description = "quiet", personality = PersonalityTraits.default(),
        backstory = "story", greetingMessage = "hello",
        exampleDialogues = emptyList(), voiceConfig = null, behaviorRules = null
    )
}
