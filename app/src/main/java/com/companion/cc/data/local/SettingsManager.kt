package com.companion.cc.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.companion.cc.util.EncryptionHelper
import com.companion.cc.util.Logger
import com.companion.cc.domain.capability.CapabilityFlags
import com.companion.cc.domain.capability.CapabilityScope
import com.companion.cc.domain.usage.UsageRecord
import com.companion.cc.domain.usage.UsageSummary
import com.companion.cc.domain.usage.UsageCostCalculator
import com.companion.cc.ui.theme.TactileIntensityPreference
import com.companion.cc.ui.theme.TactileIntensityPreferenceCodec
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionHelper: EncryptionHelper
) {
    companion object {
        const val ENCRYPTED_VALUE_PREFIX = "keystore:"
            private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")

            /**
             * V7 设置页通知开关（同步读取，供通知发送方门控）。
             * DataStore 实例与 SettingsManager 相同（"settings" 文件），超时兜底返回 true。
             */
            fun isNotificationPrefEnabled(context: Context): Boolean = try {
                kotlinx.coroutines.runBlocking {
                    kotlinx.coroutines.withTimeoutOrNull(500) {
                        context.dataStore.data.first()[NOTIFICATIONS_ENABLED_KEY] ?: true
                    } ?: true
                }
            } catch (e: Exception) { true }
    }

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val API_KEY = stringPreferencesKey("api_key")
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL = stringPreferencesKey("model")
        val VISION_API_KEY = stringPreferencesKey("vision_api_key")  // 视觉模型专用 API Key
        val VISION_SERVICE_MODE = stringPreferencesKey("vision_service_mode")
        val SELF_HOSTED_DEVICE_TOKEN = stringPreferencesKey("self_hosted_device_token")
        val INSTALLATION_ID = stringPreferencesKey("installation_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")  // 主题模式: "system", "light", "dark"
        val FONT_SIZE = stringPreferencesKey("font_size")  // 字体大小: "small", "medium", "large", "xlarge"
        val TACTILE_INTENSITY = stringPreferencesKey("tactile_intensity")
        val MATERIAL_STYLE = stringPreferencesKey("material_style")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val VISUAL_CUSTOMIZATION = stringPreferencesKey("visual_customization")
        val USER_AVATAR = stringPreferencesKey("user_avatar")  // 用户头像 URL
        val COMPANION_AVATAR_PREFIX = "companion_avatar_"  // AI 伴侣头像前缀
    }

    val userIdFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.USER_ID] ?: generateUserId().also { id ->
            saveUserId(id)
        }
    }

    val apiKeyFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        val encryptedKey = preferences[Keys.API_KEY]
        if (encryptedKey != null) {
            // 尝试解密
            val decrypted = encryptionHelper.decrypt(encryptedKey)
            if (decrypted != null) {
                Logger.d("SettingsManager", "【加密】API Key 解密成功")
                decrypted
            } else {
                // 解密失败，可能是旧版本未加密的数据
                Logger.w("SettingsManager", "【加密】API Key 解密失败，返回原始数据")
                encryptedKey
            }
        } else {
            null
        }
    }

    val baseUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.BASE_URL] ?: "https://api.siliconflow.cn/v1/"
    }

    val modelFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.MODEL] ?: "deepseek-ai/DeepSeek-V3"  // 原Web版使用的模型
    }

    val visionApiKeyFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        val encryptedKey = preferences[Keys.VISION_API_KEY]
        if (encryptedKey != null) {
            // 尝试解密
            val decrypted = encryptionHelper.decrypt(encryptedKey)
            if (decrypted != null) {
                Logger.d("SettingsManager", "【加密】视觉 API Key 解密成功")
                decrypted
            } else {
                Logger.w("SettingsManager", "【加密】视觉 API Key 解密失败，返回原始数据")
                encryptedKey
            }
        } else {
            null
        }
    }

    val visionServiceModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.VISION_SERVICE_MODE] ?: "GEMINI"
    }

    val selfHostedDeviceTokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[Keys.SELF_HOSTED_DEVICE_TOKEN]
            ?.takeIf { it.startsWith(ENCRYPTED_VALUE_PREFIX) }
            ?.removePrefix(ENCRYPTED_VALUE_PREFIX)
            ?.let(encryptionHelper::decrypt)
    }

    /**
     * 在同一次 DataStore 事务内创建或读取安装标识，避免并发首次配对时拿到不同 ID。
     */
    suspend fun getOrCreateInstallationId(): String {
        var installationId: String? = null
        context.dataStore.edit { preferences ->
            installationId = preferences[Keys.INSTALLATION_ID]
            if (installationId == null) {
                installationId = UUID.randomUUID().toString()
                preferences[Keys.INSTALLATION_ID] = requireNotNull(installationId)
            }
        }
        return requireNotNull(installationId)
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.THEME_MODE] ?: "system"  // 默认跟随系统
    }

    val materialStyleFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.MATERIAL_STYLE] ?: "GLASS"  // V7 默认玻璃
    }

    val fontSizeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.FONT_SIZE] ?: "medium"  // 默认中等
    }

    // V7 设置页通知开关（持久化偏好，独立于系统运行时授权）
    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.NOTIFICATIONS_ENABLED] ?: true
    }

    val tactileIntensityFlow: Flow<TactileIntensityPreference> = context.dataStore.data.map { preferences ->
        TactileIntensityPreferenceCodec.fromStoredValue(preferences[Keys.TACTILE_INTENSITY])
    }

    /** Serialized appearance settings owned by the visual customization manager. */
    val visualCustomizationFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[Keys.VISUAL_CUSTOMIZATION]
    }

    /** Serialized appearance settings owned by the visual customization manager. */
    fun usageSummaryFlow(userId: String, characterId: String): Flow<UsageSummary> =
        context.dataStore.data.map { preferences ->
            val scope = "usage:${userId}:${characterId}"
            UsageSummary(
                inputTokens = preferences[longPreferencesKey("$scope:input")] ?: 0L,
                outputTokens = preferences[longPreferencesKey("$scope:output")] ?: 0L,
                estimatedCost = (preferences[longPreferencesKey("$scope:costMicros")] ?: 0L) / 1_000_000.0
            )
        }

    suspend fun recordUsage(record: UsageRecord) {
        val scope = "usage:${record.userId}:${record.characterId}"
        val costMicros = (UsageCostCalculator.cost(record) * 1_000_000.0).toLong()
        context.dataStore.edit { preferences ->
            val inputKey = longPreferencesKey("$scope:input")
            val outputKey = longPreferencesKey("$scope:output")
            val costKey = longPreferencesKey("$scope:costMicros")
            preferences[inputKey] = (preferences[inputKey] ?: 0L) + record.inputTokens
            preferences[outputKey] = (preferences[outputKey] ?: 0L) + record.outputTokens
            preferences[costKey] = (preferences[costKey] ?: 0L) + costMicros
        }
    }
    fun capabilityFlagsFlow(userId: String, characterId: String): Flow<CapabilityFlags> =
        context.dataStore.data.map { preferences ->
            val scope = CapabilityScope.key(userId, characterId)
            CapabilityFlags(
                image = preferences[booleanPreferencesKey("$scope:image")] ?: false,
                video = preferences[booleanPreferencesKey("$scope:video")] ?: false,
                voice = preferences[booleanPreferencesKey("$scope:voice")] ?: false,
                vision = preferences[booleanPreferencesKey("$scope:vision")] ?: false,
                time = preferences[booleanPreferencesKey("$scope:time")] ?: false,
                email = preferences[booleanPreferencesKey("$scope:email")] ?: false
            )
        }

    suspend fun saveCapabilityFlags(
        userId: String,
        characterId: String,
        flags: CapabilityFlags
    ) {
        val scope = CapabilityScope.key(userId, characterId)
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("$scope:image")] = flags.image
            preferences[booleanPreferencesKey("$scope:video")] = flags.video
            preferences[booleanPreferencesKey("$scope:voice")] = flags.voice
            preferences[booleanPreferencesKey("$scope:vision")] = flags.vision
            preferences[booleanPreferencesKey("$scope:time")] = flags.time
            preferences[booleanPreferencesKey("$scope:email")] = flags.email
        }
    }

    private fun generateUserId(): String {
        return "user_${UUID.randomUUID()}"
    }

    private suspend fun saveUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.USER_ID] = userId
        }
    }

    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            // 加密 API Key
            val encrypted = encryptionHelper.encrypt(apiKey)
            if (encrypted != null) {
                preferences[Keys.API_KEY] = encrypted
                Logger.d("SettingsManager", "【加密】API Key 已加密保存")
            } else {
                // 加密失败，保存明文（降级处理）
                Logger.w("SettingsManager", "【加密】API Key 加密失败，保存明文")
                preferences[Keys.API_KEY] = apiKey
            }
        }
    }

    suspend fun saveBaseUrl(baseUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.BASE_URL] = baseUrl
        }
    }

    suspend fun saveModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.MODEL] = model
        }
    }

    suspend fun saveVisionApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            // 加密 Vision API Key
            val encrypted = encryptionHelper.encrypt(apiKey)
            if (encrypted != null) {
                preferences[Keys.VISION_API_KEY] = encrypted
                Logger.d("SettingsManager", "【加密】视觉 API Key 已加密保存")
            } else {
                // 加密失败，保存明文（降级处理）
                Logger.w("SettingsManager", "【加密】视觉 API Key 加密失败，保存明文")
                preferences[Keys.VISION_API_KEY] = apiKey
            }
        }
    }

    suspend fun saveVisionServiceMode(mode: String) {
        val normalized = if (mode == "SELF_HOSTED") "SELF_HOSTED" else "GEMINI"
        context.dataStore.edit { preferences ->
            preferences[Keys.VISION_SERVICE_MODE] = normalized
        }
    }

    suspend fun saveSelfHostedDeviceToken(token: String) {
        require(token.isNotBlank()) { "设备令牌不能为空" }
        val encryptedToken = encryptionHelper.encrypt(token)
            ?: throw SecurityException("无法安全保存设备令牌，请检查设备安全设置后重试")
        context.dataStore.edit { preferences ->
            preferences[Keys.SELF_HOSTED_DEVICE_TOKEN] = ENCRYPTED_VALUE_PREFIX + encryptedToken
        }
    }

    suspend fun clearSelfHostedDeviceToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(Keys.SELF_HOSTED_DEVICE_TOKEN)
        }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = mode
            Logger.d("SettingsManager", "主题模式已保存: $mode")
        }
    }

    suspend fun saveMaterialStyle(style: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.MATERIAL_STYLE] = style
            Logger.d("SettingsManager", "材质风格已保存: $style")
        }
    }

    // V9 造人：TA 的内在状态快照（scopeKey = "userId:companionId"），重启不失忆
    suspend fun saveEmotionSnapshot(scopeKey: String, snapshot: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey("emotion_$scopeKey")] = snapshot
        }
    }

    suspend fun loadEmotionSnapshot(scopeKey: String): String? =
        context.dataStore.data.map { it[stringPreferencesKey("emotion_$scopeKey")] }.first()

    suspend fun saveNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun saveFontSize(size: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.FONT_SIZE] = size
            Logger.d("SettingsManager", "字体大小已保存: $size")
        }
    }

    suspend fun saveTactileIntensity(preference: TactileIntensityPreference) {
        context.dataStore.edit { preferences ->
            preferences[Keys.TACTILE_INTENSITY] = TactileIntensityPreferenceCodec.toStoredValue(preference)
        }
    }

    suspend fun saveVisualCustomization(serializedCustomization: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.VISUAL_CUSTOMIZATION] = serializedCustomization
        }
    }

    // ==================== 头像管理 ====================

    /**
     * 获取用户头像 URL
     */
    val userAvatarFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[Keys.USER_AVATAR]
    }

    /**
     * 保存用户头像
     */
    suspend fun saveUserAvatar(avatarUrl: String?) {
        context.dataStore.edit { preferences ->
            if (avatarUrl != null) {
                preferences[Keys.USER_AVATAR] = avatarUrl
                Logger.d("SettingsManager", "用户头像已保存")
            } else {
                preferences.remove(Keys.USER_AVATAR)
                Logger.d("SettingsManager", "用户头像已清除")
            }
        }
    }

    /**
     * 获取 AI 伴侣头像 URL
     */
    fun getCompanionAvatarFlow(companionId: String): Flow<String?> {
        val key = stringPreferencesKey("${Keys.COMPANION_AVATAR_PREFIX}$companionId")
        return context.dataStore.data.map { preferences ->
            preferences[key]
        }
    }

    /**
     * 保存 AI 伴侣头像
     */
    suspend fun saveCompanionAvatar(companionId: String, avatarUrl: String?) {
        val key = stringPreferencesKey("${Keys.COMPANION_AVATAR_PREFIX}$companionId")
        context.dataStore.edit { preferences ->
            if (avatarUrl != null) {
                preferences[key] = avatarUrl
                Logger.d("SettingsManager", "伴侣 $companionId 头像已保存")
            } else {
                preferences.remove(key)
                Logger.d("SettingsManager", "伴侣 $companionId 头像已清除")
            }
        }
    }

    /** Reset user preferences without changing the stable local identity. */
    suspend fun resetUserPreferences() {
        context.dataStore.edit { preferences ->
            val userId = preferences[Keys.USER_ID]
            val installationId = preferences[Keys.INSTALLATION_ID]
            preferences.clear()
            userId?.let { preferences[Keys.USER_ID] = it }
            installationId?.let { preferences[Keys.INSTALLATION_ID] = it }
        }
    }

    suspend fun clearAllSettings() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
