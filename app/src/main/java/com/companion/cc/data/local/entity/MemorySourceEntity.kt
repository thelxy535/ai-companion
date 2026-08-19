package com.companion.cc.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memory_sources",
    indices = [Index(value = ["scopeKey", "occurredAt"]), Index(value = ["scopeKey", "contentHash"], unique = true)]
)
data class MemorySourceEntity(
    @PrimaryKey val id: String,
    val scopeKey: String,
    val messageId: String?,
    val contentSnapshot: String,
    val sourceType: String,
    val occurredAt: Long,
    val contentHash: String,
    val createdAt: Long
)
