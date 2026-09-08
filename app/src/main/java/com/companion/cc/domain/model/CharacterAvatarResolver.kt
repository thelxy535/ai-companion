package com.companion.cc.domain.model

/**
 * V9PM 头像统一解析：全 App 单一入口。
 * 回退链：设置覆盖/角色图片头像 → 伴侣 emoji（companions 按 id）→ 自定义 ✨ → 名字首字。
 */

data class ResolvedCharacterAvatar(
    val avatarUrl: String?,
    val emoji: String
)

object CharacterAvatarResolver {
    private val imageReferencePattern = Regex(".*\\.(jpe?g|png|webp|gif|heic)(\\?.*)?$", RegexOption.IGNORE_CASE)

    fun resolve(character: ChatCharacter, overrideAvatar: String? = null): ResolvedCharacterAvatar {
        val value = (overrideAvatar ?: character.avatar)?.trim().orEmpty()
        val emojiValue = companionEmojiOf(character)
        return if (value.isNotEmpty() && value.isImageReference()) {
            ResolvedCharacterAvatar(avatarUrl = value, emoji = emojiValue)
        } else {
            ResolvedCharacterAvatar(avatarUrl = null, emoji = emojiValue.ifEmpty { character.name.firstOrNull()?.toString().orEmpty() })
        }
    }

    fun companionEmojiOf(character: ChatCharacter): String {
        val builtIn = companions.firstOrNull { it.id == character.id }
        return when (character) {
            is ChatCharacter.BuiltIn -> builtIn?.emoji ?: "\uD83E\uDD3A"
            is ChatCharacter.Custom -> "\u2728"
        }
    }

    private fun String.isImageReference(): Boolean {
        return contains("://") || startsWith("/") ||
            matches(imageReferencePattern)
    }
}