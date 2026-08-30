package com.companion.cc.domain.character

import com.companion.cc.domain.memory.MemoryCapsuleV2
import com.companion.cc.domain.memory.MemoryCapsuleV2Validator
import com.companion.cc.domain.memory.MemoryScopeKey
import com.companion.cc.domain.model.BehaviorRules
import com.companion.cc.domain.model.CustomCharacter
import com.companion.cc.domain.model.ExampleDialogue
import com.companion.cc.domain.model.PersonalityTraits
import com.companion.cc.domain.model.VoiceConfig
import com.google.gson.Gson
import com.google.gson.JsonParser

/** Data retained for revival; conversation history is deliberately excluded. */
data class CharacterMemoryCapsulePayload(
    val sourceCharacterId: String,
    val name: String,
    val serialized: String
)

data class CharacterMemoryCapsuleRestored(
    val character: CustomCharacter,
    val memorySnapshot: MemoryCapsuleV2?,
    val sourceCharacterId: String? = null
)

class CharacterMemoryCapsuleBuilder(
    private val gson: Gson = Gson()
) {
    fun build(character: CustomCharacter): CharacterMemoryCapsulePayload =
        build(character, memorySnapshot = null)

    fun build(
        character: CustomCharacter,
        memorySnapshot: MemoryCapsuleV2?
    ): CharacterMemoryCapsulePayload {
        validateSnapshot(character, memorySnapshot)
        val envelope = CharacterMemoryCapsuleEnvelope(
            sourceCharacterId = character.id,
            configuration = character.toCapsuleConfiguration(),
            memorySnapshot = memorySnapshot
        )
        return CharacterMemoryCapsulePayload(
            sourceCharacterId = character.id,
            name = character.name,
            serialized = gson.toJson(envelope)
        )
    }

    fun restore(
        serialized: String,
        userId: String,
        characterId: String
    ): CustomCharacter = restorePayload(serialized, userId, characterId).character

    fun restorePayload(
        serialized: String,
        userId: String,
        characterId: String
    ): CharacterMemoryCapsuleRestored {
        val root = JsonParser().parse(serialized)
        require(root.isJsonObject) { "Capsule payload is corrupted" }
        val objectRoot = root.asJsonObject
        val envelope = if (objectRoot.has("capsuleFormat")) {
            gson.fromJson(objectRoot, CharacterMemoryCapsuleEnvelope::class.java)
                ?: throw IllegalArgumentException("Capsule payload is corrupted")
        } else {
            null
        }
        val configuration = envelope?.configuration
            ?: gson.fromJson(objectRoot, CapsuleCharacterConfiguration::class.java)
            ?: throw IllegalArgumentException("Capsule payload is corrupted")
        val memorySnapshot = envelope?.memorySnapshot
        if (envelope != null) {
            require(envelope.capsuleFormat == CHARACTER_CAPSULE_FORMAT) {
                "Unsupported character capsule format"
            }
            require(envelope.schemaVersion == CHARACTER_CAPSULE_SCHEMA_VERSION) {
                "Unsupported character capsule schema version"
            }
            require(envelope.sourceCharacterId.isNotBlank()) {
                "Capsule source character ID is required"
            }
        }
        validateSnapshot(
            userId = userId,
            sourceCharacterId = envelope?.sourceCharacterId,
            memorySnapshot = memorySnapshot
        )
        return CharacterMemoryCapsuleRestored(
            character = configuration.toCharacter(userId, characterId),
            memorySnapshot = memorySnapshot,
            sourceCharacterId = envelope?.sourceCharacterId
        )
    }

    private fun validateSnapshot(
        character: CustomCharacter,
        memorySnapshot: MemoryCapsuleV2?
    ) {
        validateSnapshot(character.userId, character.id, memorySnapshot)
    }

    private fun validateSnapshot(
        userId: String,
        sourceCharacterId: String?,
        memorySnapshot: MemoryCapsuleV2?
    ) {
        if (memorySnapshot == null) return
        MemoryCapsuleV2Validator.validate(memorySnapshot).getOrThrow()
        require(MemoryScopeKey.belongsToUser(memorySnapshot.scopeKey, userId)) {
            "Memory capsule does not belong to user"
        }
        if (sourceCharacterId != null) {
            require(memorySnapshot.scopeKey == MemoryScopeKey.forCharacter(userId, sourceCharacterId)) {
                "Memory capsule does not belong to source character"
            }
        }
    }

    private fun CustomCharacter.toCapsuleConfiguration() = CapsuleCharacterConfiguration(
        name = name,
        avatar = avatar,
        description = description,
        personality = personality,
        backstory = backstory,
        greetingMessage = greetingMessage,
        exampleDialogues = exampleDialogues,
        voiceConfig = voiceConfig,
        behaviorRules = behaviorRules
    )
}

private const val CHARACTER_CAPSULE_FORMAT = "cc-switch.character-capsule"
private const val CHARACTER_CAPSULE_SCHEMA_VERSION = 2

private data class CharacterMemoryCapsuleEnvelope(
    val capsuleFormat: String = CHARACTER_CAPSULE_FORMAT,
    val schemaVersion: Int = CHARACTER_CAPSULE_SCHEMA_VERSION,
    val sourceCharacterId: String,
    val configuration: CapsuleCharacterConfiguration,
    val memorySnapshot: MemoryCapsuleV2? = null
)

private data class CapsuleCharacterConfiguration(
    val name: String,
    val avatar: String?,
    val description: String,
    val personality: PersonalityTraits,
    val backstory: String,
    val greetingMessage: String,
    val exampleDialogues: List<ExampleDialogue>,
    val voiceConfig: VoiceConfig?,
    val behaviorRules: BehaviorRules?
) {
    fun toCharacter(userId: String, characterId: String) = CustomCharacter(
        id = characterId,
        userId = userId,
        name = name,
        avatar = avatar,
        description = description,
        personality = personality,
        backstory = backstory,
        greetingMessage = greetingMessage,
        exampleDialogues = exampleDialogues,
        voiceConfig = voiceConfig,
        behaviorRules = behaviorRules
    )
}
