package com.companion.cc.data.mapper

import com.companion.cc.data.local.entity.CustomCharacterEntity
import com.companion.cc.domain.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * CustomCharacter 和 CustomCharacterEntity 之间的转换
 * 使用 org.json 进行序列化，避免 kotlinx.serialization 问题
 */
object CustomCharacterMapper {

    fun toDomain(entity: CustomCharacterEntity): CustomCharacter {
        return CustomCharacter(
            id = entity.id,
            userId = entity.userId,
            name = entity.name,
            avatar = entity.avatar,
            description = entity.description,
            personality = parsePersonalityTraits(entity.personality),
            backstory = entity.backstory,
            greetingMessage = entity.greetingMessage,
            exampleDialogues = parseExampleDialogues(entity.exampleDialogues),
            voiceConfig = entity.voiceConfig?.let { parseVoiceConfig(it) },
            behaviorRules = entity.behaviorRules?.let { parseBehaviorRules(it) },
            scenario = entity.scenario,
            alternateGreetings = parseStringList(entity.alternateGreetings),
            creatorNotes = entity.creatorNotes,
            creator = entity.creator,
            characterVersion = entity.characterVersion,
            tags = parseStringList(entity.tags),
            systemPromptOverride = entity.systemPromptOverride,
            postHistoryInstructions = entity.postHistoryInstructions,
            characterBook = parseCharacterBook(entity.characterBook),
            rhythm = parseRhythm(entity.personality),
            isCustom = entity.isCustom,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(domain: CustomCharacter): CustomCharacterEntity {
        return CustomCharacterEntity(
            id = domain.id,
            userId = domain.userId,
            name = domain.name,
            avatar = domain.avatar,
            description = domain.description,
            personality = serializePersonalityTraits(domain.personality, domain.rhythm),
            backstory = domain.backstory,
            greetingMessage = domain.greetingMessage,
            exampleDialogues = serializeExampleDialogues(domain.exampleDialogues),
            voiceConfig = domain.voiceConfig?.let { serializeVoiceConfig(it) },
            behaviorRules = domain.behaviorRules?.let { serializeBehaviorRules(it) },
            scenario = domain.scenario,
            alternateGreetings = serializeStringList(domain.alternateGreetings),
            creatorNotes = domain.creatorNotes,
            creator = domain.creator,
            characterVersion = domain.characterVersion,
            tags = serializeStringList(domain.tags),
            systemPromptOverride = domain.systemPromptOverride,
            postHistoryInstructions = domain.postHistoryInstructions,
            characterBook = serializeCharacterBook(domain.characterBook),
            isCustom = domain.isCustom,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }

    private fun serializeStringList(values: List<String>): String = JSONArray(values).toString()

    private fun serializeCharacterBook(entries: List<com.companion.cc.domain.model.CharacterBookEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(JSONObject().apply {
                put("keys", JSONArray(entry.keys))
                put("content", entry.content)
                put("enabled", entry.enabled)
                put("constant", entry.constant)
                put("priority", entry.priority)
                put("insertionOrder", entry.insertionOrder)
            })
        }
        return array.toString()
    }

    private fun parseCharacterBook(json: String): List<com.companion.cc.domain.model.CharacterBookEntry> {
        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val obj = array.getJSONObject(index)
                val keys = mutableListOf<String>()
                val keysArray = obj.optJSONArray("keys")
                if (keysArray != null) {
                    for (i in 0 until keysArray.length()) keys.add(keysArray.getString(i))
                }
                com.companion.cc.domain.model.CharacterBookEntry(
                    keys = keys,
                    content = obj.optString("content", ""),
                    enabled = obj.optBoolean("enabled", true),
                    constant = obj.optBoolean("constant", false),
                    priority = obj.optInt("priority", 0),
                    insertionOrder = obj.optInt("insertionOrder", 0)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun parseStringList(json: String): List<String> {
        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { index -> array.getString(index) }
        }.getOrDefault(emptyList())
    }

    // PersonalityTraits 序列化
    private fun serializePersonalityTraits(
        traits: PersonalityTraits,
        rhythm: com.companion.cc.domain.character.CompanionRhythm = com.companion.cc.domain.character.CompanionRhythm()
    ): String {
        return JSONObject().apply {
            put("openness", traits.openness)
            put("conscientiousness", traits.conscientiousness)
            put("extraversion", traits.extraversion)
            put("agreeableness", traits.agreeableness)
            put("neuroticism", traits.neuroticism)
            put("customTraits", JSONObject(traits.customTraits))
            put("rhythm", JSONObject().apply {
                put("wakeHour", rhythm.wakeHour)
                put("sleepHour", rhythm.sleepHour)
                put("socialBattery", rhythm.socialBattery)
                put("memoryStickiness", rhythm.memoryStickiness)
                put("recoverySpeed", rhythm.recoverySpeed)
            })
        }.toString()
    }

    private fun parseRhythm(json: String): com.companion.cc.domain.character.CompanionRhythm =
        runCatching {
            val obj = JSONObject(json).optJSONObject("rhythm") ?: return@runCatching com.companion.cc.domain.character.CompanionRhythm()
            com.companion.cc.domain.character.CompanionRhythm(
                wakeHour = obj.optDouble("wakeHour", 8.0).toFloat(),
                sleepHour = obj.optDouble("sleepHour", 23.0).toFloat(),
                socialBattery = obj.optDouble("socialBattery", 0.65).toFloat(),
                memoryStickiness = obj.optDouble("memoryStickiness", 0.5).toFloat(),
                recoverySpeed = obj.optDouble("recoverySpeed", 0.5).toFloat()
            )
        }.getOrDefault(com.companion.cc.domain.character.CompanionRhythm())

    private fun parsePersonalityTraits(json: String): PersonalityTraits {
        val obj = JSONObject(json)
        val customTraitsJson = obj.optJSONObject("customTraits")
        val customTraits = mutableMapOf<String, String>()
        customTraitsJson?.keys()?.forEach { key ->
            customTraits[key] = customTraitsJson.getString(key)
        }
        return PersonalityTraits(
            openness = obj.getDouble("openness").toFloat(),
            conscientiousness = obj.getDouble("conscientiousness").toFloat(),
            extraversion = obj.getDouble("extraversion").toFloat(),
            agreeableness = obj.getDouble("agreeableness").toFloat(),
            neuroticism = obj.getDouble("neuroticism").toFloat(),
            customTraits = customTraits
        )
    }

    // ExampleDialogue 列表序列化
    private fun serializeExampleDialogues(dialogues: List<ExampleDialogue>): String {
        return JSONArray().apply {
            dialogues.forEach { dialogue ->
                put(JSONObject().apply {
                    put("user", dialogue.user)
                    put("assistant", dialogue.assistant)
                })
            }
        }.toString()
    }

    private fun parseExampleDialogues(json: String): List<ExampleDialogue> {
        val array = JSONArray(json)
        val result = mutableListOf<ExampleDialogue>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(ExampleDialogue(
                user = obj.getString("user"),
                assistant = obj.getString("assistant")
            ))
        }
        return result
    }

    // VoiceConfig 序列化
    private fun serializeVoiceConfig(config: VoiceConfig): String {
        return JSONObject().apply {
            put("pitch", config.pitch)
            put("speed", config.speed)
            put("volume", config.volume)
            put("voiceId", config.voiceId)
        }.toString()
    }

    private fun parseVoiceConfig(json: String): VoiceConfig {
        val obj = JSONObject(json)
        return VoiceConfig(
            pitch = obj.getDouble("pitch").toFloat(),
            speed = obj.getDouble("speed").toFloat(),
            volume = obj.getDouble("volume").toFloat(),
            voiceId = if (obj.isNull("voiceId")) null else obj.optString("voiceId").takeIf { it.isNotBlank() }
        )
    }

    // BehaviorRules 序列化
    private fun serializeBehaviorRules(rules: BehaviorRules): String {
        return JSONObject().apply {
            put("responseStyle", rules.responseStyle.name)
            put("emojiFrequency", rules.emojiFrequency.name)
            put("formalityLevel", rules.formalityLevel.name)
            put("topicPreferences", JSONArray(rules.topicPreferences))
            put("avoidTopics", JSONArray(rules.avoidTopics))
        }.toString()
    }

    private fun parseBehaviorRules(json: String): BehaviorRules {
        val obj = JSONObject(json)
        val topicPreferences = mutableListOf<String>()
        val topicPrefsArray = obj.getJSONArray("topicPreferences")
        for (i in 0 until topicPrefsArray.length()) {
            topicPreferences.add(topicPrefsArray.getString(i))
        }
        val avoidTopics = mutableListOf<String>()
        val avoidTopicsArray = obj.getJSONArray("avoidTopics")
        for (i in 0 until avoidTopicsArray.length()) {
            avoidTopics.add(avoidTopicsArray.getString(i))
        }
        return BehaviorRules(
            responseStyle = ResponseStyle.valueOf(obj.getString("responseStyle")),
            emojiFrequency = EmojiFrequency.valueOf(obj.getString("emojiFrequency")),
            formalityLevel = FormalityLevel.valueOf(obj.getString("formalityLevel")),
            topicPreferences = topicPreferences,
            avoidTopics = avoidTopics
        )
    }
}
