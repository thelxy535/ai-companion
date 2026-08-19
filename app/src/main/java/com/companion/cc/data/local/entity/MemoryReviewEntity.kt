package com.companion.cc.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memory_reviews",
    indices = [Index(value = ["scopeKey", "status", "createdAt"]), Index(value = ["scopeKey", "proposalHash"], unique = true)]
)
data class MemoryReviewEntity(
    @PrimaryKey val id: String,
    val scopeKey: String,
    val kind: String,
    val title: String,
    val content: String,
    val confidence: Double,
    val status: String = "pending",
    val sourceIdsJson: String = "[]",
    val proposalHash: String,
    val createdAt: Long,
    val resolvedAt: Long? = null,
    val resolutionNote: String = ""
)
