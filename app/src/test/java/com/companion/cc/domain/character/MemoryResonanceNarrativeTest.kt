package com.companion.cc.domain.character

import com.companion.cc.domain.model.Attitude
import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Mood
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryResonanceNarrativeTest {
    @Test
    fun `memory is reframed by mood instead of copied as a record`() {
        val narrative = MemoryResonanceNarrative.build(
            memory = "我们上次在雨里买了热饮",
            state = EmotionalState.default().copy(mood = Mood.SAD, attitude = Attitude.WARM),
            rhythm = CompanionRhythm(memoryStickiness = 0.8f)
        )

        assertTrue(narrative.contains("想起"))
        assertTrue(narrative.contains("雨里买了热饮"))
        assertFalse(narrative.contains("数据库"))
    }

    @Test
    fun `empty memory produces no forced resonance`() {
        assertTrue(
            MemoryResonanceNarrative.build(
                memory = " ",
                state = EmotionalState.default(),
                rhythm = CompanionRhythm()
            ).isBlank()
        )
    }
}
