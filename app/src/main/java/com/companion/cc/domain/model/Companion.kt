package com.companion.cc.domain.model

data class Companion(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val greeting: String,
    val avatarUrl: String? = null  // 头像 URL，可为空使用默认 emoji
)

val companions = listOf(
    Companion(
        id = "xiaocan",
        name = "小璨",
        emoji = "💕",
        description = "温暖、善解人意的陪伴者，总能理解你的感受",
        greeting = "你好呀！我是小璨，很高兴见到你~ 💕",
        avatarUrl = null  // 默认使用 emoji
    ),
    Companion(
        id = "muse",
        name = "缪斯",
        emoji = "🎭",
        description = "冷静、理性的思考者，帮你分析问题的本质",
        greeting = "你好，我是缪斯。有什么想聊的吗？",
        avatarUrl = null  // 默认使用 emoji
    )
)
