package com.companion.cc.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedules",
    indices = [Index(value = ["userId", "characterId", "status", "nextRunAt"])]
)
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val characterId: String,
    val prompt: String,
    val recurrence: String,
    val nextRunAt: Long,
    val status: String = "ACTIVE",
    val lastError: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
