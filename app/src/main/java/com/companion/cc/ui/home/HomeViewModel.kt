package com.companion.cc.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.domain.repository.MessageRepository
import com.companion.cc.domain.repository.CustomCharacterRepository
import com.companion.cc.domain.manager.OnlineStatusManager
import com.companion.cc.domain.model.CustomCharacter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Home 页面 ViewModel
 *
 * 用于获取最后消息时间，实现动态排序
 * 支持加载和显示自定义角色
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    val messageRepository: MessageRepository,
    private val settingsManager: SettingsManager,
    val onlineStatusManager: OnlineStatusManager,
    private val customCharacterRepository: CustomCharacterRepository
) : ViewModel() {

    // 自定义角色列表
    private val _customCharacters = MutableStateFlow<List<CustomCharacter>>(emptyList())
    val customCharacters: StateFlow<List<CustomCharacter>> = _customCharacters.asStateFlow()

    init {
        loadCustomCharacters()
    }

    /**
     * 加载自定义角色
     */
    private fun loadCustomCharacters() {
        viewModelScope.launch {
            android.util.Log.d("HomeViewModel", "开始加载自定义角色")
            settingsManager.userIdFlow.collect { userId ->
                android.util.Log.d("HomeViewModel", "用户ID: $userId")
                customCharacterRepository.getCharactersByUser(userId).collect { characters ->
                    android.util.Log.d("HomeViewModel", "加载到 ${characters.size} 个自定义角色")
                    characters.forEach { char ->
                        android.util.Log.d("HomeViewModel", "角色: ${char.name} (${char.id})")
                    }
                    _customCharacters.value = characters
                }
            }
        }
    }

    /**
     * 获取当前用户ID
     */
    suspend fun getUserId(): String {
        return settingsManager.userIdFlow.first()
    }
}
