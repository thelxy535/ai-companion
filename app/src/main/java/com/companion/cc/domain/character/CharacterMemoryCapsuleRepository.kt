package com.companion.cc.domain.character

import com.companion.cc.data.local.entity.CharacterMemoryCapsuleEntity
import kotlinx.coroutines.flow.Flow

interface CharacterMemoryCapsuleRepository {
    fun observe(userId: String): Flow<List<CharacterMemoryCapsuleEntity>>
    suspend fun findByToken(userId: String, token: String): CharacterMemoryCapsuleEntity?
    suspend fun markRestored(
        capsule: CharacterMemoryCapsuleEntity,
        restoredCharacterId: String,
        restoredAt: Long
    )
}
