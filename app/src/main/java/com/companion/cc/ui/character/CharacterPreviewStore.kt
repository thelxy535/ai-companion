package com.companion.cc.ui.character

import com.companion.cc.domain.model.CustomCharacter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 试聊草稿的中转站（进程单例）：角色向导 → 试聊屏 传递未入库的角色草稿。
 * 试聊全程不落库；向导点「完成」才走正式保存。
 */
object CharacterPreviewStore {
    private val _draft = MutableStateFlow<CustomCharacter?>(null)
    val draft: StateFlow<CustomCharacter?> = _draft.asStateFlow()

    fun put(value: CustomCharacter) {
        _draft.value = value
    }

    fun clear() {
        _draft.value = null
    }
}
