package com.companion.cc.domain.character

import com.companion.cc.domain.model.Attitude
import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Mood

/** Turns a recalled fact into an optional emotional cue without mutating the fact. */
object MemoryResonanceNarrative {
    fun build(memory: String, state: EmotionalState, rhythm: CompanionRhythm): String {
        val remembered = memory.trim().takeIf { it.isNotBlank() } ?: return ""
        val feeling = when {
            state.attitude == Attitude.COLD -> "这件事还压在心里，但现在不想主动展开"
            state.mood == Mood.SAD || state.mood == Mood.ANXIOUS -> "想起时有一点酸软，不必把它说得很完整"
            state.mood == Mood.HAPPY || state.mood == Mood.EXCITED -> "想起时心里亮了一下，可能顺手分享一点当时的细节"
            rhythm.memoryStickiness >= 0.7f -> "这件事在心里停得久一点，偶尔会自然浮上来"
            else -> "只是刚好联想到，不需要为了证明记得而转题"
        }
        return "- 这段经历：$remembered\n- 对它的感觉：$feeling"
    }
}
