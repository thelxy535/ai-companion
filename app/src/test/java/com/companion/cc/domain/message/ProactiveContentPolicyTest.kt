package com.companion.cc.domain.message

import com.companion.cc.domain.character.InnerState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProactiveContentPolicyTest {

    @Test
    fun `does not ask the model to invent a topic without a continuity cue`() {
        assertFalse(ProactiveContentPolicy.shouldGenerateWithModel(InnerState()))
    }

    @Test
    fun `allows model generation when the character has an unfinished thought`() {
        assertTrue(
            ProactiveContentPolicy.shouldGenerateWithModel(
                InnerState(unfinishedThought = "想把昨晚没说完的电影结局讲完")
            )
        )
    }
}
