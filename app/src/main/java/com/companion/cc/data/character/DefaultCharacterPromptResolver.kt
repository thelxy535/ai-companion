package com.companion.cc.data.character

import com.companion.cc.data.local.CompanionConfigLoader
import com.companion.cc.data.mapper.CustomCharacterPromptMapper
import com.companion.cc.domain.character.CharacterPromptResolver
import com.companion.cc.domain.character.ResolvedCharacterPrompt
import com.companion.cc.domain.character.UnknownCharacterException
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.repository.CustomCharacterRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultCharacterPromptResolver @Inject constructor(
    private val configLoader: CompanionConfigLoader,
    private val currentUserProvider: CurrentUserProvider,
    private val customCharacterRepository: CustomCharacterRepository,
) : CharacterPromptResolver {
    override suspend fun resolve(characterId: String): ResolvedCharacterPrompt {
        configLoader.getCompanionConfigStrict(characterId)?.let { config ->
            return ResolvedCharacterPrompt(
                config = config,
                source = com.companion.cc.domain.character.CharacterPromptSource.BuiltIn,
            )
        }

        val userId = currentUserProvider.requireUserId()
        val character = customCharacterRepository.getCharacterByIdForUser(characterId, userId)
            ?: throw UnknownCharacterException(characterId)
        return ResolvedCharacterPrompt(CustomCharacterPromptMapper.toCompanionConfig(character))
    }
}
