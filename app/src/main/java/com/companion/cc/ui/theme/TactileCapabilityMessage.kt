package com.companion.cc.ui.theme

internal fun tactileCapabilityMessage(
    hasVibrator: Boolean,
    systemHapticsEnabled: Boolean,
    powerSave: Boolean,
    preference: TactileIntensityPreference,
    effectTier: GlassEffectTier
): String = when {
    !hasVibrator -> "当前设备没有可用的振动器。"
    !systemHapticsEnabled -> "系统触感反馈已关闭，应用不会产生触感。"
    preference == TactileIntensityPreference.OFF -> "应用触感反馈已关闭。"
    effectTier == GlassEffectTier.STEADY -> "当前视觉模式会关闭应用触感反馈。"
    powerSave && preference != TactileIntensityPreference.SYSTEM ->
        "省电模式下，应用会自动降低自定义触感强度。"
    preference == TactileIntensityPreference.SYSTEM ->
        "当前使用设备和系统提供的默认触感。"
    else -> "当前使用应用设定的触感强度。"
}
