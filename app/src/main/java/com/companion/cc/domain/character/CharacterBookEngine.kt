package com.companion.cc.domain.character

import com.companion.cc.domain.model.CharacterBookEntry
import com.companion.cc.domain.model.CustomCharacter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Character Book 引擎（V9PM 第 6 项）。
 *
 * 关键词条目按当前消息触发注入，常驻条目始终注入；按 priority 排序、截断长度，
 * 作为独立区块追加到 system prompt，让角色知识库真正影响回复。
 */
@Singleton
class CharacterBookEngine @Inject constructor() {

    companion object {
        private const val MAX_CHARACTERS = 1200
    }

    data class BookInjection(
        val block: String,
        val triggeredKeys: List<String>
    )

    fun buildInjection(
        book: List<CharacterBookEntry>,
        currentMessage: String
    ): BookInjection {
        val normalized = currentMessage.lowercase()
        val matched = book.filter { entry ->
            entry.enabled && !entry.constant && entry.keys.any { normalized.contains(it.lowercase()) }
        }
        val constants = book.filter { it.enabled && it.constant }
        val selected = (constants + matched)
            .distinctBy { it.content }
            .sortedWith(compareByDescending<CharacterBookEntry> { it.priority }.thenBy { it.insertionOrder })
            .take(20)

        if (selected.isEmpty()) return BookInjection("", emptyList())

        val sb = StringBuilder("\n\n【角色知识库】")
        val triggeredKeys = mutableListOf<String>()
        for (entry in selected) {
            if (sb.length >= MAX_CHARACTERS) break
            val line = "- ${entry.content.trim()}"
            if (sb.length + line.length + 1 > MAX_CHARACTERS) break
            sb.append("\n").append(line)
            if (entry in matched) triggeredKeys.addAll(entry.keys)
        }
        return BookInjection(sb.toString(), triggeredKeys.distinct())
    }

    /** 常驻条目是否非空（供 UI 判断是否展示知识库区）。 */
    fun hasConstantEntries(book: List<CharacterBookEntry>): Boolean =
        book.any { it.enabled && it.constant }

    /** 从 CustomCharacter 读取知识库条目。 */
    fun entriesOf(character: CustomCharacter): List<CharacterBookEntry> = character.characterBook
}
