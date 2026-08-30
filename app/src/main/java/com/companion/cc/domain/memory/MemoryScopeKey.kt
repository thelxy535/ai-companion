package com.companion.cc.domain.memory

object MemoryScopeKey {
    fun forCharacter(userId: String, characterId: String): String {
        val normalizedUserId = userId.trim().also {
            require(it.isNotEmpty()) { "userId must not be blank" }
        }
        val normalizedCharacterId = characterId.trim().also {
            require(it.isNotEmpty()) { "characterId must not be blank" }
        }
        return "user:$normalizedUserId:companion:$normalizedCharacterId"
    }

    fun belongsToUser(scopeKey: String, userId: String): Boolean {
        val normalizedUserId = userId.trim()
        return normalizedUserId.isNotEmpty() &&
            scopeKey.startsWith("user:$normalizedUserId:companion:")
    }
}
