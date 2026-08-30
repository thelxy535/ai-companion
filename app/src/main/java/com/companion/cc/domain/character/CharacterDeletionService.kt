package com.companion.cc.domain.character

import com.companion.cc.data.local.entity.CharacterCleanupTaskEntity
import com.companion.cc.data.local.entity.CharacterMemoryCapsuleEntity
import com.companion.cc.domain.memory.MemoryCapsuleV2
import com.companion.cc.domain.model.CustomCharacter
import com.companion.cc.domain.repository.CustomCharacterRepository
import java.util.UUID

fun interface CharacterMemoryCapsuleTokenSource {
    fun generate(): String
}

fun interface CharacterMemoryCapsuleMemoryProvider {
    suspend fun snapshot(userId: String, characterId: String): MemoryCapsuleV2?
}

fun interface CharacterMemoryCapsuleCipher {
    fun encrypt(value: String): String
}

data class CharacterDeletionResult(
    val capsuleId: String,
    val revivalToken: String
)

interface CharacterDeletionTransaction {
    suspend fun deleteAtomically(
        userId: String,
        characterId: String,
        capsuleFactory: suspend () -> CharacterMemoryCapsuleEntity,
        cleanupTask: CharacterCleanupTaskEntity
    ): Boolean
}

class CharacterDeletionService(
    private val characterRepository: CustomCharacterRepository,
    private val transaction: CharacterDeletionTransaction,
    private val capsuleBuilder: CharacterMemoryCapsuleBuilder,
    private val tokenSource: CharacterMemoryCapsuleTokenSource,
    private val cipher: CharacterMemoryCapsuleCipher,
    private val memorySnapshotProvider: CharacterMemoryCapsuleMemoryProvider =
        CharacterMemoryCapsuleMemoryProvider { _, _ -> null },
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun delete(
        userId: String,
        characterId: String
    ): Result<CharacterDeletionResult> = delete(userId, characterId, now())

    suspend fun delete(
        userId: String,
        characterId: String,
        now: Long
    ): Result<CharacterDeletionResult> {
        val character = characterRepository.getCharacterByIdForUser(characterId, userId)
            ?: return Result.failure(IllegalArgumentException("Character does not belong to user"))
        val token = tokenSource.generate()
        val capsuleId = UUID.randomUUID().toString()
        val cleanupTask = CharacterCleanupTaskEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            characterId = characterId,
            avatarReference = character.avatar,
            createdAt = now,
            updatedAt = now
        )
        return if (
            transaction.deleteAtomically(
                userId = userId,
                characterId = characterId,
                capsuleFactory = {
                    val memorySnapshot = memorySnapshotProvider.snapshot(userId, characterId)
                    val payload = capsuleBuilder.build(character, memorySnapshot)
                    CharacterMemoryCapsuleEntity(
                        id = capsuleId,
                        userId = userId,
                        sourceCharacterId = characterId,
                        schemaVersion = 2,
                        revivalTokenHash = CharacterMemoryCapsuleToken.hash(token),
                        encryptedRevivalToken = cipher.encrypt(token),
                        encryptedPayload = cipher.encrypt(payload.serialized),
                        createdAt = now,
                        deletedAt = now
                    )
                },
                cleanupTask = cleanupTask
            )
        ) {
            Result.success(CharacterDeletionResult(capsuleId, token))
        } else {
            Result.failure(IllegalStateException("Character deletion was not committed"))
        }
    }
}
