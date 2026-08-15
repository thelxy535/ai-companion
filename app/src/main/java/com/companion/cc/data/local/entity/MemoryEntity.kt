package com.companion.cc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 记忆条目实体
 */
@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val role: String, // USER or ASSISTANT
    val content: String,
    val timestamp: Long,
    val date: String, // YYYY-MM-DD
    val importance: Int, // 0-100
    val emotion: String?,
    val topics: String // JSON array of topics
)

/**
 * 用户画像实体
 */
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val name: String?,
    val age: Int?,
    val gender: String?,
    val occupation: String?,
    val location: String?,
    val personality: String, // JSON
    val interests: String, // JSON array
    val relationships: String, // JSON array
    val importantEvents: String, // JSON array
    val habits: String, // JSON array
    val updatedAt: Long
)
