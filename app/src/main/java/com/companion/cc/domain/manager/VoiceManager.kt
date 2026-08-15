package com.companion.cc.domain.manager

import com.companion.cc.domain.audio.VoiceSpeechRecognizer
import com.companion.cc.domain.audio.VoiceTTSEngine
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语音管理器
 *
 * 统一管理语音输入和输出
 * 提供简单的接口给 ViewModel 使用
 */
@Singleton
class VoiceManager @Inject constructor(
    private val speechRecognizer: VoiceSpeechRecognizer,
    private val ttsEngine: VoiceTTSEngine
) {
    // TTS 状态
    val isSpeaking: StateFlow<Boolean> = ttsEngine.isSpeaking
    val ttsReady: StateFlow<Boolean> = ttsEngine.isReady
    val ttsError: StateFlow<String?> = ttsEngine.error

    // STT 状态
    val isListening: StateFlow<Boolean> = speechRecognizer.isListening
    val recognitionResult: StateFlow<String?> = speechRecognizer.recognitionResult
    val sttError: StateFlow<String?> = speechRecognizer.error
    val volumeLevel: StateFlow<Float> = speechRecognizer.volumeLevel

    /**
     * 初始化
     */
    fun initialize() {
        ttsEngine.initialize()
    }

    /**
     * 开始语音输入
     */
    fun startVoiceInput() {
        speechRecognizer.startListening()
    }

    /**
     * 停止语音输入
     */
    fun stopVoiceInput() {
        speechRecognizer.stopListening()
    }

    /**
     * 取消语音输入
     */
    fun cancelVoiceInput() {
        speechRecognizer.cancelListening()
    }

    /**
     * 朗读文字
     *
     * @param text 要朗读的文字
     * @param pitch 音调（0.5-2.0）
     * @param speed 语速（0.5-2.0）
     */
    fun speak(text: String, pitch: Float = 1.0f, speed: Float = 1.0f) {
        ttsEngine.speak(text, pitch, speed)
    }

    /**
     * 停止朗读
     */
    fun stopSpeaking() {
        ttsEngine.stop()
    }

    /**
     * 清除识别结果
     */
    fun clearRecognitionResult() {
        speechRecognizer.clearResult()
    }

    /**
     * 清除错误
     */
    fun clearErrors() {
        ttsEngine.clearError()
        speechRecognizer.clearError()
    }

    /**
     * 清理资源
     */
    fun destroy() {
        speechRecognizer.destroy()
        ttsEngine.shutdown()
    }
}
