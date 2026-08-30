package com.companion.cc.integration

import com.companion.cc.data.character.DefaultCharacterPromptResolver
import com.companion.cc.data.local.CompanionConfigLoader
import com.companion.cc.data.mapper.CustomCharacterPromptMapper
import com.companion.cc.domain.character.CharacterPromptSource
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.model.*
import com.companion.cc.domain.repository.CustomCharacterRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * 集成测试：角色 Prompt 持久化场景
 *
 * 验证：
 * 1. 创建自定义角色后，resolver 读取完整持久化数据
 * 2. 编辑角色后，下次 resolve 立即返回最新数据
 * 3. 应用重启（模拟 repository 重新查询）仍能恢复完整配置
 */
class CharacterPromptPersistenceTest {
    private lateinit var configLoader: CompanionConfigLoader
    private lateinit var userProvider: CurrentUserProvider
    private lateinit var repository: CustomCharacterRepository
    private lateinit var resolver: DefaultCharacterPromptResolver

    private val testUserId = "test-user-123"
    private val testCharacterId = "custom-char-456"

    @Before
    fun setup() {
        configLoader = mock()
        userProvider = mock {
            onBlocking { requireUserId() } doReturn testUserId
            on { userId } doReturn flowOf(testUserId)
        }
        repository = mock()
        resolver = DefaultCharacterPromptResolver(
            configLoader = configLoader,
            currentUserProvider = userProvider,
            customCharacterRepository = repository
        )
    }

    @Test
    fun `scenario - create character then resolve returns persisted data`() = runTest {
        // === 1. 创建角色并保存到 Room ===
        val createdCharacter = CustomCharacter(
            id = testCharacterId,
            userId = testUserId,
            name = "初次创建",
            avatar = null,
            description = "初次创建的描述",
            personality = PersonalityTraits(
                openness = 0.8f,
                conscientiousness = 0.7f,
                extraversion = 0.6f,
                agreeableness = 0.9f,
                neuroticism = 0.2f,
                customTraits = mapOf("custom_trait" to "初次创建特质")
            ),
            backstory = "初次创建背景",
            greetingMessage = "初次创建问候",
            exampleDialogues = listOf(
                ExampleDialogue("用户输入", "初次创建回复")
            ),
            voiceConfig = null,
            behaviorRules = BehaviorRules(
                responseStyle = ResponseStyle.FRIENDLY,
                emojiFrequency = EmojiFrequency.MEDIUM,
                formalityLevel = FormalityLevel.CASUAL,
                topicPreferences = listOf("初次偏好话题"),
                avoidTopics = listOf("初次避免话题")
            )
        )

        whenever(repository.getCharacterByIdForUser(testCharacterId, testUserId))
            .thenReturn(createdCharacter)
        whenever(configLoader.getCompanionConfigStrict(testCharacterId))
            .thenReturn(null)

        // === 2. Resolve 并验证返回完整持久化数据 ===
        val resolved = resolver.resolve(testCharacterId)

        assertEquals(CharacterPromptSource.Custom, resolved.source)
        assertEquals("初次创建", resolved.config.name)
        assertTrue(resolved.config.prompts.system.contains("初次创建背景"))
        assertTrue(resolved.config.prompts.system.contains("custom_trait"))
        assertTrue(resolved.config.prompts.system.contains("初次创建特质"))
        assertTrue(resolved.config.prompts.system.contains("初次偏好话题"))
        assertTrue(resolved.config.prompts.system.contains("初次避免话题"))
        assertTrue(resolved.config.prompts.system.contains("用户输入"))
        assertTrue(resolved.config.prompts.system.contains("初次创建回复"))
        assertEquals(listOf("初次创建问候"), resolved.config.prompts.greeting)
    }

    @Test
    fun `scenario - edit character then resolve immediately returns updated data`() = runTest {
        // === 1. 首次 resolve 返回旧数据 ===
        val originalCharacter = CustomCharacter(
            id = testCharacterId,
            userId = testUserId,
            name = "编辑前名称",
            avatar = null,
            description = "编辑前描述",
            personality = PersonalityTraits(
                openness = 0.5f,
                conscientiousness = 0.5f,
                extraversion = 0.5f,
                agreeableness = 0.5f,
                neuroticism = 0.5f
            ),
            backstory = "编辑前背景",
            greetingMessage = "编辑前问候",
            exampleDialogues = emptyList(),
            voiceConfig = null,
            behaviorRules = null
        )

        whenever(repository.getCharacterByIdForUser(testCharacterId, testUserId))
            .thenReturn(originalCharacter)
        whenever(configLoader.getCompanionConfigStrict(testCharacterId))
            .thenReturn(null)

        val firstResolve = resolver.resolve(testCharacterId)
        assertEquals("编辑前名称", firstResolve.config.name)
        assertTrue(firstResolve.config.prompts.system.contains("编辑前背景"))

        // === 2. 模拟用户编辑并保存到 Room ===
        val editedCharacter = originalCharacter.copy(
            name = "编辑后名称",
            backstory = "编辑后背景",
            greetingMessage = "编辑后问候",
            personality = originalCharacter.personality.copy(
                customTraits = mapOf("new_trait" to "编辑后新增特质")
            )
        )

        // Repository 现在返回编辑后的数据
        whenever(repository.getCharacterByIdForUser(testCharacterId, testUserId))
            .thenReturn(editedCharacter)

        // === 3. 再次 resolve，应立即返回最新数据 ===
        val secondResolve = resolver.resolve(testCharacterId)

        assertEquals("编辑后名称", secondResolve.config.name)
        assertEquals(listOf("编辑后问候"), secondResolve.config.prompts.greeting)
        assertTrue(secondResolve.config.prompts.system.contains("编辑后背景"))
        assertTrue(secondResolve.config.prompts.system.contains("new_trait"))
        assertTrue(secondResolve.config.prompts.system.contains("编辑后新增特质"))
        assertFalse(secondResolve.config.prompts.system.contains("编辑前背景"))

        // === 4. 验证两次都调用了 repository，没有使用缓存 ===
        verify(repository, times(2)).getCharacterByIdForUser(testCharacterId, testUserId)
    }

    @Test
    fun `scenario - app restart still recovers full persisted config`() = runTest {
        // === 模拟应用重启：重新创建 resolver 实例 ===
        val characterInDb = CustomCharacter(
            id = testCharacterId,
            userId = testUserId,
            name = "重启后恢复",
            avatar = null,
            description = "持久化描述",
            personality = PersonalityTraits(
                openness = 0.9f,
                conscientiousness = 0.8f,
                extraversion = 0.7f,
                agreeableness = 0.6f,
                neuroticism = 0.1f,
                customTraits = mapOf("persistent" to "持久化特质")
            ),
            backstory = "持久化背景故事",
            greetingMessage = "持久化问候",
            exampleDialogues = listOf(
                ExampleDialogue("持久化用户", "持久化助手回复")
            ),
            voiceConfig = null,
            behaviorRules = BehaviorRules(
                responseStyle = ResponseStyle.ROMANTIC,
                emojiFrequency = EmojiFrequency.HIGH,
                formalityLevel = FormalityLevel.CASUAL,
                topicPreferences = listOf("持久化偏好"),
                avoidTopics = listOf("持久化避免")
            )
        )

        whenever(repository.getCharacterByIdForUser(testCharacterId, testUserId))
            .thenReturn(characterInDb)
        whenever(configLoader.getCompanionConfigStrict(testCharacterId))
            .thenReturn(null)

        // 创建新的 resolver 实例（模拟重启）
        val freshResolver = DefaultCharacterPromptResolver(
            configLoader = configLoader,
            currentUserProvider = userProvider,
            customCharacterRepository = repository
        )

        // === Resolve 应从 Room 恢复完整配置 ===
        val resolved = freshResolver.resolve(testCharacterId)

        assertEquals("重启后恢复", resolved.config.name)
        assertTrue(resolved.config.prompts.system.contains("持久化背景故事"))
        assertTrue(resolved.config.prompts.system.contains("persistent"))
        assertTrue(resolved.config.prompts.system.contains("持久化特质"))
        assertTrue(resolved.config.prompts.system.contains("持久化偏好"))
        assertTrue(resolved.config.prompts.system.contains("持久化避免"))
        assertTrue(resolved.config.prompts.system.contains("持久化用户"))
        assertTrue(resolved.config.prompts.system.contains("持久化助手回复"))
        assertEquals(listOf("持久化问候"), resolved.config.prompts.greeting)
        assertEquals(0.9f, resolved.config.apiParameters.temperature, 0.01f)
    }
}
