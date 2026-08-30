package com.companion.cc.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.domain.character.CharacterCatalog
import com.companion.cc.domain.identity.CurrentUserProvider
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
    private val characterManager: CharacterCustomizationManager,
    private val currentUserProvider: CurrentUserProvider,
    private val characterCatalog: CharacterCatalog
) : ViewModel() {

    // 当前编辑的角色
    private val _currentCharacter = MutableStateFlow<CustomCharacter?>(null)
    val currentCharacter: StateFlow<CustomCharacter?> = _currentCharacter.asStateFlow()

    // 保存状态
    private val _saveState = MutableStateFlow<CharacterSaveState>(CharacterSaveState.Idle)
    val saveState: StateFlow<CharacterSaveState> = _saveState.asStateFlow()

    private val saveGate = SingleFlightGate()
    private val deleteGate = SingleFlightGate()
    private var editingCharacterId: String? = null

    // 所有角色（来自 CharacterCatalog）
    val characters: StateFlow<List<ChatCharacter>> = characterCatalog.observeCharacters()
        // V7：Lazily 常驻内存——首次进角色页后数据驻留，再次进入无冷加载卡顿
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 表单字段
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _backstory = MutableStateFlow("")
    val backstory: StateFlow<String> = _backstory.asStateFlow()

    private val _greetingMessage = MutableStateFlow("你好，很高兴见到你！")
    val greetingMessage: StateFlow<String> = _greetingMessage.asStateFlow()

    private val _personality = MutableStateFlow(PersonalityTraits.default())
    val personality: StateFlow<PersonalityTraits> = _personality.asStateFlow()

    private val _behaviorRules = MutableStateFlow(BehaviorRules.default())
    val behaviorRules: StateFlow<BehaviorRules> = _behaviorRules.asStateFlow()

    private val _voiceConfig = MutableStateFlow(VoiceConfig.default())
    val voiceConfig: StateFlow<VoiceConfig> = _voiceConfig.asStateFlow()

    private val _exampleDialogues = MutableStateFlow<List<ExampleDialogue>>(emptyList())
    val exampleDialogues: StateFlow<List<ExampleDialogue>> = _exampleDialogues.asStateFlow()

    /**
     * 加载角色进行编辑
     */
    fun loadCharacterForEdit(characterId: String) {
        editingCharacterId = characterId
        viewModelScope.launch {
            try {
                val userId = currentUserProvider.requireUserId()
                val character = characterManager.getCharacter(userId, characterId)
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
                } else {
                    _saveState.value = CharacterSaveState.Failure("找不到要编辑的角色")
                }
            } catch (e: Exception) {
                _saveState.value = CharacterSaveState.Failure("加载角色失败: ${e.message}")
            }
        }
    }

    /**
     * 开始新角色创建
     */
    fun startNewCharacter() {
        editingCharacterId = null
        resetForm()
        _currentCharacter.value = null
    }

    /**
     * 保存角色
     */
    fun saveCharacter() {
        if (!canSaveCharacter(editingCharacterId, _currentCharacter.value)) {
            _saveState.value = CharacterSaveState.Failure("找不到要编辑的角色，无法保存")
            return
        }
        if (!saveGate.tryAcquire()) {
            return // 防止重复保存
        }
        _saveState.value = CharacterSaveState.Saving

        viewModelScope.launch {
            _saveState.value = try {
                val userId = currentUserProvider.requireUserId()
                val saved = _currentCharacter.value?.let { existing ->
                    characterManager.updateCharacter(
                        existing.copy(
                            name = _name.value,
                            description = _description.value,
                            personality = _personality.value,
                            backstory = _backstory.value,
                            greetingMessage = _greetingMessage.value,
                            behaviorRules = _behaviorRules.value,
                            voiceConfig = _voiceConfig.value,
                            exampleDialogues = _exampleDialogues.value,
                        )
                    )
                } ?: characterManager.createCharacter(
                    userId = userId,
                    name = _name.value,
                    description = _description.value,
                    personality = _personality.value,
                    backstory = _backstory.value,
                    greetingMessage = _greetingMessage.value,
                    exampleDialogues = _exampleDialogues.value,
                    voiceConfig = _voiceConfig.value,
                    behaviorRules = _behaviorRules.value,
                )
                CharacterSaveState.Success(saved.id)
            } catch (error: Exception) {
                CharacterSaveState.Failure(error.message ?: "保存角色失败")
            } finally {
                saveGate.release()
            }
        }
    }

    /**
     * 消费保存成功结果（UI 调用以重置状态）
     */
    fun consumeSaveResult() {
        if (_saveState.value is CharacterSaveState.Success) {
            _saveState.value = CharacterSaveState.Idle
        }
    }

    /**
     * 删除角色
     */
    fun deleteCharacter(characterId: String) {
        if (!deleteGate.tryAcquire()) {
            return
        }

        viewModelScope.launch {
            try {
                val userId = currentUserProvider.requireUserId()
                characterManager.deleteCharacter(userId, characterId)
            } catch (e: Exception) {
                _saveState.value = CharacterSaveState.Failure("删除失败: ${e.message}")
            } finally {
                deleteGate.release()
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

    fun updatePersonality(value: PersonalityTraits) {
        _personality.value = value
    }

    fun updatePersonalityTrait(trait: String, value: Float) {
        _personality.value = when (trait) {
            "openness" -> _personality.value.copy(openness = value)
            "conscientiousness" -> _personality.value.copy(conscientiousness = value)
            "extraversion" -> _personality.value.copy(extraversion = value)
            "agreeableness" -> _personality.value.copy(agreeableness = value)
            "neuroticism" -> _personality.value.copy(neuroticism = value)
            else -> _personality.value
        }
    }

    fun updateBehaviorRules(value: BehaviorRules) {
        _behaviorRules.value = value
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

    fun updateVoiceConfig(value: VoiceConfig) {
        _voiceConfig.value = value
    }

    fun updateExampleDialogues(value: List<ExampleDialogue>) {
        _exampleDialogues.value = value
    }

    fun addExampleDialogue(dialogue: ExampleDialogue) {
        _exampleDialogues.value = _exampleDialogues.value + dialogue
    }

    fun removeExampleDialogue(index: Int) {
        _exampleDialogues.value = _exampleDialogues.value.filterIndexed { i, _ -> i != index }
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
     * 判断当前表单是否有未保存修改。
     */
    fun hasUnsavedChanges(): Boolean {
        val existing = _currentCharacter.value
        if (editingCharacterId != null && existing == null) {
            return false
        }

        return if (existing != null) {
            _name.value != existing.name ||
                    _description.value != existing.description ||
                    _backstory.value != existing.backstory ||
                    _greetingMessage.value != existing.greetingMessage ||
                    _personality.value != existing.personality ||
                    _behaviorRules.value != (existing.behaviorRules ?: BehaviorRules.default()) ||
                    _voiceConfig.value != (existing.voiceConfig ?: VoiceConfig.default()) ||
                    _exampleDialogues.value != existing.exampleDialogues
        } else {
            _name.value.isNotBlank() ||
                    _description.value.isNotBlank() ||
                    _backstory.value.isNotBlank() ||
                    _greetingMessage.value != "你好，很高兴见到你！" ||
                    _personality.value != PersonalityTraits.default() ||
                    _behaviorRules.value != BehaviorRules.default() ||
                    _voiceConfig.value != VoiceConfig.default() ||
                    _exampleDialogues.value.isNotEmpty()
        }
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
