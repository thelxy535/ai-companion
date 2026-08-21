package com.companion.cc.domain.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 打字状态管理器
 * 持久化管理所有角色的"正在输入..."状态
 */
@Singleton
class TypingStateManager @Inject constructor() {

    // 正在回复的角色集合 (characterId)
    private val _typingCharacters = MutableStateFlow<Set<String>>(emptySet())
    val typingCharacters: StateFlow<Set<String>> = _typingCharacters.asStateFlow()

    /**
     * 标记角色开始回复
     */
    fun startTyping(characterId: String) {
        _typingCharacters.value = _typingCharacters.value + characterId
    }

    /**
     * 标记角色停止回复
     */
    fun stopTyping(characterId: String) {
        _typingCharacters.value = _typingCharacters.value - characterId
    }

    /**
     * 检查角色是否正在回复
     */
    fun isTyping(characterId: String): Boolean {
        return characterId in _typingCharacters.value
    }

    /**
     * 清除所有打字状态
     */
    fun clearAll() {
        _typingCharacters.value = emptySet()
    }
}
