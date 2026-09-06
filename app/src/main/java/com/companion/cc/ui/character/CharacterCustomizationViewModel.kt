package com.companion.cc.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.domain.character.CharacterCatalog
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.manager.CharacterCustomizationManager
import com.companion.cc.domain.model.*
import com.companion.cc.data.character.CharacterExportFormat
import com.companion.cc.data.character.CharacterTransferCodec
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
    private val characterCatalog: CharacterCatalog,
    private val settingsManager: com.companion.cc.data.local.SettingsManager
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

    // V9PM 头像一致性：设置页/聊天页自定义的伴侣头像覆盖 Map（全 App 统一展示）
    val avatarOverrides: StateFlow<Map<String, String>> = settingsManager.companionAvatarOverridesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

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

    private val _pendingImportedCharacter = MutableStateFlow<CustomCharacter?>(null)
    val pendingImportedCharacter: StateFlow<CustomCharacter?> = _pendingImportedCharacter.asStateFlow()
    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()
    private val _pendingCardExport = MutableStateFlow<Pair<String, String>?>(null)
    val pendingCardExport: StateFlow<Pair<String, String>?> = _pendingCardExport.asStateFlow()

    private val _scenario = MutableStateFlow("")
    val scenario: StateFlow<String> = _scenario.asStateFlow()
    private val _alternateGreetings = MutableStateFlow<List<String>>(emptyList())
    val alternateGreetings: StateFlow<List<String>> = _alternateGreetings.asStateFlow()
    private val _creatorNotes = MutableStateFlow("")
    val creatorNotes: StateFlow<String> = _creatorNotes.asStateFlow()
    private val _creator = MutableStateFlow("")
    val creator: StateFlow<String> = _creator.asStateFlow()
    private val _characterVersion = MutableStateFlow("1.0")
    val characterVersion: StateFlow<String> = _characterVersion.asStateFlow()
    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()
    private val _systemPromptOverride = MutableStateFlow("")
    val systemPromptOverride: StateFlow<String> = _systemPromptOverride.asStateFlow()
    private val _postHistoryInstructions = MutableStateFlow("")
    val postHistoryInstructions: StateFlow<String> = _postHistoryInstructions.asStateFlow()
    private val _characterBook = MutableStateFlow<List<com.companion.cc.domain.model.CharacterBookEntry>>(emptyList())
    val characterBook: StateFlow<List<com.companion.cc.domain.model.CharacterBookEntry>> = _characterBook.asStateFlow()

    /**
     * 加载角色进行编辑
     */
    /** V9PM：组装试聊草稿（不入库；userId 用占位，试聊屏自行管理会话） */
    fun buildDraft(): CustomCharacter? {
        val name = _name.value.trim()
        if (name.isEmpty()) return null
        return CustomCharacter(
            id = editingCharacterId ?: "preview_${System.currentTimeMillis()}",
            userId = "draft",
            name = name,
            avatar = null,
            description = _description.value.trim(),
            personality = _personality.value,
            backstory = _backstory.value.trim(),
            greetingMessage = _greetingMessage.value,
            exampleDialogues = _exampleDialogues.value,
            voiceConfig = _voiceConfig.value,
            behaviorRules = _behaviorRules.value,
        )
    }

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
            _scenario.value = character.scenario
            _alternateGreetings.value = character.alternateGreetings
            _creatorNotes.value = character.creatorNotes
            _creator.value = character.creator
            _characterVersion.value = character.characterVersion
            _tags.value = character.tags
            _systemPromptOverride.value = character.systemPromptOverride
            _postHistoryInstructions.value = character.postHistoryInstructions
            _characterBook.value = character.characterBook
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
                            scenario = _scenario.value,
                            alternateGreetings = _alternateGreetings.value,
                            creatorNotes = _creatorNotes.value,
                            creator = _creator.value,
                            characterVersion = _characterVersion.value,
                            tags = _tags.value,
                            systemPromptOverride = _systemPromptOverride.value,
                            postHistoryInstructions = _postHistoryInstructions.value,
                            characterBook = _characterBook.value,
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
                    scenario = _scenario.value,
                    alternateGreetings = _alternateGreetings.value,
                    creatorNotes = _creatorNotes.value,
                    creator = _creator.value,
                    characterVersion = _characterVersion.value,
                    tags = _tags.value,
                    systemPromptOverride = _systemPromptOverride.value,
                    postHistoryInstructions = _postHistoryInstructions.value,
                    characterBook = _characterBook.value,
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

    fun importCharacterCard(json: String) {
        viewModelScope.launch {
            _importError.value = null
            try {
                val userId = currentUserProvider.requireUserId()
                _pendingImportedCharacter.value = CharacterTransferCodec.parse(json, userId).getOrThrow()
            } catch (error: Exception) {
                _importError.value = error.message ?: "角色卡解析失败"
            }
        }
    }

    fun clearPendingImport() {
        _pendingImportedCharacter.value = null
        _importError.value = null
    }

    fun setImportError(message: String) {
        _importError.value = message
    }

    fun confirmImportedCharacter() {
        val imported = _pendingImportedCharacter.value ?: return
        viewModelScope.launch {
            try {
                characterManager.saveImportedCharacter(imported)
                clearPendingImport()
            } catch (error: Exception) {
                _importError.value = error.message ?: "角色卡保存失败"
            }
        }
    }

    fun clearPendingCardExport() {
        _pendingCardExport.value = null
    }

    fun exportCharacterCard(characterId: String, format: CharacterExportFormat = CharacterExportFormat.STANDARD_JSON) {
        viewModelScope.launch {
            try {
                val userId = currentUserProvider.requireUserId()
                val character = characterManager.getCharacter(userId, characterId)
                    ?: error("找不到要导出的角色")
                _pendingCardExport.value = CharacterTransferCodec.fileName(character, format) to
                    CharacterTransferCodec.serialize(character, format)
            } catch (error: Exception) {
                _importError.value = error.message ?: "角色卡导出失败"
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

    fun updateScenario(value: String) { _scenario.value = value }
    fun updateAlternateGreetings(value: List<String>) { _alternateGreetings.value = value }
    fun updateCreatorNotes(value: String) { _creatorNotes.value = value }
    fun updateCreator(value: String) { _creator.value = value }
    fun updateCharacterVersion(value: String) { _characterVersion.value = value }
    fun updateTags(value: List<String>) { _tags.value = value }
    fun updateSystemPromptOverride(value: String) { _systemPromptOverride.value = value }
    fun updatePostHistoryInstructions(value: String) { _postHistoryInstructions.value = value }

    fun updateCharacterBook(value: List<com.companion.cc.domain.model.CharacterBookEntry>) {
        _characterBook.value = value
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
        _scenario.value = ""
        _alternateGreetings.value = emptyList()
        _creatorNotes.value = ""
        _creator.value = ""
        _characterVersion.value = "1.0"
        _tags.value = emptyList()
        _systemPromptOverride.value = ""
        _postHistoryInstructions.value = ""
        _characterBook.value = emptyList()
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
                    _exampleDialogues.value != existing.exampleDialogues ||
                    _scenario.value != existing.scenario ||
                    _alternateGreetings.value != existing.alternateGreetings ||
                    _creatorNotes.value != existing.creatorNotes ||
                    _creator.value != existing.creator ||
                    _characterVersion.value != existing.characterVersion ||
                    _tags.value != existing.tags ||
                    _systemPromptOverride.value != existing.systemPromptOverride ||
                    _postHistoryInstructions.value != existing.postHistoryInstructions ||
                    _characterBook.value != existing.characterBook
        } else {
            _name.value.isNotBlank() ||
                    _description.value.isNotBlank() ||
                    _backstory.value.isNotBlank() ||
                    _greetingMessage.value != "你好，很高兴见到你！" ||
                    _personality.value != PersonalityTraits.default() ||
                    _behaviorRules.value != BehaviorRules.default() ||
                    _voiceConfig.value != VoiceConfig.default() ||
                    _exampleDialogues.value.isNotEmpty() ||
                    _scenario.value.isNotBlank() ||
                    _alternateGreetings.value.isNotEmpty() ||
                    _creatorNotes.value.isNotBlank() ||
                    _creator.value.isNotBlank() ||
                    _characterVersion.value != "1.0" ||
                    _tags.value.isNotEmpty() ||
                    _systemPromptOverride.value.isNotBlank() ||
                    _postHistoryInstructions.value.isNotBlank() ||
                    _characterBook.value.isNotEmpty()
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
