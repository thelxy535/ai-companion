package com.companion.cc.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memory_nodes",
    indices = [Index(value = ["scopeKey", "status", "updatedAt"]), Index(value = ["scopeKey", "kind", "createdAt"])]
)
data class MemoryNodeEntity(
    @PrimaryKey val id: String,
    val scopeKey: String,
    val kind: String,
    val subjectRole: String,
    val subjectKey: String,
    val title: String,
    val content: String,
    val importance: Int = 50,
    val confidence: Double = 0.5,
    val validFrom: Long,
    val validUntil: Long? = null,
    val status: String = "active",
    val createdAt: Long,
    val updatedAt: Long,
    val currentVersion: Int = 1
)
