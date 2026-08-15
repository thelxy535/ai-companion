package com.companion.cc.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 标签实体
 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    val name: String,  // 标签名称

    val color: String = "#2196F3",  // 标签颜色（十六进制）

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 消息-标签关联表
 */
@Entity(
    tableName = "message_tags",
    primaryKeys = ["message_id", "tag_id"]
)
data class MessageTagEntity(
    @ColumnInfo(name = "message_id")
    val messageId: String,

    @ColumnInfo(name = "tag_id")
    val tagId: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
