package com.companion.cc.domain.repository

import com.companion.cc.data.local.dao.CustomCharacterDao
import com.companion.cc.data.mapper.CustomCharacterMapper
import com.companion.cc.domain.model.CustomCharacter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 自定义角色仓库
 */
@Singleton
class CustomCharacterRepository @Inject constructor(
    private val characterDao: CustomCharacterDao
) {

    /**
     * 获取用户的所有角色
     */
    fun getCharactersByUser(userId: String): Flow<List<CustomCharacter>> {
        return characterDao.getCharactersByUser(userId).map { entities ->
            entities.map { CustomCharacterMapper.toDomain(it) }
        }
    }

    /**
     * 根据ID获取角色
     */
    suspend fun getCharacterById(characterId: String): CustomCharacter? {
        return characterDao.getCharacterById(characterId)?.let {
            CustomCharacterMapper.toDomain(it)
        }
    }

    /**
     * 获取角色 Flow
     */
    fun getCharacterByIdFlow(characterId: String): Flow<CustomCharacter?> {
        return characterDao.getCharacterByIdFlow(characterId).map { entity ->
            entity?.let { CustomCharacterMapper.toDomain(it) }
        }
    }

    /**
     * 保存角色
     */
    suspend fun saveCharacter(character: CustomCharacter) {
        characterDao.insert(CustomCharacterMapper.toEntity(character))
    }

    /**
     * 更新角色
     */
    suspend fun updateCharacter(character: CustomCharacter) {
        characterDao.update(
            CustomCharacterMapper.toEntity(
                character.copy(updatedAt = System.currentTimeMillis())
            )
        )
    }

    /**
     * 删除角色
     */
    suspend fun deleteCharacter(characterId: String) {
        characterDao.delete(characterId)
    }

    /**
     * 删除用户的所有角色
     */
    suspend fun deleteAllByUser(userId: String) {
        characterDao.deleteAllByUser(userId)
    }

    /**
     * 获取用户的角色数量
     */
    suspend fun getCharacterCount(userId: String): Int {
        return characterDao.getCharacterCount(userId)
    }
}
