package com.companion.cc.data.character

import com.companion.cc.domain.model.CharacterBookEntry
import com.companion.cc.domain.model.CharacterCard
import com.companion.cc.domain.model.CharacterCardData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Parser and serializer for Character Card V2/V3 JSON envelopes. */
object CharacterCardJsonCodec {
    private const val MAX_JSON_BYTES = 2 * 1024 * 1024
    private val json = Json { prettyPrint = true; prettyPrintIndent = "  "; ignoreUnknownKeys = true }

    fun parse(jsonText: String): Result<CharacterCard> = runCatching {
        require(jsonText.toByteArray(Charsets.UTF_8).size <= MAX_JSON_BYTES) { "角色卡文件过大" }
        val root = json.parseToJsonElement(jsonText).jsonObject
        val spec = root.string("spec") ?: "chara_card_v2"
        require(spec == "chara_card_v2" || spec == "chara_card_v3") { "不支持的角色卡格式" }
        val data = root["data"]?.jsonObject ?: root
        val name = data.string("name")?.trim().orEmpty()
        require(name.isNotBlank()) { "角色卡缺少名称" }
        CharacterCard(
            spec = spec,
            specVersion = root.string("spec_version") ?: if (spec == "chara_card_v3") "3.0" else "2.0",
            data = parseData(data, name)
        )
    }

    fun serialize(card: CharacterCard): String = json.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("spec", JsonPrimitive(card.spec))
            put("spec_version", JsonPrimitive(card.specVersion))
            put("data", dataToJson(card.data))
        }
    )

    private fun parseData(data: JsonObject, name: String) = CharacterCardData(
        name = name,
        description = data.string("description").orEmpty(),
        personality = data.string("personality").orEmpty(),
        scenario = data.string("scenario").orEmpty(),
        firstMessage = data.string("first_mes") ?: data.string("firstMessage").orEmpty(),
        alternateGreetings = data.stringList("alternate_greetings"),
        exampleMessages = data.string("mes_example").orEmpty(),
        creatorNotes = data.string("creator_notes").orEmpty(),
        systemPrompt = data.string("system_prompt").orEmpty(),
        postHistoryInstructions = data.string("post_history_instructions").orEmpty(),
        tags = data.stringList("tags"),
        creator = data.string("creator").orEmpty(),
        characterVersion = data.string("character_version") ?: "1.0",
        characterBook = parseBook(data["character_book"]?.jsonObject)
    )

    private fun dataToJson(data: CharacterCardData) = buildJsonObject {
        put("name", JsonPrimitive(data.name))
        put("description", JsonPrimitive(data.description))
        put("personality", JsonPrimitive(data.personality))
        put("scenario", JsonPrimitive(data.scenario))
        put("first_mes", JsonPrimitive(data.firstMessage))
        put("alternate_greetings", JsonArray(data.alternateGreetings.map(::JsonPrimitive)))
        put("mes_example", JsonPrimitive(data.exampleMessages))
        put("creator_notes", JsonPrimitive(data.creatorNotes))
        put("system_prompt", JsonPrimitive(data.systemPrompt))
        put("post_history_instructions", JsonPrimitive(data.postHistoryInstructions))
        put("tags", JsonArray(data.tags.map(::JsonPrimitive)))
        put("creator", JsonPrimitive(data.creator))
        put("character_version", JsonPrimitive(data.characterVersion))
        put("character_book", buildJsonObject {
            put("entries", JsonArray(data.characterBook.map { entryToJson(it) }))
        })
    }

    private fun parseBook(book: JsonObject?): List<CharacterBookEntry> {
        val entries = book?.get("entries")?.jsonArray ?: return emptyList()
        return buildList {
            entries.forEachIndexed { index, element ->
                val item = element.jsonObject
                val content = item.string("content").orEmpty()
                if (content.isNotBlank()) add(CharacterBookEntry(
                    keys = item.stringList("keys"),
                    content = content,
                    enabled = item.boolean("enabled") ?: true,
                    constant = item.boolean("constant") ?: false,
                    priority = item.int("priority") ?: 0,
                    insertionOrder = item.int("insertion_order") ?: index
                ))
            }
        }
    }

    private fun entryToJson(entry: CharacterBookEntry) = buildJsonObject {
        put("keys", JsonArray(entry.keys.map(::JsonPrimitive)))
        put("content", JsonPrimitive(entry.content))
        put("enabled", JsonPrimitive(entry.enabled))
        put("constant", JsonPrimitive(entry.constant))
        put("priority", JsonPrimitive(entry.priority))
        put("insertion_order", JsonPrimitive(entry.insertionOrder))
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
    private fun JsonObject.stringList(key: String): List<String> = this[key]?.jsonArray
        ?.mapNotNull { it.jsonPrimitive.contentOrNull?.trim()?.takeIf(String::isNotBlank) }
        .orEmpty()
    private fun JsonObject.boolean(key: String): Boolean? = this[key]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
    private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
}
