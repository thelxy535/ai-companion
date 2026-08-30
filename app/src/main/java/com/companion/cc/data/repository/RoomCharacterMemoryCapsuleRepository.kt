package com.companion.cc.data.repository

import com.companion.cc.data.local.dao.CharacterMemoryCapsuleDao
import com.companion.cc.data.local.entity.CharacterMemoryCapsuleEntity
import com.companion.cc.domain.character.CharacterMemoryCapsuleRepository
import com.companion.cc.domain.character.CharacterMemoryCapsuleToken
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomCharacterMemoryCapsuleRepository @Inject constructor(
    private val dao: CharacterMemoryCapsuleDao
) : CharacterMemoryCapsuleRepository {
    override fun observe(userId: String): Flow<List<CharacterMemoryCapsuleEntity>> =
        dao.observeByUser(userId)

    override suspend fun findByToken(
        userId: String,
        token: String
    ): CharacterMemoryCapsuleEntity? {
        if (token.isBlank()) return null
        return dao.findByTokenHash(userId, CharacterMemoryCapsuleToken.hash(token))
            ?.takeIf { it.status == "available" }
    }

    override suspend fun markRestored(
        capsule: CharacterMemoryCapsuleEntity,
        restoredCharacterId: String,
        restoredAt: Long
    ) {
        check(capsule.status == "available") { "Capsule has already been used" }
        dao.update(
            capsule.copy(
                status = "restored",
                restoredAt = restoredAt,
                restoredCharacterId = restoredCharacterId
            )
        )
    }
}
