package com.companion.cc.domain.model

/**
 * Display-ready avatar values. Persisted avatar fields may contain either an image
 * reference or an emoji, so callers do not have to send emoji text through Coil.
 */
data class ResolvedCharacterAvatar(
    val avatarUrl: String?,
    val emoji: String
)

object CharacterAvatarResolver {
    fun resolve(avatar: String?, name: String): ResolvedCharacterAvatar {
        val value = avatar?.trim().orEmpty()
        return if (value.isNotEmpty() && value.isImageReference()) {
            ResolvedCharacterAvatar(avatarUrl = value, emoji = name.firstOrNull()?.toString().orEmpty())
        } else {
            ResolvedCharacterAvatar(avatarUrl = null, emoji = value.ifEmpty { name.firstOrNull()?.toString().orEmpty() })
        }
    }

    private fun String.isImageReference(): Boolean {
        return contains("://") || startsWith("/") ||
            matches(Regex(".*\\.(jpe?g|png|webp|gif|heic)(\\?.*)?$", RegexOption.IGNORE_CASE))
    }
}
