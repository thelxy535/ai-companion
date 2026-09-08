package com.companion.cc.domain.character

import org.junit.Assert.assertEquals
import org.junit.Test

class SceneContinuityPolicyTest {
    @Test
    fun `legacy scene becomes a structured snapshot`() {
        val previous = InnerState(scene = "在房间里，状态放松", currentActivity = "翻着书")

        val snapshot = previous.effectiveSceneState()

        assertEquals("房间", snapshot.room)
        assertEquals("翻着书", snapshot.activity)
    }

    @Test
    fun `ordinary mention does not teleport the scene`() {
        val previous = InnerState(scene = "在房间里，状态放松", currentActivity = "翻着书")
        val next = SceneContinuityPolicy.update(previous, "我才不想出门呢，先陪你聊会儿", null, 10L)

        assertEquals(previous.scene, next.scene)
    }

    @Test
    fun `explicit transition changes scene`() {
        val previous = InnerState(scene = "在房间里，状态放松")
        val next = SceneContinuityPolicy.update(previous, "我回到家了，刚把包放下", "把包放到椅背上", 10L)

        assertEquals("刚回到家", next.scene)
        assertEquals("把包放到椅背上", next.currentActivity)
        assertEquals("家", next.effectiveSceneState().location)
        assertEquals("把包放到椅背上", next.effectiveSceneState().activity)
    }

    @Test
    fun `without an action the previous activity remains`() {
        val previous = InnerState(scene = "在厨房", currentActivity = "烧水", recentAction = "看了一眼水壶")
        val next = SceneContinuityPolicy.update(previous, "嗯，听你说。", null, 10L)

        assertEquals("烧水", next.currentActivity)
        assertEquals("看了一眼水壶", next.recentAction)
    }

    @Test
    fun `negated location does not change scene`() {
        val previous = InnerState(scene = "在房间里")
        val next = SceneContinuityPolicy.update(previous, "我不在外面，还是在房间里陪你", null, 10L)

        assertEquals("在房间里", next.scene)
    }

    @Test
    fun `clothing and posture persist when the next reply does not mention them`() {
        val previous = InnerState(
            physicalScene = SceneState(
                location = "家",
                room = "卧室",
                posture = "坐着",
                clothing = "睡衣",
                heldItem = "杯子",
                activity = "看书"
            )
        )

        val next = SceneContinuityPolicy.update(previous, "嗯，我在听。", "抬眼看向你", 20L)

        assertEquals("睡衣", next.effectiveSceneState().clothing)
        assertEquals("坐着", next.effectiveSceneState().posture)
        assertEquals("杯子", next.effectiveSceneState().heldItem)
    }

    @Test
    fun `explicit dressing transition updates clothing without teleporting`() {
        val previous = InnerState(
            physicalScene = SceneState(location = "家", room = "卧室", posture = "坐着", clothing = "睡衣")
        )

        val next = SceneContinuityPolicy.update(previous, "我换上外套，准备出门。", "起身把外套穿好", 30L)

        assertEquals("外套", next.effectiveSceneState().clothing)
        assertEquals("站着", next.effectiveSceneState().posture)
        assertEquals("家", next.effectiveSceneState().location)
    }
}
