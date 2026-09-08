package com.companion.cc.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt

/** Scene geometry shared by glass surfaces so their backdrop replicas align. */
@Stable
private class GlassSceneState {
    var size by mutableStateOf(IntSize.Zero)
    var coordinates: LayoutCoordinates? by mutableStateOf(null)

    fun update(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        size = coordinates.size
    }

    fun offsetOf(surfaceCoordinates: LayoutCoordinates): Offset {
        val sceneCoordinates = coordinates
        return if (sceneCoordinates != null && sceneCoordinates.isAttached) {
            // Dialogs and popups are rendered in a separate Compose hierarchy.
            // Compose throws when localPositionOf crosses that boundary; those
            // surfaces should use the local fallback recipe instead of crashing.
            runCatching {
                sceneCoordinates.localPositionOf(surfaceCoordinates, Offset.Zero)
            }.getOrDefault(Offset.Zero)
        } else {
            Offset.Zero
        }
    }
}

private val LocalGlassScene = staticCompositionLocalOf<GlassSceneState?> { null }

internal object GlassTouchOptics {
    fun replicaOffset(sceneOffset: Offset): IntOffset = IntOffset(
        x = -sceneOffset.x.roundToInt(),
        y = -sceneOffset.y.roundToInt()
    )

    fun clampPoint(x: Float, y: Float, width: Float, height: Float): Offset = Offset(
        x.coerceIn(0f, width),
        y.coerceIn(0f, height)
    )

    fun opposingPoint(x: Float, y: Float, width: Float, height: Float): Offset = Offset(
        (width - x).coerceIn(0f, width),
        (height - y).coerceIn(0f, height)
    )

    fun edgeProximity(x: Float, y: Float, width: Float, height: Float, band: Float): Float {
        val distance = minOf(x, y, width - x, height - y)
        return ((band - distance) / band).coerceIn(0f, 1f)
    }
}

/**
 * Single application-owned backdrop. Route scenes provide theme metadata only;
 * this host keeps the full-screen layer alive while content transitions.
 */
@Composable
fun AppBackdropHost(
    route: VisualRoute,
    companionId: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val rootTheme = LocalVisualTheme.current
    val customization = LocalVisualCustomization.current
    val effectTier = rootTheme.effectTier
    val backdrop = remember(
        rootTheme.tokens.backdrop.isDark,
        route,
        companionId,
        customization,
        effectTier
    ) {
        VisualThemeResolver.resolve(
            isDark = rootTheme.tokens.backdrop.isDark,
            route = route,
            companionId = companionId,
            customization = customization,
            effectTier = effectTier
        ).tokens.backdrop
    }

    Box(modifier = modifier.fillMaxSize()) {
        AdaptiveBackdropLayer(
            backdrop = backdrop,
            modifier = Modifier.matchParentSize()
        )
        content()
    }
}

/**
 * Route-scoped visual root. It provides a companion-aware theme to child glass
 * surfaces while the application shell owns the full-screen backdrop.
 */
@Composable
fun VisualScene(
    route: VisualRoute,
    companionId: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val rootTheme = LocalVisualTheme.current
    val customization = LocalVisualCustomization.current
    val routeTheme = remember(
        rootTheme.tokens.backdrop.isDark,
        rootTheme.effectTier,
        route,
        companionId,
        customization
    ) {
        VisualThemeResolver.resolve(
            isDark = rootTheme.tokens.backdrop.isDark,
            route = route,
            companionId = companionId,
            customization = customization,
            effectTier = rootTheme.effectTier
        )
    }
    val sceneState = remember { GlassSceneState() }
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes

    CompositionLocalProvider(
        LocalVisualTheme provides routeTheme,
        LocalGlassScene provides sceneState
    ) {
        MaterialTheme(
            colorScheme = routeTheme.materialColors,
            typography = typography,
            shapes = shapes
        ) {
            // 场景级默认文字色：未显式给色的 Text 跟随路由主题（V9PM）。
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides routeTheme.tokens.contentPrimary
            ) {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .onGloballyPositioned(sceneState::update)
                ) {
                    content()
                }
            }
        }
    }
}

/** Draws the controlled route backdrop once; glass surfaces can reproduce it safely. */
@Composable
fun AdaptiveBackdropLayer(
    backdrop: BackdropSpec,
    modifier: Modifier = Modifier
) {
    // Theme toggle: 800ms ease matching the global aurora rhythm.
    // Route navigation lands on the new color immediately — animateColorAsState snaps
    // when recomposition outpaces the animation, which is the correct nav behavior.
    val animSpec = androidx.compose.animation.core.tween<androidx.compose.ui.graphics.Color>(
        800, easing = androidx.compose.animation.core.LinearEasing
    )
    val startColor by androidx.compose.animation.animateColorAsState(
        backdrop.baseStart, animSpec, label = "backdropStart"
    )
    val endColor by androidx.compose.animation.animateColorAsState(
        backdrop.baseEnd, animSpec, label = "backdropEnd"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(startColor, endColor),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )
        }

        val userBackdrop = backdrop.userBackdrop
        if (userBackdrop.managedImageReference != null) {
            AsyncImage(
                model = userBackdrop.managedImageReference,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = userBackdrop.imageOpacity,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (backdrop.isDark) {
                        androidx.compose.ui.graphics.Color.Black.copy(alpha = userBackdrop.scrimOpacity)
                    } else {
                        androidx.compose.ui.graphics.Color.White.copy(alpha = userBackdrop.scrimOpacity)
                    }
                )
        )
    }
}

/**
 * A reusable structural glass surface. On the full tier it blurs an aligned,
 * owned replica of the scene backdrop; compact list items should use the
 * default static recipe rather than instantiate this for every row.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    useStrongFill: Boolean = false,
    fillOverride: androidx.compose.ui.graphics.Color? = null,
    borderOverride: androidx.compose.ui.graphics.Color? = null,
    enableTouchFeedback: Boolean = false,
    enableBackdropBlur: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val visualTheme = LocalVisualTheme.current
    val scene = LocalGlassScene.current
    val effectProfile = LocalGlassEffectProfile.current
    var surfaceOffset by remember { mutableStateOf(Offset.Zero) }
    val glass = visualTheme.tokens.glass
    val shouldBlur = enableBackdropBlur && glass.usesBackdropBlur &&
        effectProfile.backdropBackend != GlassBackdropBackend.NONE &&
        scene != null && scene.size != IntSize.Zero
    val fill = when {
        useStrongFill -> glass.strongFill
        shouldBlur -> glass.fill
        else -> glass.compactFill
    }
    val resolvedFill = fillOverride ?: fill
    var pressed by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var touchPosition by remember { mutableStateOf(Offset.Unspecified) }
    val pressScale by animateFloatAsState(
        targetValue = if (enableTouchFeedback && (pressed || dragging)) glass.pressScale else 1f,
        animationSpec = tween(durationMillis = if (pressed || dragging) 90 else effectProfile.releaseDurationMillis),
        label = "glassPressScale"
    )

    Box(
        modifier = modifier
            .shadow(if (enableTouchFeedback && pressed) 5.dp else 2.dp, shape, clip = false)
            // Draw the glass fill on the shaped host itself. A separate matchParentSize
            // background can be measured independently and expose a rectangular layer.
            .then(if (resolvedFill.alpha > 0f) Modifier.background(resolvedFill, shape) else Modifier)
            .border(1.dp, borderOverride ?: glass.border, shape)
            .clip(shape)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                shadowElevation = 0f
            }
            .then(if (enableTouchFeedback) Modifier.pointerInput(effectProfile.followsPointer) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    dragging = false
                    touchPosition = down.position
                    try {
                        if (effectProfile.followsPointer) {
                            do {
                                val event = awaitPointerEvent()
                                event.changes.firstOrNull()?.let {
                                    if (it.pressed) {
                                        dragging = true
                                        touchPosition = it.position
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        } else {
                            waitForUpOrCancellation()
                        }
                    } finally {
                        pressed = false
                        dragging = false
                        touchPosition = Offset.Unspecified
                    }
                }
            } else Modifier)
            .drawWithContent {
                drawContent()
                if (enableTouchFeedback && (pressed || dragging) && touchPosition != Offset.Unspecified) {
                    val contact = GlassTouchOptics.clampPoint(
                        touchPosition.x, touchPosition.y, size.width, size.height
                    )
                    val opposing = GlassTouchOptics.opposingPoint(
                        contact.x, contact.y, size.width, size.height
                    )
                    val edgeAlpha = glass.edgeResponseAlpha * GlassTouchOptics.edgeProximity(
                        contact.x, contact.y, size.width, size.height, 28f
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glass.highlight.copy(alpha = glass.contactHighlightAlpha),
                                glass.contactTint.copy(alpha = edgeAlpha),
                                androidx.compose.ui.graphics.Color.Transparent
                            ),
                            center = contact,
                            radius = glass.contactRadiusDp.dp.toPx()
                        )
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glass.shadow.copy(alpha = glass.contactShadeAlpha),
                                androidx.compose.ui.graphics.Color.Transparent
                            ),
                            center = opposing,
                            radius = glass.contactRadiusDp.dp.toPx() * 0.9f
                        )
                    )
                }
            }
            .onGloballyPositioned { coordinates ->
                surfaceOffset = scene?.offsetOf(coordinates) ?: Offset.Zero
            }
    ) {
        if (shouldBlur && enableBackdropBlur) {
            AlignedBackdropReplica(
                modifier = Modifier.matchParentSize(),
                sceneSize = scene?.size ?: IntSize.Zero,
                sceneOffset = surfaceOffset,
                backdrop = visualTheme.tokens.backdrop,
                blurRadius = glass.blurRadiusDp.dp,
                backdropDetail = glass.backdropDetail,
                shape = shape
            )
        }

        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface
        ) {
            Box(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}

/** Readable, non-blurred glass recipe for repeated cards and list rows. */
@Composable
fun CompactGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    fillOverride: androidx.compose.ui.graphics.Color? = null,
    color: androidx.compose.ui.graphics.Color? = null,
    borderOverride: androidx.compose.ui.graphics.Color? = null,
    @Suppress("UNUSED_PARAMETER") tonalElevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val visualTheme = LocalVisualTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) visualTheme.tokens.glass.pressScale else 1f,
        animationSpec = tween(durationMillis = if (pressed) 90 else 180),
        label = "compactGlassPressScale"
    )
    val fill = fillOverride ?: color ?: visualTheme.tokens.glass.compactFill
    val border = borderOverride ?: visualTheme.tokens.glass.border

    Box(
        modifier = modifier
            // .shadow(if (pressed) 3.dp else 0.5.dp, shape, clip = false)  // 暂时移除阴影测试
            .then(if (fill.alpha > 0f) Modifier.background(fill, shape) else Modifier)
            .border(0.5.dp, border, shape)
            .clip(shape)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface
        ) {
            content()
        }
    }
}

@Composable
private fun AlignedBackdropReplica(
    modifier: Modifier,
    sceneSize: IntSize,
    sceneOffset: Offset,
    backdrop: BackdropSpec,
    blurRadius: Dp,
    backdropDetail: Float,
    shape: Shape
) {
    val density = LocalDensity.current
    val width = with(density) { sceneSize.width.toDp() }
    val height = with(density) { sceneSize.height.toDp() }

    Box(
        modifier = modifier.clip(shape)
    ) {
        Box(
            modifier = Modifier
                .requiredSize(width, height)
                .offset {
                    GlassTouchOptics.replicaOffset(sceneOffset)
                }
                .graphicsLayer { alpha = backdropDetail.coerceIn(0f, 1f) }
                .blur(blurRadius)
        ) {
            AdaptiveBackdropLayer(
                backdrop = backdrop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun GlassDialogSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit
) {
    GlassSurface(
        modifier = modifier,
        shape = shape,
        useStrongFill = true,
        content = { content() }
    )
}

@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismissRequest) {
        GlassDialogSurface(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                icon?.let {
                    it()
                    Spacer(modifier = Modifier.padding(top = 12.dp))
                }
                title?.invoke()
                if (title != null && text != null) {
                    Spacer(modifier = Modifier.padding(top = 12.dp))
                }
                text?.invoke()
                Spacer(modifier = Modifier.padding(top = 20.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(1f))
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}

@Composable
fun AppPanel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable BoxScope.() -> Unit
) = GlassSurface(
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    contentPadding = contentPadding,
    content = content
)

@Composable
fun GlassBottomDock(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    content: @Composable BoxScope.() -> Unit
) = GlassSurface(
    modifier = modifier,
    // V9PM：Dock 四角统一连续大 R（原来只有顶部两角 26dp，底部边缘不一致）
    shape = com.companion.cc.ui.designsystem.smoothCorner(34.dp),
    contentPadding = contentPadding,
    useStrongFill = true,
    content = content
)
