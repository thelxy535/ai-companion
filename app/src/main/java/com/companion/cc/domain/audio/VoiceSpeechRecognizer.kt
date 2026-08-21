package com.companion.cc.domain.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语音识别器
 *
 * 使用 Android 原生 SpeechRecognizer API
 * 支持实时语音转文字
 */
@Singleton
class VoiceSpeechRecognizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognitionListener: RecognitionListener? = null

    // 识别状态
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    // 识别结果
    private val _recognitionResult = MutableStateFlow<String?>(null)
    val recognitionResult: StateFlow<String?> = _recognitionResult.asStateFlow()

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 音量级别（0-10）
    private val _volumeLevel = MutableStateFlow(0f)
    val volumeLevel: StateFlow<Float> = _volumeLevel.asStateFlow()

    /**
     * 开始监听
     *
     * @param language 语言代码，默认中文
     */
    fun startListening(language: String = "zh-CN") {
        if (_isListening.value) {
            android.util.Log.w("VoiceSpeechRecognizer", "Already listening")
            return
        }

        try {
            // 检查是否可用
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _error.value = "语音识别不可用"
                return
            }

            // 清理旧的识别器（如果存在）
            if (speechRecognizer != null) {
                try {
                    speechRecognizer?.destroy()
                } catch (e: Exception) {
                    android.util.Log.w("VoiceSpeechRecognizer", "Error destroying old recognizer", e)
                }
            }

            // 创建新的识别器
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

            // 设置监听器
            recognitionListener = createRecognitionListener()
            speechRecognizer?.setRecognitionListener(recognitionListener)

            // 创建 Intent
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            // 开始识别（不立即设置 isListening，等待 onReadyForSpeech 回调）
            speechRecognizer?.startListening(intent)
            _error.value = null

            android.util.Log.d("VoiceSpeechRecognizer", "Starting listening...")
        } catch (e: Exception) {
            android.util.Log.e("VoiceSpeechRecognizer", "Error starting recognition", e)
            _error.value = "启动识别失败: ${e.message}"
            _isListening.value = false
        }
    }

    /**
     * 停止监听
     */
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            _isListening.value = false
            android.util.Log.d("VoiceSpeechRecognizer", "Stopped listening")
        } catch (e: Exception) {
            android.util.Log.e("VoiceSpeechRecognizer", "Error stopping recognition", e)
        }
    }

    /**
     * 取消监听
     */
    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
            _isListening.value = false
            _recognitionResult.value = null
            android.util.Log.d("VoiceSpeechRecognizer", "Cancelled listening")
        } catch (e: Exception) {
            android.util.Log.e("VoiceSpeechRecognizer", "Error cancelling recognition", e)
        }
    }

    /**
     * 创建识别监听器
     */
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                android.util.Log.d("VoiceSpeechRecognizer", "Ready for speech")
                _isListening.value = true
            }

            override fun onBeginningOfSpeech() {
                android.util.Log.d("VoiceSpeechRecognizer", "Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // 更新音量级别（归一化到 0-10）
                _volumeLevel.value = (rmsdB / 2f).coerceIn(0f, 10f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // 不处理
            }

            override fun onEndOfSpeech() {
                android.util.Log.d("VoiceSpeechRecognizer", "Speech ended")
                _isListening.value = false
            }

            override fun onError(error: Int) {
                val errorMessage = getErrorMessage(error)
                android.util.Log.e("VoiceSpeechRecognizer", "Recognition error: $errorMessage (code: $error)")

                // 对于某些错误，不显示给用户（静默失败，可以重试）
                val isSilentError = error == SpeechRecognizer.ERROR_NO_MATCH ||
                                   error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT

                if (!isSilentError) {
                    _error.value = errorMessage
                }
                _isListening.value = false
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val result = matches[0]
                    _recognitionResult.value = result
                    android.util.Log.d("VoiceSpeechRecognizer", "Recognition result: $result")
                }
                _isListening.value = false
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val result = matches[0]
                    _recognitionResult.value = result
                    android.util.Log.d("VoiceSpeechRecognizer", "Partial result: $result")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // 不处理
            }
        }
    }

    /**
     * 获取错误信息
     */
    private fun getErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "音频错误"
            SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
            SpeechRecognizer.ERROR_NETWORK -> "网络错误"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
            SpeechRecognizer.ERROR_NO_MATCH -> "没有匹配"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙碌"
            SpeechRecognizer.ERROR_SERVER -> "服务器错误"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音超时"
            else -> "未知错误: $error"
        }
    }

    /**
     * 清理资源
     */
    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            recognitionListener = null
            _isListening.value = false
            android.util.Log.d("VoiceSpeechRecognizer", "Destroyed")
        } catch (e: Exception) {
            android.util.Log.e("VoiceSpeechRecognizer", "Error destroying", e)
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * 清除结果
     */
    fun clearResult() {
        _recognitionResult.value = null
    }
}
