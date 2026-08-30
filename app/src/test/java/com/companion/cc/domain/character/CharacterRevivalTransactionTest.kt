package com.companion.cc.domain.character

import com.companion.cc.data.local.entity.CharacterMemoryCapsuleEntity
import com.companion.cc.domain.model.CustomCharacter
import com.companion.cc.domain.model.PersonalityTraits
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CharacterRevivalTransactionTest {
    private lateinit var capsules: CharacterMemoryCapsuleRepository
    private lateinit var transaction: CharacterRevivalTransaction
    private lateinit var builder: CharacterMemoryCapsuleBuilder
    private lateinit var service: CharacterRevivalService

    @Before
    fun setUp() {
        capsules = mock()
        transaction = mock()
        builder = mock()
        service = CharacterRevivalService(
            capsules = capsules,
            transaction = transaction,
            capsuleBuilder = builder,
            decryptor = CharacterMemoryCapsuleDecryptor { "payload" },
            now = { 3L }
        )
    }

    @Test
    fun revivalSucceedsOnlyWhenCharacterAndCapsuleCommitTogether() = runTest {
        val capsule = capsule()
        whenever(capsules.findByToken("user-1", "token")).thenReturn(capsule)
        whenever(builder.restorePayload(eq("payload"), eq("user-1"), any())).thenAnswer { invocation ->
            CharacterMemoryCapsuleRestored(character(invocation.getArgument(2)), null)
        }
        whenever(transaction.reviveAtomically(eq(capsule), any(), eq(3L), isNull())).thenReturn(false)

        val result = service.revive("user-1", "token", CharacterRevivalMode.REINTRODUCTION)

        assertTrue(result.isFailure)
        verify(transaction).reviveAtomically(eq(capsule), any(), eq(3L), isNull())
    }

    private fun capsule() = CharacterMemoryCapsuleEntity(
        id = "capsule-1", userId = "user-1", sourceCharacterId = "old-id",
        schemaVersion = 1, revivalTokenHash = "hash", encryptedRevivalToken = "token",
        encryptedPayload = "encrypted", createdAt = 1L, deletedAt = 2L
    )

    private fun character(id: String) = CustomCharacter(
        id = id, userId = "user-1", name = "Moss", avatar = null,
        description = "quiet", personality = PersonalityTraits.default(),
        backstory = "story", greetingMessage = "hello",
        exampleDialogues = emptyList(), voiceConfig = null, behaviorRules = null
    )
}
