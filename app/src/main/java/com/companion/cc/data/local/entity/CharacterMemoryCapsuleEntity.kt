package com.companion.cc.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "character_memory_capsules",
    indices = [
        Index(value = ["userId", "createdAt"]),
        Index(value = ["revivalTokenHash"], unique = true)
    ]
)
data class CharacterMemoryCapsuleEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val sourceCharacterId: String,
    val schemaVersion: Int,
    val revivalTokenHash: String,
    val encryptedRevivalToken: String,
    val encryptedPayload: String,
    val status: String = "available",
    val createdAt: Long,
    val deletedAt: Long,
    val restoredAt: Long? = null,
    val restoredCharacterId: String? = null,
    val exportedAt: Long? = null
)
