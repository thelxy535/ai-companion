package com.companion.cc.domain.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文字转语音引擎
 *
 * 使用 Android 原生 TextToSpeech API
 * 支持多语言、语速、音调调整
 */
@Singleton
class VoiceTTSEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    // TTS 状态
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    // 初始化状态
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * 初始化 TTS
     */
    fun initialize() {
        if (isInitialized) return

        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    // 设置中文
                    val result = tts?.setLanguage(Locale.CHINESE)

                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        android.util.Log.w("VoiceTTSEngine", "Chinese not supported, using default")
                        _error.value = "中文语音不支持"
                    }

                    // 设置监听器
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                            android.util.Log.d("VoiceTTSEngine", "Started speaking: $utteranceId")
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                            android.util.Log.d("VoiceTTSEngine", "Done speaking: $utteranceId")
                        }

                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                            _error.value = "语音播放错误"
                            android.util.Log.e("VoiceTTSEngine", "Error speaking: $utteranceId")
                        }
                    })

                    isInitialized = true
                    _isReady.value = true
                    android.util.Log.d("VoiceTTSEngine", "Initialized successfully")
                } else {
                    _error.value = "TTS 初始化失败"
                    android.util.Log.e("VoiceTTSEngine", "Initialization failed")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("VoiceTTSEngine", "Error initializing TTS", e)
            _error.value = "初始化失败: ${e.message}"
        }
    }

    /**
     * 朗读文字
     *
     * @param text 要朗读的文字
     * @param pitch 音调（0.5-2.0，默认1.0）
     * @param speed 语速（0.5-2.0，默认1.0）
     */
    fun speak(
        text: String,
        pitch: Float = 1.0f,
        speed: Float = 1.0f
    ) {
        if (!isInitialized) {
            android.util.Log.w("VoiceTTSEngine", "TTS not initialized")
            initialize()
            return
        }

        if (text.isBlank()) {
            android.util.Log.w("VoiceTTSEngine", "Empty text")
            return
        }

        try {
            // 设置音调和语速
            tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
            tts?.setSpeechRate(speed.coerceIn(0.5f, 2.0f))

            // 开始朗读
            val utteranceId = "tts_${System.currentTimeMillis()}"
            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

            if (result == TextToSpeech.ERROR) {
                _error.value = "语音播放失败"
                android.util.Log.e("VoiceTTSEngine", "Speak error")
            } else {
                android.util.Log.d("VoiceTTSEngine", "Speaking: ${text.take(50)}...")
            }
        } catch (e: Exception) {
            android.util.Log.e("VoiceTTSEngine", "Error speaking", e)
            _error.value = "播放失败: ${e.message}"
        }
    }

    /**
     * 停止朗读
     */
    fun stop() {
        try {
            tts?.stop()
            _isSpeaking.value = false
            android.util.Log.d("VoiceTTSEngine", "Stopped")
        } catch (e: Exception) {
            android.util.Log.e("VoiceTTSEngine", "Error stopping", e)
        }
    }

    /**
     * 设置语言
     */
    fun setLanguage(locale: Locale) {
        try {
            val result = tts?.setLanguage(locale)

            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                _error.value = "不支持该语言"
                android.util.Log.w("VoiceTTSEngine", "Language not supported: $locale")
            } else {
                android.util.Log.d("VoiceTTSEngine", "Language set to: $locale")
            }
        } catch (e: Exception) {
            android.util.Log.e("VoiceTTSEngine", "Error setting language", e)
        }
    }

    /**
     * 获取可用语言列表
     */
    fun getAvailableLanguages(): Set<Locale>? {
        return try {
            tts?.availableLanguages
        } catch (e: Exception) {
            android.util.Log.e("VoiceTTSEngine", "Error getting languages", e)
            null
        }
    }

    /**
     * 清理资源
     */
    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
            _isReady.value = false
            _isSpeaking.value = false
            android.util.Log.d("VoiceTTSEngine", "Shutdown")
        } catch (e: Exception) {
            android.util.Log.e("VoiceTTSEngine", "Error shutting down", e)
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _error.value = null
    }
}
