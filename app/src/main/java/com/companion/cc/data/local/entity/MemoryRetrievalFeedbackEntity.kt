package com.companion.cc.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(tableName = "memory_retrieval_feedback", primaryKeys = ["traceId", "nodeId"], indices = [Index("nodeId")])
data class MemoryRetrievalFeedbackEntity(
    val traceId: String,
    val nodeId: String,
    val feedback: String,
    val note: String = "",
    val createdAt: Long
)
