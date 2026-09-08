package com.nothingness.bruhpatcher.ui.components

// Authentic iOS / HyperOS Liquid Glass Floating Navigation Bar
// Optical Realism Architecture based on:
// 1. Kyant0/AndroidLiquidGlass & 1812z/HyperIsland (Apache 2.0 / MIT)
// 2. compose-miuix-ui (yukonga) real-time LayerBackdrop & RuntimeShader engine
// 3. Kashif-E/KMPLiquidGlass & SukiSU-Ultra floating capsule physics
//
// Real-time Optical Pipeline:
// - Hardware LayerBackdrop recording captured from underlying App screen
// - Real AGSL SDF rounded-rect refraction lens with 7-sample chromatic dispersion
// - Layered composition: base inactive row + active cyan row captured into tabsBackdrop
// - Slidable indicator pill rendering CombinedBackdrop (background + active tabs through liquid lens)
// - Dynamic gyro/accelerometer device tilt specular bloom highlights (rememberDeviceTilt)
// - Kyant0 damped spring physics: 78/56 press ratio, velocity-based inertia, rubber-band edges

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.nothingness.bruhpatcher.ui.components.liquid.DampedDragAnimation
import com.nothingness.bruhpatcher.ui.components.liquid.InteractiveHighlight
import com.nothingness.bruhpatcher.ui.components.liquid.LocalBarBlurBackdrop
import com.nothingness.bruhpatcher.ui.components.liquid.lens
import com.nothingness.bruhpatcher.ui.components.liquid.rememberCombinedBackdrop
import com.nothingness.bruhpatcher.ui.components.liquid.rememberGravityHighlight
import com.nothingness.bruhpatcher.ui.components.liquid.vibrancy
import com.nothingness.bruhpatcher.ui.theme.AppColors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.sign

/**
 * Navigation items for the iOS-Style Liquid Glass Floating Bar
 */
enum class LiquidNavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Rounded.Dashboard),
    CONFIG("config", "Config", Icons.Rounded.Build),
    PROGRESS("progress", "Terminal", Icons.Rounded.Terminal),
    SETTINGS("settings", "Settings", Icons.Rounded.Settings)
}

private val LocalLiquidTabScale = staticCompositionLocalOf { { 1f } }

private val IndicatorSpecular = Highlight(
    width = 1.dp,
    alpha = 1f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.14f),
        innerBlurRadius = 2.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.3f, -0.05f),
            color = Color.White,
            intensity = 1.2f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.5f),
            color = Color.White,
            intensity = 0.4f,
        ),
        dualPeak = true,
    ),
)

/**
 * Authentic Liquid Glass Floating Navigation Bar for HyperOS / Android 17.
 * 
 * Features 3-layer optical liquid glass architecture:
 * - Layer 1: Outer capsule with hardware backdrop blur, SDF lens refraction, and inactive tabs
 * - Layer 2: Active Cyan tab layer captured into tabsBackdrop
 * - Layer 3: Refractive indicator pill rendering combinedBackdrop with dynamic press scaling,
 *            velocity deformation, and chromatic aberration
 */
@Composable
fun LiquidGlassFloatingBar(
    currentRoute: String,
    onNavigate: (LiquidNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    isPatchingActive: Boolean = false,
    isHighDynamicContrast: Boolean = true,
    items: List<LiquidNavDestination> = listOf(
        LiquidNavDestination.DASHBOARD,
        LiquidNavDestination.CONFIG,
        LiquidNavDestination.PROGRESS,
        LiquidNavDestination.SETTINGS
    )
) {
    if (items.isEmpty()) return

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    val onNavigateUpdated by rememberUpdatedState(onNavigate)

    val tabsCount = items.size
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceIn(0, tabsCount - 1)

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    // Edge rubber-band elasticity
    val offsetAnimation = remember { Animatable(0f) }
    val rubberBandPx = with(density) { 4.dp.toPx() }
    val panelOffset by remember(rubberBandPx) {
        derivedStateOf {
            if (totalWidthPx == 0f) 0f
            else {
                val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
            }
        }
    }

    var currentIndex by remember(selectedIndex) { mutableIntStateOf(selectedIndex) }

    class DragHolder { var animation: DampedDragAnimation? = null }
    val holder = remember { DragHolder() }

    val dragAnimation = remember(animationScope, tabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            canDrag = { offset ->
                val animation = holder.animation ?: return@DampedDragAnimation true
                if (tabWidthPx == 0f) return@DampedDragAnimation false
                val padding = with(density) { 4.dp.toPx() }
                val indicatorX = animation.value * tabWidthPx
                val touchX = if (isLtr) {
                    padding + indicatorX + offset.x
                } else {
                    totalWidthPx - padding - tabWidthPx - indicatorX + offset.x
                }
                touchX in 0f..totalWidthPx
            },
            onDragStopped = {
                val target = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                currentIndex = target
                animateToValue(target.toFloat())
                animationScope.launch { offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f)) }
            },
            onDrag = { _, amount ->
                if (tabWidthPx > 0f) {
                    updateValue(
                        (targetValue + amount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch { offsetAnimation.snapTo(offsetAnimation.value + amount.x) }
                }
            }
        ).also { holder.animation = it }
    }

    // External route change listener
    LaunchedEffect(selectedIndex) {
        snapshotFlow { selectedIndex }.collectLatest {
            currentIndex = it.fastCoerceIn(0, tabsCount - 1)
        }
    }

    // Tab selection animation & navigation dispatch
    LaunchedEffect(dragAnimation) {
        snapshotFlow { currentIndex }.drop(1).collectLatest { index ->
            dragAnimation.animateToValue(index.toFloat())
            onNavigateUpdated(items[index])
        }
    }

    val interactiveHighlight = remember(animationScope, tabWidthPx, isLtr) {
        InteractiveHighlight(
            animationScope = animationScope,
            position = { size, _ ->
                Offset(
                    x = if (isLtr) (dragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                    else size.width - (dragAnimation.value + 0.5f) * tabWidthPx + panelOffset,
                    y = size.height / 2f
                )
            }
        )
    }

    val pillShape = CircleShape
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val backdrop = LocalBarBlurBackdrop.current
    val tabsBackdrop = rememberLayerBackdrop()
    val combinedBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)

    val baseHighlight = rememberGravityHighlight(IndicatorSpecular, -45f)
    val pillHighlight = rememberGravityHighlight(IndicatorSpecular, 90f)

    val containerColor = if (isDark) {
        Color(0x3810131C)
    } else {
        Color(0x44FFFFFF)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.width(IntrinsicSize.Min),
            contentAlignment = Alignment.CenterStart
        ) {
            // ── Layer 1: Base Capsule (Outer Refractive Glass + Base Inactive Tabs) ──
            Row(
                modifier = Modifier
                    .selectableGroup()
                    .onGloballyPositioned { coordinates ->
                        totalWidthPx = coordinates.size.width.toFloat()
                        tabWidthPx = ((totalWidthPx - with(density) { 8.dp.toPx() }) / tabsCount).coerceAtLeast(0f)
                    }
                    .graphicsLayer { translationX = panelOffset }
                    .shadow(
                        elevation = 12.dp,
                        shape = pillShape,
                        ambientColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.15f),
                        spotColor = Color.Black.copy(alpha = if (isDark) 0.55f else 0.25f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .then(
                        if (backdrop != null) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { pillShape },
                                effects = {
                                    vibrancy()
                                    blur(4.dp.toPx(), 4.dp.toPx())
                                    lens(24.dp.toPx(), 24.dp.toPx())
                                },
                                highlight = { baseHighlight.copy(alpha = 0.75f) },
                                layerBlock = {
                                    val width = size.width.coerceAtLeast(1f)
                                    val scale = lerp(1f, 1f + 16.dp.toPx() / width, dragAnimation.pressProgress)
                                    scaleX = scale
                                    scaleY = scale
                                },
                                onDrawSurface = { drawRect(containerColor) }
                            )
                        } else {
                            Modifier
                                .clip(pillShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xE6141721),
                                            Color(0xF20D0F17)
                                        )
                                    )
                                )
                                .border(
                                    width = 0.8.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.26f),
                                            Color.White.copy(alpha = 0.04f)
                                        )
                                    ),
                                    shape = pillShape
                                )
                        }
                    )
                    .then(interactiveHighlight.modifier)
                    .height(64.dp)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompositionLocalProvider(
                    LocalLiquidTabScale provides { 1f }
                ) {
                    items.forEachIndexed { index, destination ->
                        LiquidNavItem(
                            destination = destination,
                            isPatchingActive = isPatchingActive && destination == LiquidNavDestination.PROGRESS,
                            tintColor = if (backdrop == null && selectedIndex == index) {
                                AppColors.HyperOsCyan
                            } else if (isDark) {
                                Color(0x8A9EADC0)
                            } else {
                                Color(0x8A4A5568)
                            },
                            onClick = {
                                if (currentIndex != index) {
                                    currentIndex = index
                                }
                            },
                            modifier = Modifier
                                .defaultMinSize(minWidth = 76.dp)
                                .weight(1f)
                        )
                    }
                }
            }

            // ── Layer 2: Active Tab Row (Rendered for tabsBackdrop capture) ─────────
            // Hidden on direct display via alpha(0f), but recorded into tabsBackdrop
            // so the indicator pill refracts active cyan tabs through its liquid lens!
            CompositionLocalProvider(
                LocalLiquidTabScale provides { lerp(1f, 1.15f, dragAnimation.pressProgress) }
            ) {
                Row(
                    modifier = Modifier
                        .clearAndSetSemantics {}
                        .alpha(0f)
                        .layerBackdrop(tabsBackdrop)
                        .graphicsLayer { translationX = panelOffset }
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, destination ->
                        LiquidNavItem(
                            destination = destination,
                            isPatchingActive = isPatchingActive && destination == LiquidNavDestination.PROGRESS,
                            tintColor = AppColors.HyperOsCyan,
                            onClick = {},
                            modifier = Modifier
                                .defaultMinSize(minWidth = 76.dp)
                                .weight(1f)
                        )
                    }
                }
            }

            // ── Layer 3: Refractive Liquid Indicator Pill ──────────────────────────
            if (tabWidthPx > 0f) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .graphicsLayer {
                            val offset = dragAnimation.value * tabWidthPx
                            translationX = if (isLtr) offset + panelOffset else -offset + panelOffset
                        }
                        .then(dragAnimation.modifier)
                        .then(
                            if (backdrop != null) {
                                Modifier.drawBackdrop(
                                    backdrop = combinedBackdrop,
                                    shape = { pillShape },
                                    effects = {
                                        val progress = dragAnimation.pressProgress
                                        blur(3.dp.toPx(), 3.dp.toPx())
                                        lens(
                                            refractionHeight = lerp(14.dp.toPx(), 20.dp.toPx(), progress),
                                            refractionAmount = lerp(16.dp.toPx(), 24.dp.toPx(), progress),
                                            depthEffect = true,
                                            chromaticAberration = lerp(0.35f, 0.65f, progress),
                                        )
                                    },
                                    highlight = {
                                        pillHighlight.copy(
                                            alpha = lerp(0.40f, 0.95f, dragAnimation.pressProgress)
                                        )
                                    },
                                    layerBlock = {
                                        scaleX = dragAnimation.scaleX
                                        scaleY = dragAnimation.scaleY
                                        val vel = (dragAnimation.velocity / 10f).fastCoerceIn(-0.25f, 0.25f)
                                        scaleX /= 1f - vel * 0.75f
                                        scaleY *= 1f - vel * 0.25f
                                    },
                                    onDrawSurface = {
                                        val progress = dragAnimation.pressProgress
                                        val tintColor = if (isDark) {
                                            Color.White.copy(alpha = lerp(0.10f, 0.16f, progress))
                                        } else {
                                            Color.White.copy(alpha = lerp(0.50f, 0.65f, progress))
                                        }
                                        drawRect(tintColor)
                                        drawRect(
                                            AppColors.HyperOsCyan.copy(
                                                alpha = lerp(0.06f, 0.12f, progress)
                                            )
                                        )
                                    }
                                )
                            } else {
                                Modifier
                                    .clip(pillShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.22f),
                                                Color.White.copy(alpha = 0.09f)
                                            )
                                        ),
                                        shape = pillShape
                                    )
                                    .border(
                                        width = 0.8.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.55f),
                                                Color.White.copy(alpha = 0.06f),
                                                Color.White.copy(alpha = 0.22f)
                                            )
                                        ),
                                        shape = pillShape
                                    )
                            }
                        )
                        .height(56.dp)
                        .width(with(density) { tabWidthPx.toDp() })
                ) {
                    // Specular rim streak (subtle physical glass optical reflection)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .padding(horizontal = 14.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.40f),
                                        Color.White.copy(alpha = 0.40f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

/**
 * Individual navigation tab item inside the Liquid Glass Bar
 */
@Composable
private fun LiquidNavItem(
    destination: LiquidNavDestination,
    isPatchingActive: Boolean,
    tintColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scale = LocalLiquidTabScale.current

    Column(
        modifier = modifier
            .selectable(
                selected = false,
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            )
            .fillMaxHeight()
            .graphicsLayer {
                val value = scale()
                scaleX = value
                scaleY = value
            },
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.title,
                tint = tintColor,
                modifier = Modifier.size(22.dp)
            )

            // Minimalist status indicator badge dot
            if (isPatchingActive) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF30D158)) // Apple System Green
                )
            }
        }

        Text(
            text = destination.title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.1.sp
            ),
            color = tintColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
        )
    }
}
