package com.companion.cc.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "memory_versions",
    primaryKeys = ["nodeId", "version"],
    foreignKeys = [ForeignKey(entity = MemoryNodeEntity::class, parentColumns = ["id"], childColumns = ["nodeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("nodeId")]
)
data class MemoryVersionEntity(
    val nodeId: String,
    val version: Int,
    val kind: String,
    val title: String,
    val content: String,
    val importance: Int,
    val confidence: Double,
    val validFrom: Long,
    val validUntil: Long?,
    val changeReason: String,
    val actor: String,
    val createdAt: Long
)
