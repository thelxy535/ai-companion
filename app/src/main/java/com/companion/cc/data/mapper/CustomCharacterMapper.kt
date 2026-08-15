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
            personality = serializePersonalityTraits(domain.personality),
            backstory = domain.backstory,
            greetingMessage = domain.greetingMessage,
            exampleDialogues = serializeExampleDialogues(domain.exampleDialogues),
            voiceConfig = domain.voiceConfig?.let { serializeVoiceConfig(it) },
            behaviorRules = domain.behaviorRules?.let { serializeBehaviorRules(it) },
            isCustom = domain.isCustom,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }

    // PersonalityTraits 序列化
    private fun serializePersonalityTraits(traits: PersonalityTraits): String {
        return JSONObject().apply {
            put("openness", traits.openness)
            put("conscientiousness", traits.conscientiousness)
            put("extraversion", traits.extraversion)
            put("agreeableness", traits.agreeableness)
            put("neuroticism", traits.neuroticism)
            put("customTraits", JSONObject(traits.customTraits))
        }.toString()
    }

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
            voiceId = obj.optString("voiceId", null)
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
