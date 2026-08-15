package com.companion.cc.data.remote.model

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerializedName("max_tokens")
    val maxTokens: Int = 300,
    val temperature: Float = 0.7f,
    @SerializedName("top_p")
    val topP: Float? = 0.8f,
    @SerializedName("frequency_penalty")
    val frequencyPenalty: Float? = 0.4f,
    @SerializedName("presence_penalty")
    val presencePenalty: Float? = 0.15f,
    val stream: Boolean = false  // 流式响应开关
)

data class ChatMessage(
    val role: String,
    val content: String
)
