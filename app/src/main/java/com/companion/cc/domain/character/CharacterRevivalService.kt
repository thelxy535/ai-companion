package com.companion.cc.domain.character

import com.companion.cc.domain.model.CustomCharacter
import com.companion.cc.domain.memory.MemoryCapsuleV2
import java.util.UUID

enum class CharacterRevivalMode {
    REINTRODUCTION,
    WITH_MEMORIES
}

fun interface CharacterMemoryCapsuleDecryptor {
    fun decrypt(value: String): String
}

class CharacterRevivalService(
    private val capsules: CharacterMemoryCapsuleRepository,
    private val transaction: CharacterRevivalTransaction,
    private val capsuleBuilder: CharacterMemoryCapsuleBuilder,
    private val decryptor: CharacterMemoryCapsuleDecryptor,
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun revive(
        userId: String,
        token: String,
        mode: CharacterRevivalMode
    ): Result<CustomCharacter> = runCatching {
        val capsule = requireNotNull(capsules.findByToken(userId, token)) {
            "Capsule token is invalid, expired, or already used"
        }
        val restoredPayload = capsuleBuilder.restorePayload(
            serialized = decryptor.decrypt(capsule.encryptedPayload),
            userId = userId,
            characterId = UUID.randomUUID().toString()
        )
        require(
            restoredPayload.sourceCharacterId == null ||
                restoredPayload.sourceCharacterId == capsule.sourceCharacterId
        ) { "Capsule source character does not match payload" }
        val memorySnapshot: MemoryCapsuleV2? = when (mode) {
            CharacterRevivalMode.REINTRODUCTION -> null
            CharacterRevivalMode.WITH_MEMORIES -> requireNotNull(restoredPayload.memorySnapshot) {
                "Capsule does not contain a memory snapshot"
            }
        }
        val restored = restoredPayload.character
        check(transaction.reviveAtomically(capsule, restored, now(), memorySnapshot)) {
            "Capsule revival was not committed"
        }
        restored
    }
}
