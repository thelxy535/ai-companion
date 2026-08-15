package com.companion.cc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * 交互时间记录实体
 *
 * 记录每次用户与AI的交互时间，用于时间上下文分析
 */
@Entity(tableName = "interaction_times")
data class InteractionTimeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: String,              // 用户ID
    val companionId: String,         // 伴侣ID
    val timestamp: LocalDateTime,    // 交互时间戳
    val messageCount: Int = 1,       // 本次交互的消息数（用于统计）
    val sessionId: String            // 会话ID（用于后台反思）
)
