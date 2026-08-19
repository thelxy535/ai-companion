package com.companion.cc.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "memory_relations",
    primaryKeys = ["fromNodeId", "toNodeId", "relationType"],
    foreignKeys = [
        ForeignKey(entity = MemoryNodeEntity::class, parentColumns = ["id"], childColumns = ["fromNodeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MemoryNodeEntity::class, parentColumns = ["id"], childColumns = ["toNodeId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("toNodeId"), Index(value = ["scopeKey", "status"])]
)
data class MemoryRelationEntity(
    val fromNodeId: String,
    val toNodeId: String,
    val relationType: String,
    val scopeKey: String,
    val weight: Double = 0.5,
    val status: String = "proposed",
    val evidenceJson: String = "[]",
    val createdAt: Long,
    val updatedAt: Long
)
