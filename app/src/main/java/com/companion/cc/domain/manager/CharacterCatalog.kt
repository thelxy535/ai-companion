package com.companion.cc.domain.manager

import com.companion.cc.domain.model.ChatCharacter
import com.companion.cc.domain.model.companions
import com.companion.cc.domain.repository.CustomCharacterRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 角色目录 - 统一管理内置角色和自定义角色
 */
@Singleton
class CharacterCatalog @Inject constructor(
    private val customCharacterRepository: CustomCharacterRepository
) {
    /**
     * 获取角色（内置或自定义）
     * @param userId 用户 ID
     * @param characterId 角色 ID
     * @return 角色对象，如果不存在或无权访问则返回 null
     */
    suspend fun getCharacter(userId: String, characterId: String): ChatCharacter? {
        // 先检查是否为内置角色
        val builtInCompanion = companions.find { it.id == characterId }
        if (builtInCompanion != null) {
            return ChatCharacter.BuiltIn(
                id = builtInCompanion.id,
                name = builtInCompanion.name,
                avatar = builtInCompanion.avatarUrl,
                description = builtInCompanion.description
            )
        }

        // 检查自定义角色（带用户权限检查）
        val customCharacter = customCharacterRepository.getCharacterByIdForUser(characterId, userId)
        return customCharacter?.let {
            ChatCharacter.Custom(
                id = it.id,
                name = it.name,
                avatar = it.avatar,
                description = it.description,
                personality = it.personality.toString(),
                userId = it.userId
            )
        }
    }

    /**
     * 判断角色是否存在
     */
    suspend fun characterExists(userId: String, characterId: String): Boolean {
        return getCharacter(userId, characterId) != null
    }

    /**
     * 判断角色是否为内置角色
     */
    fun isBuiltInCharacter(characterId: String): Boolean {
        return companions.any { it.id == characterId }
    }

    /**
     * 判断角色是否为自定义角色
     */
    suspend fun isCustomCharacter(userId: String, characterId: String): Boolean {
        if (isBuiltInCharacter(characterId)) {
            return false
        }
        return customCharacterRepository.getCharacterByIdForUser(characterId, userId) != null
    }
}