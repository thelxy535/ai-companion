package com.companion.cc.domain.model

/**
 * 伴侣人格配置（简化版）
 * 用于动态注册自定义角色到 PersonalityManager
 */
data class CompanionPersonality(
    val id: String,
    val name: String,
    val greeting: String,
    val systemPrompt: String,
    val traits: List<String>,
    val responseStyle: String,
    val apiParameters: Map<String, Any>
)
