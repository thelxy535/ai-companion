package com.companion.cc.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.companion.cc.util.EncryptionHelper
import com.companion.cc.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionHelper: EncryptionHelper
) {
    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val API_KEY = stringPreferencesKey("api_key")
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL = stringPreferencesKey("model")
        val VISION_API_KEY = stringPreferencesKey("vision_api_key")  // 视觉模型专用 API Key
        val THEME_MODE = stringPreferencesKey("theme_mode")  // 主题模式: "system", "light", "dark"
        val FONT_SIZE = stringPreferencesKey("font_size")  // 字体大小: "small", "medium", "large", "xlarge"
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

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.THEME_MODE] ?: "system"  // 默认跟随系统
    }

    val fontSizeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.FONT_SIZE] ?: "medium"  // 默认中等
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

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = mode
            Logger.d("SettingsManager", "主题模式已保存: $mode")
        }
    }

    suspend fun saveFontSize(size: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.FONT_SIZE] = size
            Logger.d("SettingsManager", "字体大小已保存: $size")
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

    suspend fun clearAllSettings() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
