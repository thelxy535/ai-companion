package com.companion.cc.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "memory_scope_quarantine",
    primaryKeys = ["resourceType", "resourceId"],
    indices = [Index("legacyScopeKey")]
)
data class MemoryScopeQuarantineEntity(
    val resourceType: String,
    val resourceId: String,
    val legacyScopeKey: String,
    val createdAt: Long
)
