package com.companion.cc.domain.model

/**
 * 聊天角色统一模型
 * 封装内置角色和自定义角色
 */
sealed class ChatCharacter {
    abstract val id: String
    abstract val name: String
    abstract val avatar: String?
    abstract val description: String

    /**
     * 内置角色
     */
    data class BuiltIn(
        override val id: String,
        override val name: String,
        override val avatar: String?,
        override val description: String
    ) : ChatCharacter()

    /**
     * 自定义角色
     */
    data class Custom(
        override val id: String,
        override val name: String,
        override val avatar: String?,
        override val description: String,
        val personality: String,
        val userId: String
    ) : ChatCharacter()

    /**
     * 判断是否为内置角色
     */
    fun isBuiltIn(): Boolean = this is BuiltIn

    /**
     * 判断是否为自定义角色
     */
    fun isCustom(): Boolean = this is Custom
}
