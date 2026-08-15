package com.companion.cc.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.domain.manager.CharacterCustomizationManager
import com.companion.cc.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 角色自定义 ViewModel
 */
@HiltViewModel
class CharacterCustomizationViewModel @Inject constructor(
    private val characterManager: CharacterCustomizationManager
) : ViewModel() {

    // 用户角色列表
    private val _characters = MutableStateFlow<List<CustomCharacter>>(emptyList())
    val characters: StateFlow<List<CustomCharacter>> = _characters.asStateFlow()

    // 当前编辑的角色
    private val _currentCharacter = MutableStateFlow<CustomCharacter?>(null)
    val currentCharacter: StateFlow<CustomCharacter?> = _currentCharacter.asStateFlow()

    // 表单字段
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _backstory = MutableStateFlow("")
    val backstory: StateFlow<String> = _backstory.asStateFlow()

    private val _greetingMessage = MutableStateFlow("")
    val greetingMessage: StateFlow<String> = _greetingMessage.asStateFlow()

    private val _personality = MutableStateFlow(PersonalityTraits.default())
    val personality: StateFlow<PersonalityTraits> = _personality.asStateFlow()

    private val _behaviorRules = MutableStateFlow(BehaviorRules.default())
    val behaviorRules: StateFlow<BehaviorRules> = _behaviorRules.asStateFlow()

    private val _voiceConfig = MutableStateFlow(VoiceConfig.default())
    val voiceConfig: StateFlow<VoiceConfig> = _voiceConfig.asStateFlow()

    private val _exampleDialogues = MutableStateFlow<List<ExampleDialogue>>(emptyList())
    val exampleDialogues: StateFlow<List<ExampleDialogue>> = _exampleDialogues.asStateFlow()

    // UI 状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadCharacters()
    }

    /**
     * 加载用户的所有角色
     */
    fun loadCharacters(userId: String = "default") {
        viewModelScope.launch {
            characterManager.getUserCharacters(userId).collect { list ->
                _characters.value = list
            }
        }
    }

    /**
     * 开始创建新角色
     */
    fun startCreateCharacter() {
        resetForm()
        _currentCharacter.value = null
    }

    /**
     * 加载角色进行编辑
     */
    fun loadCharacterForEdit(characterId: String) {
        viewModelScope.launch {
            try {
                val character = characterManager.getCharacter(characterId)
                if (character != null) {
                    _currentCharacter.value = character
                    _name.value = character.name
                    _description.value = character.description
                    _backstory.value = character.backstory
                    _greetingMessage.value = character.greetingMessage
                    _personality.value = character.personality
                    _behaviorRules.value = character.behaviorRules ?: BehaviorRules.default()
                    _voiceConfig.value = character.voiceConfig ?: VoiceConfig.default()
                    _exampleDialogues.value = character.exampleDialogues
                }
            } catch (e: Exception) {
                _errorMessage.value = "加载角色失败: ${e.message}"
            }
        }
    }

    /**
     * 保存角色
     */
    fun saveCharacter(userId: String = "default") {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                android.util.Log.d("CharacterVM", "开始保存角色")
                val current = _currentCharacter.value
                if (current != null) {
                    // 更新现有角色
                    android.util.Log.d("CharacterVM", "更新现有角色: ${current.id}")
                    val updated = current.copy(
                        name = _name.value,
                        description = _description.value,
                        backstory = _backstory.value,
                        greetingMessage = _greetingMessage.value,
                        personality = _personality.value,
                        behaviorRules = _behaviorRules.value,
                        voiceConfig = _voiceConfig.value,
                        exampleDialogues = _exampleDialogues.value
                    )
                    characterManager.updateCharacter(updated)
                    android.util.Log.d("CharacterVM", "更新成功")
                } else {
                    // 创建新角色
                    android.util.Log.d("CharacterVM", "创建新角色: name=${_name.value}")
                    characterManager.createCharacter(
                        userId = userId,
                        name = _name.value,
                        description = _description.value,
                        personality = _personality.value,
                        backstory = _backstory.value,
                        greetingMessage = _greetingMessage.value,
                        exampleDialogues = _exampleDialogues.value,
                        voiceConfig = _voiceConfig.value,
                        behaviorRules = _behaviorRules.value
                    )
                    android.util.Log.d("CharacterVM", "创建成功")
                }
                // 重新加载角色列表
                loadCharacters(userId)
                android.util.Log.d("CharacterVM", "保存完成，角色列表已刷新")
            } catch (e: Exception) {
                android.util.Log.e("CharacterVM", "保存失败", e)
                _errorMessage.value = "保存失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 删除角色
     */
    fun deleteCharacter(characterId: String) {
        viewModelScope.launch {
            try {
                characterManager.deleteCharacter(characterId)
            } catch (e: Exception) {
                _errorMessage.value = "删除失败: ${e.message}"
            }
        }
    }

    // 表单更新函数
    fun updateName(value: String) {
        _name.value = value
    }

    fun updateDescription(value: String) {
        _description.value = value
    }

    fun updateBackstory(value: String) {
        _backstory.value = value
    }

    fun updateGreetingMessage(value: String) {
        _greetingMessage.value = value
    }

    fun updatePersonalityTrait(trait: String, value: Float) {
        val current = _personality.value
        _personality.value = when (trait) {
            "openness" -> current.copy(openness = value)
            "conscientiousness" -> current.copy(conscientiousness = value)
            "extraversion" -> current.copy(extraversion = value)
            "agreeableness" -> current.copy(agreeableness = value)
            "neuroticism" -> current.copy(neuroticism = value)
            else -> current
        }
    }

    fun updateResponseStyle(style: ResponseStyle) {
        _behaviorRules.value = _behaviorRules.value.copy(responseStyle = style)
    }

    fun updateEmojiFrequency(frequency: EmojiFrequency) {
        _behaviorRules.value = _behaviorRules.value.copy(emojiFrequency = frequency)
    }

    fun updateFormalityLevel(level: FormalityLevel) {
        _behaviorRules.value = _behaviorRules.value.copy(formalityLevel = level)
    }

    fun updateVoicePitch(pitch: Float) {
        _voiceConfig.value = _voiceConfig.value.copy(pitch = pitch)
    }

    fun updateVoiceSpeed(speed: Float) {
        _voiceConfig.value = _voiceConfig.value.copy(speed = speed)
    }

    fun addExampleDialogue(user: String, assistant: String) {
        _exampleDialogues.value = _exampleDialogues.value + ExampleDialogue(user, assistant)
    }

    fun removeExampleDialogue(index: Int) {
        _exampleDialogues.value = _exampleDialogues.value.filterIndexed { i, _ -> i != index }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * 重置表单
     */
    private fun resetForm() {
        _name.value = ""
        _description.value = ""
        _backstory.value = ""
        _greetingMessage.value = "你好，很高兴见到你！"
        _personality.value = PersonalityTraits.default()
        _behaviorRules.value = BehaviorRules.default()
        _voiceConfig.value = VoiceConfig.default()
        _exampleDialogues.value = emptyList()
        _currentCharacter.value = null
    }

    /**
     * 验证表单
     */
    fun isFormValid(): Boolean {
        return _name.value.isNotBlank() &&
                _description.value.isNotBlank() &&
                _backstory.value.isNotBlank()
    }
}
