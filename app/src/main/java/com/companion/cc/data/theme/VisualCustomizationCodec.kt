package com.companion.cc.data.theme

import com.companion.cc.domain.model.BackdropKind
import com.companion.cc.domain.model.BackdropTarget
import com.companion.cc.domain.model.VisualBackdrop
import com.companion.cc.domain.model.VisualCustomization
import com.companion.cc.ui.theme.VisualEffectsPreference
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles durable appearance data without exposing Gson's enum behavior to the
 * rest of the app. Unknown or malformed historical values always degrade to a
 * safe visual default instead of breaking theme rendering.
 */
@Singleton
class VisualCustomizationCodec @Inject constructor(
    private val gson: Gson
) {
    fun encode(customization: VisualCustomization): String = gson.toJson(
        StoredCustomization(
            accentHex = customization.accentHex,
            effectsPreference = customization.effectsPreference.name,
            glassOpacity = customization.glassOpacity,
            backdrops = customization.backdrops.mapKeys { it.key.name }
                .mapValues { (_, backdrop) ->
                    StoredBackdrop(
                        kind = backdrop.kind.name,
                        managedImageReference = backdrop.managedImageReference,
                        imageOpacity = backdrop.imageOpacity,
                        scrimOpacity = backdrop.scrimOpacity
                    )
                }
        )
    )

    fun decode(raw: String?): VisualCustomization {
        if (raw.isNullOrBlank()) return VisualCustomization.default()
        val stored = runCatching {
            gson.fromJson(raw, StoredCustomization::class.java)
        }.getOrNull() ?: return VisualCustomization.default()

        val backdrops = stored.backdrops.orEmpty().mapNotNull { (targetValue, backdrop) ->
            val target = BackdropTarget.entries.firstOrNull { it.name == targetValue }
                ?: return@mapNotNull null
            target to normalizeBackdrop(backdrop)
        }.toMap()

        return VisualCustomization(
            accentHex = normalizeHex(stored.accentHex),
            effectsPreference = VisualEffectsPreference.fromStoredValue(stored.effectsPreference),
            glassOpacity = (stored.glassOpacity ?: VisualCustomization.DEFAULT_GLASS_OPACITY)
                .coerceIn(0.28f, 0.88f),
            backdrops = backdrops.ifEmpty { VisualCustomization.default().backdrops }
        )
    }

    private fun normalizeBackdrop(stored: StoredBackdrop): VisualBackdrop {
        val kind = BackdropKind.entries.firstOrNull { it.name == stored.kind }
            ?: BackdropKind.NONE
        val reference = stored.managedImageReference
            ?.trim()
            ?.takeIf { it.startsWith("file:") }

        if (kind != BackdropKind.IMAGE || reference == null) {
            return VisualBackdrop()
        }

        return VisualBackdrop(
            kind = BackdropKind.IMAGE,
            managedImageReference = reference,
            imageOpacity = stored.imageOpacity.coerceIn(0f, 1f),
            scrimOpacity = stored.scrimOpacity.coerceIn(0f, 1f)
        )
    }

    private fun normalizeHex(value: String?): String? {
        val compact = value?.trim()?.removePrefix("#") ?: return null
        val expanded = when (compact.length) {
            3 -> compact.map { "$it$it" }.joinToString(separator = "")
            6 -> compact
            else -> return null
        }
        if (!expanded.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            return null
        }
        return "#${expanded.uppercase()}"
    }

    private data class StoredCustomization(
        val accentHex: String? = null,
        val effectsPreference: String? = null,
        val glassOpacity: Float? = VisualCustomization.DEFAULT_GLASS_OPACITY,
        val backdrops: Map<String, StoredBackdrop>? = null
    )

    private data class StoredBackdrop(
        val kind: String? = null,
        val managedImageReference: String? = null,
        val imageOpacity: Float = VisualBackdrop.DEFAULT_IMAGE_OPACITY,
        val scrimOpacity: Float = VisualBackdrop.DEFAULT_SCRIM_OPACITY
    )
}
