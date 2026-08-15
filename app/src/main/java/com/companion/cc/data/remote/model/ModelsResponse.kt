package com.companion.cc.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 模型列表响应
 */
@Serializable
data class ModelsResponse(
    val data: List<ModelInfo>,
    @SerialName("object") val objectType: String = "list"
)

@Serializable
data class ModelInfo(
    val id: String,
    @SerialName("object") val objectType: String = "model",
    val created: Long? = null,
    val owned_by: String? = null
)
