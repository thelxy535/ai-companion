package com.companion.cc.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ExportData(
    val version: String = "1.0",
    val exportDate: String,
    val userId: String,
    val messages: List<ExportMessage>,
    val settings: ExportSettings
)

@Serializable
data class ExportMessage(
    val id: String,
    val userId: String,
    val companionId: String = "xiaocan",
    val role: String,
    val content: String,
    val timestamp: Long,
    val emotion: String? = null,
    val mentionedOther: Boolean = false,
    val isDualConversation: Boolean = false,
    val replyToId: String? = null
)

@Serializable
data class ExportSettings(
    val apiKey: String = "",
    val baseURL: String = "",
    val model: String = ""
)
