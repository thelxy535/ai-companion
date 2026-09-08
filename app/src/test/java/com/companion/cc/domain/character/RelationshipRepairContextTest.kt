package com.companion.cc.domain.character

import com.companion.cc.domain.model.Attitude
import com.companion.cc.domain.model.EmotionalState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelationshipRepairContextTest {

    @Test
    fun `cold relationship keeps distance and leaves a gradual repair path`() {
        val context = RelationshipRepairContext.build(
            emotionalState = EmotionalState.default().copy(attitude = Attitude.COLD),
            innerState = InnerState(relationshipStage = "需要修复"),
            temperament = TemperamentProfile(stubbornness = 0.8f)
        )

        assertTrue(context.contains("没有翻篇"))
        assertTrue(context.contains("逐渐"))
        assertFalse(context.contains("已经完全和好"))
    }

    @Test
    fun `neutral relationship does not manufacture a conflict`() {
        val context = RelationshipRepairContext.build(
            emotionalState = EmotionalState.default(),
            innerState = InnerState(relationshipStage = "熟悉"),
            temperament = TemperamentProfile.DEFAULT
        )

        assertTrue(context.isBlank())
    }

    @Test
    fun `upset relationship leaves a softer path than cold war`() {
        val context = RelationshipRepairContext.build(
            emotionalState = EmotionalState.default().copy(attitude = Attitude.UPSET),
            innerState = InnerState(relationshipStage = "需要修复"),
            temperament = TemperamentProfile(stubbornness = 0.4f)
        )

        assertTrue(context.contains("不是拒绝交流"))
        assertTrue(context.contains("慢慢软下来"))
    }
}
