package com.companion.cc.data.character

import com.companion.cc.domain.model.BehaviorRules
import com.companion.cc.domain.model.CharacterBookEntry
import com.companion.cc.domain.model.CustomCharacter
import com.companion.cc.domain.model.ExampleDialogue
import com.companion.cc.domain.model.PersonalityTraits
import com.companion.cc.domain.model.VoiceConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

enum class CharacterExportFormat(val extension: String, val mimeType: String, val label: String) {
    STANDARD_JSON("json", "application/json", "标准角色卡 JSON"),
    SYLORA_JSON("sylora.json", "application/json", "弦曜完整角色包"),
    MARKDOWN("md", "text/markdown", "Markdown 资料卡"),
    TEXT("txt", "text/plain", "纯文本资料卡")
}

/** Formats used for sharing characters without exposing user/database identifiers. */
object CharacterTransferCodec {
    private const val MAX_BYTES = 2 * 1024 * 1024
    private const val FORMAT = "sylora_character"
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    private data class SyloraCard(
        val format: String = FORMAT,
        val formatVersion: Int = 1,
        val name: String,
        val avatar: String? = null,
        val description: String = "",
        val personality: PersonalityTraits,
        val backstory: String = "",
        val greetingMessage: String = "",
        val exampleDialogues: List<ExampleDialogue> = emptyList(),
        val voiceConfig: VoiceConfig? = null,
        val behaviorRules: BehaviorRules? = null,
        val scenario: String = "",
        val alternateGreetings: List<String> = emptyList(),
        val creatorNotes: String = "",
        val creator: String = "",
        val characterVersion: String = "1.0",
        val tags: List<String> = emptyList(),
        val systemPromptOverride: String = "",
        val postHistoryInstructions: String = "",
        val characterBook: List<CharacterBookEntry> = emptyList()
    )

    fun serialize(character: CustomCharacter, format: CharacterExportFormat): String = when (format) {
        CharacterExportFormat.STANDARD_JSON -> CharacterCardJsonCodec.serialize(CharacterCardMapper.toCard(character))
        CharacterExportFormat.SYLORA_JSON -> json.encodeToString(toSyloraCard(character))
        CharacterExportFormat.MARKDOWN -> markdown(character)
        CharacterExportFormat.TEXT -> text(character)
    }

    fun fileName(character: CustomCharacter, format: CharacterExportFormat): String =
        "${safeName(character.name)}.${format.extension}"

    fun parse(raw: String, userId: String): Result<CustomCharacter> = runCatching {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "角色资料卡文件过大" }
        val trimmed = raw.trim().removePrefix("\uFEFF").trim()
        if (trimmed.startsWith("{")) {
            val root = json.parseToJsonElement(trimmed).jsonObject
            if (root["format"]?.jsonPrimitive?.content == FORMAT) {
                return@runCatching fromSyloraCard(json.decodeFromString<SyloraCard>(trimmed), userId)
            }
            return@runCatching CharacterCardMapper.toCustomCharacter(
                CharacterCardJsonCodec.parse(trimmed).getOrThrow(), userId
            )
        }
        fromText(trimmed, userId)
    }

    private fun toSyloraCard(c: CustomCharacter) = SyloraCard(
        name = c.name, avatar = c.avatar, description = c.description, personality = c.personality,
        backstory = c.backstory, greetingMessage = c.greetingMessage,
        exampleDialogues = c.exampleDialogues, voiceConfig = c.voiceConfig,
        behaviorRules = c.behaviorRules, scenario = c.scenario,
        alternateGreetings = c.alternateGreetings, creatorNotes = c.creatorNotes,
        creator = c.creator, characterVersion = c.characterVersion, tags = c.tags,
        systemPromptOverride = c.systemPromptOverride,
        postHistoryInstructions = c.postHistoryInstructions, characterBook = c.characterBook
    )

    private fun fromSyloraCard(c: SyloraCard, userId: String) = CustomCharacter(
        id = UUID.randomUUID().toString(), userId = userId, name = c.name.trim(), avatar = c.avatar,
        description = c.description, personality = c.personality,
        backstory = c.backstory, greetingMessage = c.greetingMessage,
        exampleDialogues = c.exampleDialogues.take(20), voiceConfig = c.voiceConfig,
        behaviorRules = c.behaviorRules ?: BehaviorRules.default(), scenario = c.scenario,
        alternateGreetings = c.alternateGreetings.take(20), creatorNotes = c.creatorNotes,
        creator = c.creator, characterVersion = c.characterVersion, tags = c.tags.take(50),
        systemPromptOverride = c.systemPromptOverride,
        postHistoryInstructions = c.postHistoryInstructions, characterBook = c.characterBook.take(100)
    )

    private fun markdown(c: CustomCharacter): String = buildString {
        appendLine("# ${c.name}")
        section("描述", c.description)
        section("背景", c.backstory)
        section("场景", c.scenario)
        section("开场白", c.greetingMessage)
        if (c.alternateGreetings.isNotEmpty()) section("备用开场白", c.alternateGreetings.joinToString("\n- ", prefix = "- "))
        section("人格", c.personality.customTraits.entries.joinToString("\n") { "- ${it.key}：${it.value}" })
        section("创作者备注", c.creatorNotes)
        if (c.tags.isNotEmpty()) appendLine("**标签：** ${c.tags.joinToString("、")}")
        if (c.exampleDialogues.isNotEmpty()) {
            appendLine("\n## 示例对话")
            c.exampleDialogues.forEach { appendLine("\n**用户：** ${it.user}\n**${c.name}：** ${it.assistant}") }
        }
        if (c.characterBook.isNotEmpty()) {
            appendLine("\n## Character Book")
            c.characterBook.forEach { appendLine("\n### ${it.keys.joinToString("、")}\n${it.content}") }
        }
    }

    private fun text(c: CustomCharacter): String = buildString {
        appendLine("角色名：${c.name}")
        line("描述", c.description); line("背景", c.backstory); line("场景", c.scenario)
        line("开场白", c.greetingMessage); line("创作者备注", c.creatorNotes)
        if (c.tags.isNotEmpty()) line("标签", c.tags.joinToString("、"))
        if (c.exampleDialogues.isNotEmpty()) {
            appendLine("\n示例对话：")
            c.exampleDialogues.forEach { appendLine("用户：${it.user}\n${c.name}：${it.assistant}\n") }
        }
    }

    private fun fromText(raw: String, userId: String): CustomCharacter {
        val lines = raw.lines()
        fun value(label: String): String {
            val line = lines.firstOrNull {
                it.trim().startsWith("$label：") || it.trim().startsWith("**$label：**")
            } ?: return ""
            return line.trim()
                .removePrefix("**$label：**")
                .removePrefix("$label：")
                .trim()
        }
        fun section(title: String): String {
            val start = lines.indexOfFirst { it.trim() == "## $title" }
            if (start < 0) return ""
            return lines.drop(start + 1)
                .takeWhile { !it.trim().startsWith("#") }
                .joinToString("\n") { it.removePrefix("- ").trim() }
                .trim()
        }
        fun field(label: String): String = value(label).ifBlank { section(label) }
        val name = lines.firstOrNull { it.startsWith("# ") }?.removePrefix("# ")?.trim()
            ?: value("角色名")
        require(name.isNotBlank()) { "资料卡缺少角色名" }
        return CustomCharacter(
            id = UUID.randomUUID().toString(), userId = userId, name = name, avatar = null,
            description = field("描述"), personality = PersonalityTraits.default(),
            backstory = field("背景"), greetingMessage = field("开场白").ifBlank { "你好，很高兴见到你！" },
            exampleDialogues = emptyList(), voiceConfig = null, behaviorRules = BehaviorRules.default(),
            scenario = field("场景"), creatorNotes = field("创作者备注"), tags = field("标签")
                .split("、", ",").map(String::trim).filter(String::isNotBlank).take(50)
        )
    }

    private fun StringBuilder.section(title: String, value: String) { if (value.isNotBlank()) appendLine("\n## $title\n$value") }
    private fun StringBuilder.line(label: String, value: String) { if (value.isNotBlank()) appendLine("$label：$value") }
    private fun safeName(name: String) = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "character" }
}
