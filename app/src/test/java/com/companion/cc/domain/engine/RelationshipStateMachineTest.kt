package com.companion.cc.domain.engine

import com.companion.cc.domain.character.TemperamentProfile
import com.companion.cc.domain.model.Attitude
import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.EmotionalStateCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * V9 造人：态度状态机 / 时间衰减 / 持久化编解码
 */
class RelationshipStateMachineTest {

    private fun engine(temperament: TemperamentProfile = TemperamentProfile.DEFAULT): EmotionalEngine {
        return EmotionalEngine().also { it.setTemperament("c", temperament) }
    }

    @Test
    fun `strong repeated negativity provokes upset then cold war for stubborn character`() {
        val engine = engine(TemperamentProfile(stubbornness = 0.7f))
        engine.setCurrentSession("u", "c")

        engine.detectEmotion("真的很难过 生气 讨厌")   // 强负面 → 点着
        assertEquals(Attitude.UPSET, engine.getEmotionalState("u", "c").attitude)

        engine.detectEmotion("又来 讨厌 烦")            // 继续拱火 → 冷战
        assertEquals(Attitude.COLD, engine.getEmotionalState("u", "c").attitude)
    }

    @Test
    fun `gentle character does not escalate to cold war`() {
        val engine = engine(TemperamentProfile(stubbornness = 0.3f))
        engine.setCurrentSession("u", "c")

        engine.detectEmotion("难过 生气 讨厌")
        assertEquals(Attitude.UPSET, engine.getEmotionalState("u", "c").attitude)

        engine.detectEmotion("又来 讨厌 烦")            // 别扭度低 → 不进冷战
        assertEquals(Attitude.UPSET, engine.getEmotionalState("u", "c").attitude)
    }

    @Test
    fun `sincere apology repairs upset`() {
        val engine = engine(TemperamentProfile(stubbornness = 0.4f))
        engine.setCurrentSession("u", "c")

        engine.detectEmotion("难过 生气 讨厌")
        assertEquals(Attitude.UPSET, engine.getEmotionalState("u", "c").attitude)

        engine.detectEmotion("对不起，我错了，别生气")
        assertEquals(Attitude.NEUTRAL, engine.getEmotionalState("u", "c").attitude)
    }

    @Test
    fun `cold war softens to upset first not straight to neutral`() {
        val engine = engine(TemperamentProfile(stubbornness = 0.7f))
        engine.setCurrentSession("u", "c")

        engine.detectEmotion("难过 生气 讨厌")
        engine.detectEmotion("又来 讨厌 烦")
        assertEquals(Attitude.COLD, engine.getEmotionalState("u", "c").attitude)

        engine.detectEmotion("对不起，是我不好，抱抱")
        assertEquals(Attitude.UPSET, engine.getEmotionalState("u", "c").attitude)
    }

    @Test
    fun `strong positivity warms the attitude`() {
        val engine = engine()
        engine.setCurrentSession("u", "c")

        engine.detectEmotion("开心 高兴 快乐 喜欢 爱")
        assertEquals(Attitude.WARM, engine.getEmotionalState("u", "c").attitude)
    }

    @Test
    fun `time decay softens upset but cold war needs longer`() {
        val engine = engine(TemperamentProfile(grudgeDecay = 0.5f))
        engine.setCurrentSession("u", "c")

        val upsetFor2h = EmotionalState(
            mood = com.companion.cc.domain.model.Mood.CALM,
            energy = 0.6f, affection = 0.5f, stress = 0.6f,
            attitude = Attitude.UPSET,
            timestamp = System.currentTimeMillis() - 2L * 3_600_000L,
        )
        engine.setEmotionalState("u", "dc", upsetFor2h)
        assertEquals(Attitude.NEUTRAL, engine.applyTimeDecay("u", "dc").attitude)

        val coldFor2h = upsetFor2h.copy(attitude = Attitude.COLD)
        engine.setEmotionalState("u", "dc2", coldFor2h)
        assertEquals(Attitude.COLD, engine.applyTimeDecay("u", "dc2").attitude)
    }

    @Test
    fun `mild apology does not instantly fix cold war`() {
        val engine = engine(TemperamentProfile(stubbornness = 0.5f))
        engine.setCurrentSession("u", "c")

        engine.detectEmotion("难过 生气 讨厌")
        engine.detectEmotion("又来 讨厌 烦")
        assertEquals(Attitude.COLD, engine.getEmotionalState("u", "c").attitude)

        engine.detectEmotion("嗯")   // 敷衍的单字不算台阶
        assertEquals(Attitude.COLD, engine.getEmotionalState("u", "c").attitude)
    }

    @Test
    fun `codec roundtrip preserves full state`() {
        val state = EmotionalState(
            mood = com.companion.cc.domain.model.Mood.ANXIOUS,
            energy = 0.25f, affection = 0.8f, stress = 0.7f,
            attitude = Attitude.COLD, timestamp = 1234567890L,
        )
        val decoded = EmotionalStateCodec.decode(EmotionalStateCodec.encode(state))
        assertEquals(state, decoded)
    }

    @Test
    fun `codec handles legacy snapshots without attitude`() {
        val legacy = "${com.companion.cc.domain.model.Mood.CALM}|0.5|0.5|0.3"
        val decoded = EmotionalStateCodec.decode(legacy)
        assertNotNull(decoded)
        assertEquals(Attitude.NEUTRAL, decoded!!.attitude)
    }

    @Test
    fun `codec rejects garbage`() {
        assertNull(EmotionalStateCodec.decode(null))
        assertNull(EmotionalStateCodec.decode("not|a|state"))
    }
}
