package com.companion.cc.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.data.remote.vision.SelfHostedVisionProvider
import com.companion.cc.data.theme.VisualCustomizationManager
import com.companion.cc.domain.manager.AIProviderManager
import com.companion.cc.domain.manager.ApiParameters
import com.companion.cc.domain.model.AIProvider
import com.companion.cc.domain.model.BackdropTarget
import com.companion.cc.domain.model.ProviderConfig
import com.companion.cc.domain.model.VisionServiceMode
import com.companion.cc.domain.model.VisualCustomization
import com.companion.cc.domain.usecase.StreamSendMessageUseCase
import com.companion.cc.ui.theme.TactileIntensityPreference
import com.companion.cc.ui.theme.VisualEffectsPreference
import com.companion.cc.util.ErrorMessageHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val providerManager: AIProviderManager,
    private val streamSendMessageUseCase: StreamSendMessageUseCase,
    private val selfHostedVisionProvider: SelfHostedVisionProvider,
    private val visualCustomizationManager: VisualCustomizationManager
) : ViewModel() {

    val apiKey = settingsManager.apiKeyFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val baseUrl = settingsManager.baseUrlFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "https://api.siliconflow.cn/v1"
    )

    val model = settingsManager.modelFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "deepseek-ai/DeepSeek-V3"
    )

    val themeMode = settingsManager.themeModeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "system"
    )

    val materialStyle = settingsManager.materialStyleFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "GLASS"
    )

    val fontSize = settingsManager.fontSizeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "medium"
    )

    val notificationsEnabled = settingsManager.notificationsEnabledFlow.stateIn(
        viewModelScope,
        kotlinx.coroutines.flow.SharingStarted.Eagerly,
        true
    )

    val tactileIntensity = settingsManager.tactileIntensityFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TactileIntensityPreference.SYSTEM
    )

    val visualCustomization = visualCustomizationManager.customizationFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        VisualCustomization.default()
    )

    private val _visualCustomizationMessage = MutableStateFlow<String?>(null)
    val visualCustomizationMessage: StateFlow<String?> = _visualCustomizationMessage

    val userAvatar = settingsManager.userAvatarFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    // AI 伴侣头像（默认获取小璨的）
    val xiaoChanAvatar = settingsManager.getCompanionAvatarFlow("xiaocan").stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val museAvatar = settingsManager.getCompanionAvatarFlow("muse").stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    private val _currentProvider = MutableStateFlow<AIProvider?>(null)
    val currentProvider: StateFlow<AIProvider?> = _currentProvider

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels

    private val _validationMessage = MutableStateFlow<String?>(null)
    val validationMessage: StateFlow<String?> = _validationMessage

    private val _isValidating = MutableStateFlow(false)
    val isValidating: StateFlow<Boolean> = _isValidating

    private val _visionApiKeySaved = MutableStateFlow(false)
    val visionApiKeySaved: StateFlow<Boolean> = _visionApiKeySaved

    val visionServiceMode = settingsManager.visionServiceModeFlow
        .map(VisionServiceMode::fromStoredValue)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VisionServiceMode.GEMINI)

    val isSelfHostedVisionPaired = settingsManager.selfHostedDeviceTokenFlow
        .map { !it.isNullOrBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isPairingSelfHostedVision = MutableStateFlow(false)
    val isPairingSelfHostedVision: StateFlow<Boolean> = _isPairingSelfHostedVision

    private val _selfHostedVisionMessage = MutableStateFlow<String?>(null)
    val selfHostedVisionMessage: StateFlow<String?> = _selfHostedVisionMessage

    init {
        // 加载当前配置
        viewModelScope.launch {
            providerManager.getCurrentConfig()?.let { config ->
                _currentProvider.value = config.provider
                _availableModels.value = config.availableModels
            }
        }
    }

    /**
     * 验证并配置API密钥
     */
    fun validateAndConfigureApiKey(apiKey: String, customBaseUrl: String? = null) {
        viewModelScope.launch {
            _isValidating.value = true
            _validationMessage.value = null

            android.util.Log.d("SettingsViewModel", "开始验证 API Key")
            android.util.Log.d("SettingsViewModel", "API Key 长度: ${apiKey.length}")
            android.util.Log.d("SettingsViewModel", "自定义 Base URL: $customBaseUrl")

            try {
                val result = providerManager.detectAndConfigureProvider(apiKey, customBaseUrl)

                result.onSuccess { config ->
                    android.util.Log.d("EnhancedSettingsViewModel", "配置成功")
                    android.util.Log.d("EnhancedSettingsViewModel", "供应商: ${config.provider.displayName}")
                    android.util.Log.d("EnhancedSettingsViewModel", "Base URL: ${config.baseUrl}")
                    android.util.Log.d("EnhancedSettingsViewModel", "可用模型数量: ${config.availableModels.size}")
                    android.util.Log.d("EnhancedSettingsViewModel", "模型列表: ${config.availableModels.joinToString()}")

                    _currentProvider.value = config.provider
                    _availableModels.value = config.availableModels
                    _validationMessage.value = "✅ 已识别：${config.provider.displayName}\n可用模型：${config.availableModels.size}个"
                }.onFailure { error ->
                    android.util.Log.e("EnhancedSettingsViewModel", "配置失败", error)
                    _validationMessage.value = "❌ ${error.message}"
                }
            } catch (e: Exception) {
                android.util.Log.e("EnhancedSettingsViewModel", "异常", e)
                _validationMessage.value = "❌ 配置失败：${e.message}"
            } finally {
                _isValidating.value = false
            }
        }
    }

    /**
     * 切换模型
     */
    fun switchModel(modelName: String) {
        viewModelScope.launch {
            providerManager.switchModel(modelName).onFailure { error ->
                _validationMessage.value = "❌ ${error.message}"
            }
        }
    }

    /**
     * 保存视觉 API Key（用于图片理解）
     */
    fun saveVisionApiKey(visionApiKey: String) {
        viewModelScope.launch {
            settingsManager.saveVisionApiKey(visionApiKey)
            _visionApiKeySaved.value = true
            android.util.Log.d("SettingsViewModel", "视觉 API Key 已保存")

            // 3秒后重置状态
            kotlinx.coroutines.delay(3000)
            _visionApiKeySaved.value = false
        }
    }

    fun selectVisionService(mode: VisionServiceMode) {
        viewModelScope.launch {
            settingsManager.saveVisionServiceMode(mode.name)
            _selfHostedVisionMessage.value = when (mode) {
                VisionServiceMode.GEMINI -> null
                VisionServiceMode.SELF_HOSTED -> if (isSelfHostedVisionPaired.value) {
                    "已选择我的视觉服务器"
                } else {
                    "请输入一次性配对码以连接我的视觉服务器"
                }
            }
        }
    }

    fun pairSelfHostedVision(pairingCode: String) {
        viewModelScope.launch {
            _isPairingSelfHostedVision.value = true
            _selfHostedVisionMessage.value = null
            selfHostedVisionProvider.pair(pairingCode)
                .onSuccess {
                    _selfHostedVisionMessage.value = "已连接到我的视觉服务器"
                }
                .onFailure { error ->
                    _selfHostedVisionMessage.value = error.message ?: "配对失败，请检查配对码和网络"
                }
            _isPairingSelfHostedVision.value = false
        }
    }

    fun unpairSelfHostedVision() {
        viewModelScope.launch {
            selfHostedVisionProvider.clearPairing()
            if (visionServiceMode.value == VisionServiceMode.SELF_HOSTED) {
                settingsManager.saveVisionServiceMode(VisionServiceMode.GEMINI.name)
            }
            _selfHostedVisionMessage.value = "已取消我的视觉服务器配对"
        }
    }

    /**
     * 手动设置供应商
     */
    fun setCustomProvider(provider: AIProvider, apiKey: String, baseUrl: String) {
        viewModelScope.launch {
            settingsManager.saveApiKey(apiKey)
            settingsManager.saveBaseUrl(baseUrl)
            _currentProvider.value = provider
            _availableModels.value = provider.models

            if (provider.models.isNotEmpty()) {
                settingsManager.saveModel(provider.models.first())
            }
        }
    }

    /**
     * 获取所有支持的供应商
     */
    fun getAllProviders(): List<AIProvider> {
        return providerManager.getAllProviders()
    }

    /**
     * 测试当前API配置（简单ping测试）
     */
    fun testConnection() {
        viewModelScope.launch {
            _isValidating.value = true
            _validationMessage.value = "⏳ 正在测试连接..."

            try {
                val currentApiKey = apiKey.value
                val currentBaseUrl = baseUrl.value
                val currentModel = model.value

                if (currentApiKey.isNullOrBlank()) {
                    _validationMessage.value = "❌ 请先配置API Key"
                    _isValidating.value = false
                    return@launch
                }

                if (currentBaseUrl.isNullOrBlank()) {
                    _validationMessage.value = "❌ 请先配置Base URL"
                    _isValidating.value = false
                    return@launch
                }

                // 使用StreamSendMessageUseCase进行简单的ping测试
                val testMessages = listOf(
                    mapOf("role" to "user", "content" to "test")
                )

                var receivedResponse = false
                streamSendMessageUseCase(
                    systemPrompt = "You are a test assistant.",
                    conversationHistory = testMessages,
                    apiParams = ApiParameters(
                        maxTokens = 10,
                        temperature = 0.7,
                        topP = 0.9,
                        frequencyPenalty = 0.0,
                        presencePenalty = 0.0
                    )
                ).catch { e ->
                    // 使用友好的错误消息
                    val friendlyMessage = ErrorMessageHelper.getFriendlyMessage(e)
                    val suggestion = ErrorMessageHelper.getSuggestion(e)

                    _validationMessage.value = if (suggestion != null) {
                        "❌ $friendlyMessage\n\n$suggestion"
                    } else {
                        "❌ $friendlyMessage"
                    }
                }.collect { content ->
                    // 只要收到任何响应就算成功
                    if (!receivedResponse) {
                        receivedResponse = true
                        _validationMessage.value = "✅ 连接成功！\n模型：$currentModel\n响应正常"
                    }
                }

                // 如果没有收到响应
                if (!receivedResponse && _validationMessage.value == "⏳ 正在测试连接...") {
                    _validationMessage.value = "❌ 未收到响应，请检查配置"
                }
            } catch (e: Exception) {
                val friendlyMessage = ErrorMessageHelper.getFriendlyMessage(e)
                _validationMessage.value = "❌ $friendlyMessage"
            } finally {
                _isValidating.value = false
            }
        }
    }

    fun saveThemeMode(mode: String) {
        viewModelScope.launch {
            settingsManager.saveThemeMode(mode)
            android.util.Log.d("SettingsViewModel", "主题模式已保存: $mode")
        }
    }

    fun saveMaterialStyle(style: String) {
        viewModelScope.launch {
            settingsManager.saveMaterialStyle(style)
        }
    }

    fun saveFontSize(size: String) {
        viewModelScope.launch {
            settingsManager.saveFontSize(size)
            android.util.Log.d("SettingsViewModel", "字体大小已保存: $size")
        }
    }

    fun saveNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.saveNotificationsEnabled(enabled)
        }
    }

    fun saveTactileIntensity(preference: TactileIntensityPreference) {
        viewModelScope.launch {
            settingsManager.saveTactileIntensity(preference)
        }
    }

    fun selectVisualBackdrop(target: BackdropTarget, source: Uri) {
        viewModelScope.launch {
            _visualCustomizationMessage.value = null
            runCatching {
                visualCustomizationManager.replaceBackdrop(target, source)
            }.onSuccess {
                _visualCustomizationMessage.value = "背景已更新"
            }.onFailure {
                _visualCustomizationMessage.value = "无法使用这张背景图片"
            }
        }
    }

    fun clearVisualBackdrop(target: BackdropTarget) {
        viewModelScope.launch {
            visualCustomizationManager.clearBackdrop(target)
            _visualCustomizationMessage.value = "背景已移除"
        }
    }

    fun saveVisualAccent(accentHex: String?) {
        viewModelScope.launch {
            val current = visualCustomization.value
            visualCustomizationManager.save(
                current.copy(accentHex = accentHex?.trim()?.takeIf { it.isNotEmpty() })
            )
        }
    }

    fun saveVisualEffectsPreference(preference: VisualEffectsPreference) {
        viewModelScope.launch {
            visualCustomizationManager.save(
                visualCustomization.value.copy(effectsPreference = preference)
            )
        }
    }

    fun saveGlassOpacity(opacity: Float) {
        viewModelScope.launch {
            visualCustomizationManager.save(
                visualCustomization.value.copy(glassOpacity = opacity.coerceIn(0.28f, 0.88f))
            )
        }
    }

    fun resetVisualCustomization() {
        viewModelScope.launch {
            visualCustomizationManager.reset()
            _visualCustomizationMessage.value = "外观已恢复默认"
        }
    }

    fun saveBackdropAppearance(
        target: BackdropTarget,
        imageOpacity: Float,
        scrimOpacity: Float
    ) {
        viewModelScope.launch {
            val current = visualCustomization.value
            visualCustomizationManager.save(
                current.copy(
                    backdrops = current.backdrops + (
                        target to current.backdropFor(target).copy(
                            imageOpacity = imageOpacity,
                            scrimOpacity = scrimOpacity
                        )
                    )
                )
            )
        }
    }

    // ==================== 头像管理 ====================

    fun saveUserAvatar(avatarUrl: String?) {
        viewModelScope.launch {
            settingsManager.saveUserAvatar(avatarUrl)
            android.util.Log.d("SettingsViewModel", "用户头像已保存")
        }
    }

    fun saveCompanionAvatar(companionId: String, avatarUrl: String?) {
        viewModelScope.launch {
            settingsManager.saveCompanionAvatar(companionId, avatarUrl)
            android.util.Log.d("SettingsViewModel", "伴侣 $companionId 头像已保存")
        }
    }
}
