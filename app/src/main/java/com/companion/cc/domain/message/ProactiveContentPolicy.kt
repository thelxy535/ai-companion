package com.companion.cc.domain.message

import com.companion.cc.domain.character.InnerState

/**
 * Prevents proactive messages from asking the model to invent a new topic
 * when the relationship state does not contain a concrete conversational cue.
 */
object ProactiveContentPolicy {
    fun shouldGenerateWithModel(state: InnerState): Boolean = listOf(
        state.unfinishedThought,
        state.caringAbout,
        state.lookingForwardTo,
        state.lifeThread,
        state.emotionalAftertaste,
        state.currentNeed,
        state.lastInteractionMeaning
    ).any { it.isNotBlank() }
}
