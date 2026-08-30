package com.companion.cc.domain.manager

import com.companion.cc.domain.model.*
import com.companion.cc.domain.repository.CustomCharacterRepository
import com.companion.cc.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 角色自定义管理器
 *
 * 负责：
 * 1. 创建、更新、删除自定义角色
 * 2. 生成动态 System Prompt
 * 3. 集成到现有 PersonalityManager
 */
@Singleton
class CharacterCustomizationManager @Inject constructor(
    private val characterRepository: CustomCharacterRepository,
    private val personalityManager: PersonalityManager,
    private val messageRepository: MessageRepository,
    private val memoryLayerManager: MemoryLayerManager,
    private val vectorMemoryManager: VectorMemoryManager,
    private val deletionService: com.companion.cc.domain.character.CharacterDeletionService
) {

    /**
     * 创建新的自定义角色
     */
    suspend fun createCharacter(
        userId: String,
        name: String,
        description: String,
        personality: PersonalityTraits,
        backstory: String,
        greetingMessage: String = "你好，我是 $name",
        exampleDialogues: List<ExampleDialogue> = emptyList(),
        voiceConfig: VoiceConfig? = null,
        behaviorRules: BehaviorRules? = null
    ): CustomCharacter {
        val characterId = UUID.randomUUID().toString()
        val character = CustomCharacter(
            id = characterId,
            userId = userId,
            name = name,
            avatar = null,
            description = description,
            personality = personality,
            backstory = backstory,
            greetingMessage = greetingMessage,
            exampleDialogues = exampleDialogues,
            voiceConfig = voiceConfig,
            behaviorRules = behaviorRules
        )

        // 保存到数据库
        characterRepository.saveCharacter(character)

        return character
    }

    /**
     * 更新角色
     */
    suspend fun updateCharacter(character: CustomCharacter): CustomCharacter {
        characterRepository.updateCharacter(character)
        return character
    }

    /**
     * 删除角色（级联删除所有相关数据）
     *
     * 清理：
     * 1. 角色的所有消息
     * 2. 角色的所有向量记忆
     * 3. PersonalityManager 缓存
     * 4. MemoryLayerManager 缓存
     * 5. 角色本身
     */
    suspend fun deleteCharacter(userId: String, characterId: String) {
        deletionService.delete(userId, characterId).getOrThrow()
        personalityManager.removeResolvedConfig(characterId)
        memoryLayerManager.invalidateCharacter(userId, characterId)
        vectorMemoryManager.clear(userId, characterId)
    }

    /**
     * 获取角色
     */
    suspend fun getCharacter(userId: String, characterId: String): CustomCharacter? {
        return characterRepository.getCharacterByIdForUser(characterId, userId)
    }

    /**
     * 获取用户的所有角色
     */
    fun getUserCharacters(userId: String): Flow<List<CustomCharacter>> {
        return characterRepository.getCharactersByUser(userId)
    }
}
