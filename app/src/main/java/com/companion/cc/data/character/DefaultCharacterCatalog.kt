package com.companion.cc.data.character

import com.companion.cc.data.local.CompanionConfigLoader
import com.companion.cc.domain.character.CharacterCatalog
import com.companion.cc.domain.character.LegacyCharacterOwnershipMigrator
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.model.ChatCharacter
import com.companion.cc.domain.repository.CustomCharacterRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [CharacterCatalog] that merges built-in and custom characters.
 */
@Singleton
class DefaultCharacterCatalog @Inject constructor(
    private val users: CurrentUserProvider,
    private val repository: CustomCharacterRepository,
    private val configLoader: CompanionConfigLoader,
    private val migrator: LegacyCharacterOwnershipMigrator
) : CharacterCatalog {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCharacters(): Flow<List<ChatCharacter>> {
        return users.userId
            .onStart {
                // Run migration once on first subscription
                migrator.migrateIfNeeded()
            }
            .flatMapLatest { userId ->
                repository.getCharactersByUser(userId).map { customCharacters ->
                    // Built-in characters
                    val builtIn = configLoader.getEnabledCompanions().map { config ->
                        ChatCharacter.BuiltIn(
                            id = config.id,
                            name = config.name,
                            avatar = config.avatar,
                            description = config.personality.background
                        )
                    }
                    // Custom characters
                    val custom = customCharacters.map { customChar ->
                        ChatCharacter.Custom(
                            id = customChar.id,
                            name = customChar.name,
                            avatar = customChar.avatar,
                            description = customChar.description,
                            personality = customChar.personality.toString(),
                            userId = customChar.userId
                        )
                    }
                    builtIn + custom
                }
            }
    }

    override suspend fun getCharacter(characterId: String): ChatCharacter? {
        // Check built-in characters first
        configLoader.getCompanionConfig(characterId)?.let { config ->
            return ChatCharacter.BuiltIn(
                id = config.id,
                name = config.name,
                avatar = config.avatar,
                description = config.personality.background
            )
        }

        // Check custom characters for current user
        val userId = users.requireUserId()
        return repository.getCharacterById(characterId)?.let { customChar ->
            // Only return if it belongs to the current user
            if (customChar.userId == userId) {
                ChatCharacter.Custom(
                    id = customChar.id,
                    name = customChar.name,
                    avatar = customChar.avatar,
                    description = customChar.description,
                    personality = customChar.personality.toString(),
                    userId = customChar.userId
                )
            } else {
                null
            }
        }
    }
}
