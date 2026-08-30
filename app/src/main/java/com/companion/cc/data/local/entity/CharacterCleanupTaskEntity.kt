package com.companion.cc.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "character_cleanup_tasks",
    indices = [Index(value = ["userId", "status", "updatedAt"])]
)
data class CharacterCleanupTaskEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val characterId: String,
    val avatarReference: String? = null,
    val status: String = "pending",
    val attempts: Int = 0,
    val lastError: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
