package com.companion.cc.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "reflection_jobs",
    primaryKeys = ["id"],
    indices = [
        Index(value = ["scopeKey", "status", "nextRunAt"]),
        Index(value = ["idempotencyKey"], unique = true)
    ]
)
data class ReflectionJobEntity(
    val id: String,
    val scopeKey: String,
    val trigger: String,
    val sourceCursor: String,
    val status: String = STATUS_PENDING,
    val attempts: Int = 0,
    val nextRunAt: Long,
    val leaseOwner: String? = null,
    val leaseUntil: Long? = null,
    val lastError: String? = null,
    val idempotencyKey: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_RUNNING = "RUNNING"
        const val STATUS_RETRY_WAIT = "RETRY_WAIT"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
    }
}
