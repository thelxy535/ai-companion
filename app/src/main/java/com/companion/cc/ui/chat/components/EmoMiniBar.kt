package com.companion.cc.ui.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Mood

/**
 * V9PM emo-mini：顶栏下方六段（现四段）情绪彩条 + 点击展开 glass 面板。
 * 规格：margin 0 20px 6px · 高 4px · 圆角 99 · hairline 内描边 · 面板 m-rise 240ms。
 * 数据源：情绪引擎实时状态（好感/心情/压力/精力）。
 * 信任/兴趣引擎尚无数据源——待设计方定义后补段（v2）。
 */
private data class EmoSeg(val label: String, val value: Float, val colorDay: Color, val colorNight: Color)

@Composable
fun EmoMiniBar(
    stateFlow: kotlinx.coroutines.flow.StateFlow<EmotionalState>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by stateFlow.collectAsState()
    val night = com.companion.cc.ui.theme.LocalVisualTheme.current.tokens.backdrop.isDark
    var panelOpen by remember { mutableStateOf(false) }
    val togglePanel = { panelOpen = !panelOpen }

    val moodScore = when (state.mood) {
        Mood.EXCITED -> 0.95f
        Mood.HAPPY -> 0.85f
        Mood.CONTENT -> 0.75f
        Mood.CALM -> 0.60f
        Mood.ANXIOUS -> 0.40f
        Mood.SAD -> 0.30f
        Mood.TIRED -> 0.25f
    }

    val segments = listOf(
        EmoSeg("好感", state.affection, Color(0xFFE0728F), Color(0xFFE88AA4)),
        EmoSeg("心情", moodScore, Color(0xFF4F80E8), Color(0xFF6D9BF1)),
        EmoSeg("压力", state.stress, Color(0xFFE0906B), Color(0xFFE8A885)),
        EmoSeg("精力", state.energy, Color(0xFF57B894), Color(0xFF6FCBA8)),
    )
    val hairline = if (night) Color(0x29FFFFFF) else Color(0xCCFFFFFF)

    Column(modifier = modifier) {
        // emo-mini 条
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 6.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(99.dp))
                .clickable { panelOpen = !panelOpen }
                .background(hairline.copy(alpha = if (night) 0.16f else 0.35f))
                .clickable { panelOpen = !panelOpen },
            verticalAlignment = Alignment.CenterVertically
        ) {
            segments.forEach { seg ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(seg.colorDay.takeIf { !night } ?: seg.colorNight)
                )
            }
        }
        // emo-panel：glass 面板（m-rise 240ms bezier(.2,0,0,1)）
        AnimatedVisibility(
            visible = panelOpen,
            enter = fadeIn(tween(240)) + expandVertically(tween(240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))),
            exit = fadeOut(tween(160)) + shrinkVertically(tween(160)),
        ) {
            val sheen = if (night) Color(0xFF1C1A30).copy(alpha = 0.42f) else Color(0xFFFFFFFF).copy(alpha = 0.38f)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(sheen)
                    .border(1.dp, hairline, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                segments.forEach { seg ->
                    val v = (seg.value.coerceIn(0f, 1f) * 100).toInt()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(seg.label, fontSize = 11.sp, color = if (night) Color(0xFF9AA5BD) else Color(0xFF5A6578), modifier = Modifier.weight(44f))
                        Box(
                            modifier = Modifier
                                .weight(34f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(hairline.copy(alpha = if (night) 0.16f else 0.35f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(seg.value.coerceIn(0.02f, 1f))
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(seg.colorDay.takeIf { !night } ?: seg.colorNight)
                            )
                        }
                        Spacer(Modifier.weight(10f))
                        Text(
                            "$v",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (night) Color(0xFFEBF0FA) else Color(0xFF1E2434),
                            modifier = Modifier.weight(12f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Right
                        )
                    }
                }
            }
        }
    }
}
