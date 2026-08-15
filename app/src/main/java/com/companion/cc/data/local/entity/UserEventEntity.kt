package com.companion.cc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.companion.cc.domain.model.EventType
import java.time.LocalDateTime

/**
 * 用户事件实体
 *
 * 记录用户的行为事件
 */
@Entity(tableName = "user_events")
data class UserEventEntity(
    @PrimaryKey
    val id: String,

    val userId: String,              // 用户ID
    val companionId: String,         // 伴侣ID
    val eventType: EventType,        // 事件类型
    val timestamp: LocalDateTime,    // 事件时间戳
    val metadata: String = "{}"      // 元数据（JSON格式）
)
