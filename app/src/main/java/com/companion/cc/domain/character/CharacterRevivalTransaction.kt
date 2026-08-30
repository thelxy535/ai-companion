package com.companion.cc.domain.character

import com.companion.cc.data.local.entity.CharacterMemoryCapsuleEntity
import com.companion.cc.domain.memory.MemoryCapsuleV2
import com.companion.cc.domain.model.CustomCharacter

interface CharacterRevivalTransaction {
    suspend fun reviveAtomically(
        capsule: CharacterMemoryCapsuleEntity,
        character: CustomCharacter,
        restoredAt: Long,
        memorySnapshot: MemoryCapsuleV2?
    ): Boolean
}
