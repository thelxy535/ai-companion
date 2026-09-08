package com.companion.cc.domain.character

import com.companion.cc.domain.model.VoiceConfig

/**
 * 角色音色解析（V9PM 第 7 项）。
 *
 * 从角色 VoiceConfig 解析 TTS 音调/语速；无配置或越界时回退安全默认值。
 */
object CharacterVoiceResolver {
    const val DEFAULT_PITCH = 1.0f
    const val DEFAULT_SPEED = 1.0f

    private const val MIN = 0.5f
    private const val MAX = 2.0f

    fun pitch(config: VoiceConfig?): Float = (config?.pitch ?: DEFAULT_PITCH).coerceIn(MIN, MAX)

    fun speed(config: VoiceConfig?): Float = (config?.speed ?: DEFAULT_SPEED).coerceIn(MIN, MAX)
}
