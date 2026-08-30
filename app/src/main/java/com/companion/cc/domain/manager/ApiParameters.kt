package com.companion.cc.domain.manager

/**
 * API参数
 * 用于配置 AI 模型的生成参数
 */
data class ApiParameters(
    val temperature: Double,
    val topP: Double,
    val maxTokens: Int,
    val frequencyPenalty: Double,
    val presencePenalty: Double
)
