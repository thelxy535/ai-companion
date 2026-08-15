package com.companion.cc.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "companion_id")
    val companionId: String,

    val role: String, // "user" or "assistant"

    val content: String,

    val timestamp: Long,

    // Metadata
    val emotion: String? = null,

    @ColumnInfo(name = "mentioned_other")
    val mentionedOther: Boolean = false,

    @ColumnInfo(name = "is_dual_conversation")
    val isDualConversation: Boolean = false,

    @ColumnInfo(name = "reply_to_id")
    val replyToId: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    val importance: Int = 50, // 0-100, 重要程度

    val action: String? = null, // 动作描述

    @ColumnInfo(name = "is_favorited")
    val isFavorited: Boolean = false, // 是否收藏

    // 视觉感知扩展（多模态支持）
    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,        // 图片 URL

    @ColumnInfo(name = "image_analysis")
    val imageAnalysis: String? = null    // 视觉理解结果
)
