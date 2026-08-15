package com.companion.cc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 自定义角色实体（数据库表）
 */
@Entity(tableName = "custom_characters")
data class CustomCharacterEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val avatar: String?,
    val description: String,
    val personality: String,           // JSON: PersonalityTraits
    val backstory: String,
    val greetingMessage: String,
    val exampleDialogues: String,      // JSON: List<ExampleDialogue>
    val voiceConfig: String?,          // JSON: VoiceConfig
    val behaviorRules: String?,        // JSON: BehaviorRules
    val isCustom: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
