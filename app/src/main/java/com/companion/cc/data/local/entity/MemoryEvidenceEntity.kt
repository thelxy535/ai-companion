package com.companion.cc.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "memory_evidence",
    primaryKeys = ["nodeId", "sourceId"],
    foreignKeys = [
        ForeignKey(entity = MemoryNodeEntity::class, parentColumns = ["id"], childColumns = ["nodeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MemorySourceEntity::class, parentColumns = ["id"], childColumns = ["sourceId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("sourceId")]
)
data class MemoryEvidenceEntity(
    val nodeId: String,
    val sourceId: String,
    val evidenceRole: String = "support",
    val confidence: Double = 0.5,
    val summarySnapshot: String = "",
    val createdAt: Long
)
