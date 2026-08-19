package com.companion.cc.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "memory_retrieval_traces", indices = [Index(value = ["scopeKey", "createdAt"])])
data class MemoryRetrievalTraceEntity(
    @PrimaryKey val id: String,
    val scopeKey: String,
    val query: String,
    val selectedNodeIdsJson: String,
    val explanationJson: String,
    val createdAt: Long,
    val durationMs: Long
)
