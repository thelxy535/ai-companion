package com.companion.cc.domain.message

import com.companion.cc.domain.character.SceneState

data class ParsedMessageContent(
    val dialogue: String,
    val action: String?,
    val scene: SceneState? = null
)

/** Keeps assistant dialogue and stage direction separate for every message entry point. */
object MessageContentParser {
    private val actionRegex = """\[(?:动作|ACTION|stage\s*direction)[:：]\s*([^\]]+)\]""".toRegex(RegexOption.IGNORE_CASE)
    private val incompleteActionRegex = """\[(?:动作|ACTION|stage\s*direction)[:：][^\]]*$""".toRegex(RegexOption.IGNORE_CASE)
    private val sceneRegex = """\[(?:场景|SCENE)[:：]\s*([^\]]+)\]""".toRegex(RegexOption.IGNORE_CASE)
    private val incompleteSceneRegex = """\[(?:场景|SCENE)[:：][^\]]*$""".toRegex(RegexOption.IGNORE_CASE)
    private val markdownActionRegex = """^(?:\*\s*(.+?)\s*\*|_\s*(.+?)\s*_)$""".toRegex()
    private val leadingMarkdownActionRegex = """^\s*(?:\*\s*(.+?)\s*\*|_\s*(.+?)\s*_)\s+(.+)$""".toRegex()
    private val leadingParenRegex = """^\s*[（(]([^（）()]+)[）)]\s*(.*)$""".toRegex()
    private val trailingParenRegex = """^(.*?)[（(]([^（）()]+)[）)]\s*$""".toRegex()
    private val actionCueRegex = """(笑|挥|抬|低头|点头|看|望|握|靠|伸|放|拿|走|坐|站|轻|皱|眨|叹|捏|推|起身|转身|靠近|后退|眯|歪|贴|抱|摸|拍|递|喝|咬|沉默|呼吸|垂|耸)""".toRegex()

    fun parse(content: String): ParsedMessageContent {
        val actions = mutableListOf<String>()
        val parsedScene = sceneRegex.findAll(content).lastOrNull()?.groupValues?.get(1)?.let(::parseScene)
        actionRegex.findAll(content).forEach { match ->
            match.groupValues[1].trim().takeIf { it.isNotBlank() }?.let(actions::add)
        }
        var dialogue = content.replace(actionRegex, "")
            .replace(incompleteActionRegex, "")
            .replace(sceneRegex, "")
            .replace(incompleteSceneRegex, "")

        // A complete italic line is a common roleplay convention. Only accept
        // a whole line so emphasis inside ordinary dialogue stays untouched.
        dialogue = dialogue.lines().mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("*") && trimmed.indexOf('*', startIndex = 1) < 0 ||
                trimmed.startsWith("_") && trimmed.indexOf('_', startIndex = 1) < 0) {
                return@mapNotNull null
            }
            val markdown = markdownActionRegex.matchEntire(trimmed)
            if (markdown != null) {
                val action = (markdown.groupValues[1].ifBlank { markdown.groupValues[2] }).trim()
                if (action.isActionLike()) {
                    actions.add(action)
                    null
                } else line
            } else {
                val leadingMarkdown = leadingMarkdownActionRegex.matchEntire(trimmed)
                if (leadingMarkdown != null) {
                    val action = leadingMarkdown.groupValues[1]
                        .ifBlank { leadingMarkdown.groupValues[2] }
                        .trim()
                    if (action.isActionLike()) {
                        actions.add(action)
                        leadingMarkdown.groupValues[3].trim()
                    } else {
                        line
                    }
                } else {
                val leading = leadingParenRegex.matchEntire(line)
                val trailing = trailingParenRegex.matchEntire(line)
                when {
                    leading != null && leading.groupValues[2].isBlank() && leading.groupValues[1].isActionLike() -> {
                        actions.add(leading.groupValues[1].trim())
                        null
                    }
                    leading != null && leading.groupValues[2].isNotBlank() && leading.groupValues[1].isActionLike() -> {
                        actions.add(leading.groupValues[1].trim())
                        leading.groupValues[2]
                    }
                    trailing != null && trailing.groupValues[1].isNotBlank() && trailing.groupValues[2].isActionLike() -> {
                        actions.add(trailing.groupValues[2].trim())
                        trailing.groupValues[1]
                    }
                    else -> line
                }
                }
            }
        }.joinToString("\n")

        // Preserve parentheses that are part of a sentence, e.g. "我（其实...）".
        dialogue = dialogue.trim()
        val normalizedActions = actions.map { it.replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotBlank() }
            .distinct()
        return ParsedMessageContent(
            dialogue = dialogue.ifBlank { if (normalizedActions.isNotEmpty()) "……" else "" },
            action = normalizedActions.takeIf { it.isNotEmpty() }?.joinToString("，"),
            scene = parsedScene
        )
    }

    private fun parseScene(raw: String): SceneState? {
        val fields = raw.split(';', '；', ',', '，').mapNotNull { part ->
            val pair = part.split('=', '：', ':', limit = 2)
            if (pair.size != 2) null else pair[0].trim().lowercase() to cleanSceneValue(pair[1])
        }.toMap()
        fun value(vararg names: String): String = names.firstNotNullOfOrNull { fields[it] } ?: ""
        val scene = SceneState(
            location = value("地点", "location"),
            room = value("房间", "room"),
            posture = value("姿态", "posture"),
            clothing = value("穿着", "clothing", "衣着"),
            heldItem = value("手持", "held", "helditem", "物品"),
            activity = value("活动", "activity", "正在做")
        )
        return scene.takeUnless { it.isEmpty() }
    }

    private fun cleanSceneValue(raw: String): String = raw.trim()
        .takeUnless { it.equals("无", true) || it.equals("none", true) || it == "-" }
        .orEmpty()

    private fun String.isActionLike(): Boolean {
        val text = trim()
        return text.isNotBlank() &&
            !text.endsWith("？") && !text.endsWith("?") &&
            !text.endsWith("！") && !text.endsWith("!") &&
            actionCueRegex.containsMatchIn(text)
    }
}
