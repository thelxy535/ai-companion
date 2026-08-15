package com.companion.cc.data.remote.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 流式响应数据块
 */
@Serializable
data class ChatStreamChunk(
    val id: String,
    @SerializedName("object") @SerialName("object") val objectType: String = "chat.completion.chunk",
    val created: Long,
    val model: String,
    val choices: List<StreamChoice>
)

@Serializable
data class StreamChoice(
    val index: Int,
    val delta: StreamDelta,
    @SerializedName("finish_reason") @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class StreamDelta(
    val role: String? = null,
    val content: String? = null
)
