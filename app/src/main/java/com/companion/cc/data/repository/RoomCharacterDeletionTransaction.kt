package com.companion.cc.data.repository

import androidx.room.withTransaction
import com.companion.cc.data.local.database.AppDatabase
import com.companion.cc.data.local.entity.CharacterCleanupTaskEntity
import com.companion.cc.data.local.entity.CharacterMemoryCapsuleEntity
import com.companion.cc.domain.character.CharacterDeletionTransaction
import com.companion.cc.domain.memory.MemoryScopeKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Commits character deletion and its recovery capsule as one Room transaction.
 * External file/avatar cleanup is represented by the durable cleanup task.
 */
@Singleton
class RoomCharacterDeletionTransaction @Inject constructor(
    private val database: AppDatabase
) : CharacterDeletionTransaction {
    override suspend fun deleteAtomically(
        userId: String,
        characterId: String,
        capsuleFactory: suspend () -> CharacterMemoryCapsuleEntity,
        cleanupTask: CharacterCleanupTaskEntity
    ): Boolean = runCatching {
        database.withTransaction {
            val capsule = capsuleFactory()
            database.characterMemoryCapsuleDao().insert(capsule)
            database.characterCleanupTaskDao().insert(cleanupTask)

            database.tagDao().deleteMessageTagsForCompanion(userId, characterId)
            database.messageDao().deleteMessagesByCompanion(userId, characterId)
            database.vectorMemoryDao().deleteAll(userId, characterId)
            database.userEventDao().deleteAllForUserAndCompanion(userId, characterId)
            database.interactionTimeDao().deleteAllForUserAndCompanion(userId, characterId)
            val memoryScope = MemoryScopeKey.forCharacter(userId, characterId)
            database.memoryRelationDao().deleteByScope(memoryScope)
            database.memoryEvidenceDao().deleteByScope(memoryScope)
            database.memoryVersionDao().deleteByScope(memoryScope)
            database.memoryReviewDao().deleteByScope(memoryScope)
            database.memoryRetrievalDao().deleteFeedbackByScope(memoryScope)
            database.memoryRetrievalDao().deleteTracesByScope(memoryScope)
            database.memorySourceDao().deleteByScope(memoryScope)
            database.memoryNodeDao().deleteByScope(memoryScope)

            check(database.customCharacterDao().deleteForUser(characterId, userId) == 1) {
                "Character was deleted or ownership changed"
            }
            true
        }
    }.getOrElse { false }
}
