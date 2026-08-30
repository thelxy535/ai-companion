package com.companion.cc.ui.theme

object TactileIntensityPreferenceCodec {
    fun toStoredValue(preference: TactileIntensityPreference): String = preference.name.lowercase()

    fun fromStoredValue(value: String?): TactileIntensityPreference =
        value?.let { stored ->
            TactileIntensityPreference.entries.firstOrNull {
                it.name.equals(stored, ignoreCase = true)
            }
        } ?: TactileIntensityPreference.SYSTEM
}
