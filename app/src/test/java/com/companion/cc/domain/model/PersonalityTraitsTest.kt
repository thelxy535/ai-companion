package com.companion.cc.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalityTraitsTest {

    @Test
    fun `natural description is stored without discarding existing custom traits`() {
        val original = PersonalityTraits.default().copy(
            customTraits = mapOf("习惯" to "先听完再回答")
        )

        val updated = original.withNaturalDescription("慢热，但熟悉以后会主动分享小事")

        assertEquals("慢热，但熟悉以后会主动分享小事", updated.naturalDescription())
        assertEquals("先听完再回答", updated.customTraits["习惯"])
        assertTrue(updated.customTraits.containsKey(PersonalityTraits.NATURAL_DESCRIPTION_KEY))
    }

    @Test
    fun `blank natural description removes only the natural description`() {
        val original = PersonalityTraits.default().copy(
            customTraits = mapOf(
                PersonalityTraits.NATURAL_DESCRIPTION_KEY to "旧的人格描述",
                "习惯" to "会记得小事"
            )
        )

        val updated = original.withNaturalDescription("   ")

        assertEquals("", updated.naturalDescription())
        assertEquals("会记得小事", updated.customTraits["习惯"])
        assertTrue(!updated.customTraits.containsKey(PersonalityTraits.NATURAL_DESCRIPTION_KEY))
    }
}
