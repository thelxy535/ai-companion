package com.companion.cc.domain.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 在线状态管理器
 *
 * 单例模式，跨 ViewModel 共享在线状态
 * 解决导航作用域导致的状态不同步问题
 */
@Singleton
class OnlineStatusManager @Inject constructor() {

    private val _onlineCompanions = MutableStateFlow<Set<String>>(emptySet())
    val onlineCompanions: StateFlow<Set<String>> = _onlineCompanions.asStateFlow()

    /**
     * 设置伴侣为在线状态
     */
    fun setOnline(companionId: String) {
        _onlineCompanions.value = _onlineCompanions.value + companionId
    }

    /**
     * 设置伴侣为离线状态
     */
    fun setOffline(companionId: String) {
        _onlineCompanions.value = _onlineCompanions.value - companionId
    }

    /**
     * 检查伴侣是否在线
     */
    fun isOnline(companionId: String): Boolean {
        return _onlineCompanions.value.contains(companionId)
    }

    /**
     * 清空所有在线状态
     */
    fun clearAll() {
        _onlineCompanions.value = emptySet()
    }
}
