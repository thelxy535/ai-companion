package com.companion.cc.domain.chat

import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole

/** Builds a model context independently from the UI message window. */
object ConversationContextBuilder {
    fun build(
        messages: List<Message>,
        maxMessages: Int = 24,
        maxCharacters: Int = 12_000,
        maxTokens: Int = 3_000,
        relevantMessageIds: Set<String> = emptySet()
    ): List<Map<String, String>> {
        if (messages.isEmpty() || maxMessages <= 0 || maxCharacters <= 0) return emptyList()

        val ordered = messages
            .filter { it.content.isNotBlank() }
            .distinctBy { it.id }
            .sortedWith(compareBy<Message> { it.timestamp }.thenBy { it.id })
        if (ordered.isEmpty()) return emptyList()

        val selected = LinkedHashSet<String>()
        val latestUserIndex = ordered.indexOfLast { it.role == MessageRole.USER }

        // Keep the latest exchange together. This also preserves a proactive assistant
        // message immediately before the user's first reply, even when it is old in the UI.
        if (latestUserIndex >= 0) {
            selected += ordered[latestUserIndex].id
            ordered.firstOrNull { it.id == ordered[latestUserIndex].replyToId }?.let {
                selected += it.id
            }
            var index = latestUserIndex - 1
            while (index >= 0 && ordered[index].role == MessageRole.ASSISTANT) {
                selected += ordered[index].id
                index--
            }
            var next = latestUserIndex + 1
            while (next < ordered.size && ordered[next].role == MessageRole.ASSISTANT) {
                selected += ordered[next].id
                next++
            }
        }

        // A relevant older message is more useful than an arbitrary high-importance message.
        ordered.filter { it.id in relevantMessageIds }
            .sortedWith(compareByDescending<Message> { it.importance }.thenByDescending { it.timestamp })
            .forEach { if (selected.size < maxMessages) selected += it.id }

        ordered.asReversed().forEach { message ->
            if (selected.size < maxMessages) selected += message.id
        }

        val result = ordered.filter { it.id in selected }.takeLast(maxMessages).toMutableList()
        while (
            (result.sumOf { it.content.length } > maxCharacters ||
                estimateTokens(result.joinToString("\n") { it.content }) > maxTokens) &&
            result.size > 1
        ) {
            val protectedIds = setOfNotNull(
                ordered.getOrNull(latestUserIndex)?.id,
                ordered.getOrNull(latestUserIndex - 1)?.id
            ) + relevantMessageIds
            val removable = result.indexOfFirst { it.id !in protectedIds }
            result.removeAt(if (removable >= 0) removable else 0)
        }
        return result.map { message ->
            mapOf(
                "role" to if (message.role == MessageRole.USER) "user" else "assistant",
                "content" to message.content
            )
        }
    }

    /** Conservative estimate for mixed Chinese/Latin prompts without a tokenizer dependency. */
    fun estimateTokens(text: String): Int {
        var tokens = 0
        var asciiRun = 0
        text.forEach { char ->
            if (char.code <= 0x7f) {
                asciiRun++
            } else {
                tokens += (asciiRun + 3) / 4
                asciiRun = 0
                tokens++
            }
        }
        return tokens + (asciiRun + 3) / 4
    }
}
