package com.nothingness.bruhpatcher.ui.components

// Authentic iOS / HyperOS Liquid Glass Floating Navigation Bar
// Refined Apple UI Optical Glass Architecture:
// - Hardware LayerBackdrop recording captured from underlying app screen
// - Sliding indicator bubble positioned BEHIND icons/labels for crystal-clear legibility
// - Direct instant tap responsiveness on all buttons with haptic feedback
// - Swippable & draggable directly on the navbar with real-time bubble tracking
// - Zoom in liquid bubble iOS spring animation on press and slide
// - Monochromatic clean frosted glass with dynamic Monet tinting (zero purple/blue/green gradient artifacts)

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothingness.bruhpatcher.ui.components.liquid.LocalBarBlurBackdrop
import com.nothingness.bruhpatcher.ui.components.liquid.lens
import com.nothingness.bruhpatcher.ui.components.liquid.rememberGravityHighlight
import com.nothingness.bruhpatcher.ui.components.liquid.vibrancy
import com.nothingness.bruhpatcher.ui.theme.AppColors
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.roundToInt

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

private val IndicatorSpecular = Highlight(
    width = 1.dp,
    alpha = 1f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.22f),
        innerBlurRadius = 2.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.3f, -0.05f),
            color = Color.White,
            intensity = 1.0f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.5f),
            color = Color.White,
            intensity = 0.3f,
        ),
        dualPeak = true,
    ),
)

/**
 * Refined Liquid Glass Floating Navigation Bar for HyperOS / Android 17.
 * 
 * Features:
 * - Proper optical layering: Indicator pill is positioned BEHIND icons/labels
 * - 100% Clickable with instant tap responsiveness & haptic feedback
 * - Swippable & draggable directly on the navbar with fluid live tracking
 * - iOS-style zoom in liquid bubble spring animation on drag and press
 * - Clean subtle refraction matching Apple UI guidelines (no chromatic distortion)
 * - Harmonized dynamic Monet theming in both dark and light modes
 */
@Composable
fun LiquidGlassFloatingBar(
    currentRoute: String,
    onNavigate: (LiquidNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    isPatchingActive: Boolean = false,
    isHighDynamicContrast: Boolean = true,
    targetPosition: Float? = null,
    items: List<LiquidNavDestination> = listOf(
        LiquidNavDestination.DASHBOARD,
        LiquidNavDestination.CONFIG,
        LiquidNavDestination.PROGRESS,
        LiquidNavDestination.SETTINGS
    )
) {
    if (items.isEmpty()) return

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val onNavigateUpdated by rememberUpdatedState(onNavigate)

    val tabsCount = items.size
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceIn(0, tabsCount - 1)

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    // Direct drag interaction state on the navbar
    var isNavbarDragging by remember { mutableStateOf(false) }
    var dragAccumulatedOffsetPx by remember { mutableFloatStateOf(0f) }

    // Damped spring animation when targetPosition is not driving the bubble directly
    val animatedPosition = remember { Animatable(selectedIndex.toFloat()) }
    LaunchedEffect(selectedIndex) {
        if (targetPosition == null && !isNavbarDragging) {
            animatedPosition.animateTo(
                targetValue = selectedIndex.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = 520f
                )
            )
        }
    }

    // Derive continuous position from navbar drag, page swipe, or animated position
    val currentBubblePosition = when {
        isNavbarDragging && tabWidthPx > 0f -> {
            val dragFraction = if (isLtr) {
                dragAccumulatedOffsetPx / tabWidthPx
            } else {
                -dragAccumulatedOffsetPx / tabWidthPx
            }
            (selectedIndex.toFloat() + dragFraction).coerceIn(0f, (tabsCount - 1).toFloat())
        }
        targetPosition != null -> targetPosition
        else -> animatedPosition.value
    }

    // Zoom in liquid bubble animation (iOS style dynamic bubble expansion on touch/drag)
    val isBubbleExpanding = isNavbarDragging || (targetPosition != null && abs(targetPosition - selectedIndex) > 0.05f)
    val bubbleZoomScale by animateFloatAsState(
        targetValue = if (isNavbarDragging) 1.10f else if (isBubbleExpanding) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 420f
        ),
        label = "bubbleZoomScale"
    )
    val bubbleStretchX by animateFloatAsState(
        targetValue = if (isNavbarDragging) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.70f,
            stiffness = 450f
        ),
        label = "bubbleStretchX"
    )

    val pillShape = CircleShape
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val backdrop = LocalBarBlurBackdrop.current
    val primaryColor = MaterialTheme.colorScheme.primary

    val baseHighlight = rememberGravityHighlight(IndicatorSpecular, -45f)
    val pillHighlight = rememberGravityHighlight(IndicatorSpecular, 90f)

    val capsuleBgColor = if (isDark) {
        Color(0x3812151D)
    } else {
        Color(0x40FFFFFF)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    totalWidthPx = coordinates.size.width.toFloat()
                    tabWidthPx = ((totalWidthPx - with(density) { 8.dp.toPx() }) / tabsCount).coerceAtLeast(0f)
                }
                .pointerInput(tabsCount, selectedIndex) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isNavbarDragging = true
                            dragAccumulatedOffsetPx = 0f
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragAccumulatedOffsetPx += dragAmount
                        },
                        onDragEnd = {
                            if (tabWidthPx > 0f) {
                                val dragFraction = if (isLtr) dragAccumulatedOffsetPx / tabWidthPx else -dragAccumulatedOffsetPx / tabWidthPx
                                val targetIdx = (selectedIndex + dragFraction.roundToInt()).coerceIn(0, tabsCount - 1)
                                if (targetIdx != selectedIndex) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onNavigateUpdated(items[targetIdx])
                                }
                            }
                            isNavbarDragging = false
                            dragAccumulatedOffsetPx = 0f
                        },
                        onDragCancel = {
                            isNavbarDragging = false
                            dragAccumulatedOffsetPx = 0f
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // ── Layer 1: Base Capsule (Outer Refractive Glass Frame) ───────────
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .shadow(
                        elevation = 8.dp,
                        shape = pillShape,
                        ambientColor = Color.Black.copy(alpha = if (isDark) 0.28f else 0.10f),
                        spotColor = primaryColor.copy(alpha = if (isDark) 0.20f else 0.08f)
                    )
                    .then(
                        if (backdrop != null) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { pillShape },
                                effects = {
                                    vibrancy()
                                    blur(4.dp.toPx(), 4.dp.toPx())
                                    lens(10.dp.toPx(), 10.dp.toPx())
                                },
                                highlight = { baseHighlight.copy(alpha = 0.55f) },
                                onDrawSurface = { drawRect(capsuleBgColor) }
                            )
                        } else {
                            Modifier
                                .clip(pillShape)
                                .background(if (isDark) Color(0xEB131720) else Color(0xF2F5F7FA))
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = if (isDark) 0.14f else 0.45f),
                                    shape = pillShape
                                )
                        }
                    )
            )

            // ── Layer 2: Refractive Liquid Indicator Pill (BEHIND ICONS) ──────
            if (tabWidthPx > 0f) {
                val currentPillX = (currentBubblePosition.coerceIn(0f, (tabsCount - 1).toFloat())) * tabWidthPx
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .graphicsLayer {
                            translationX = if (isLtr) currentPillX else -currentPillX
                            scaleX = bubbleZoomScale * bubbleStretchX
                            scaleY = bubbleZoomScale / kotlin.math.sqrt(bubbleStretchX.coerceAtLeast(0.5f))
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        }
                        .width(with(density) { tabWidthPx.toDp() })
                        .height(56.dp)
                        .then(
                            if (backdrop != null) {
                                Modifier.drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { pillShape },
                                    effects = {
                                        blur(2.dp.toPx(), 2.dp.toPx())
                                        lens(
                                            refractionHeight = 6.dp.toPx(),
                                            refractionAmount = 8.dp.toPx(),
                                            depthEffect = true,
                                            chromaticAberration = 0.0f
                                        )
                                    },
                                    highlight = { pillHighlight.copy(alpha = 0.50f) },
                                    onDrawSurface = {
                                        val frost = if (isDark) {
                                            Color.White.copy(alpha = 0.10f)
                                        } else {
                                            Color.White.copy(alpha = 0.45f)
                                        }
                                        drawRect(frost)
                                        drawRect(primaryColor.copy(alpha = if (isDark) 0.16f else 0.12f))
                                    }
                                )
                            } else {
                                Modifier
                                    .clip(pillShape)
                                    .background(primaryColor.copy(alpha = if (isDark) 0.22f else 0.15f))
                                    .border(
                                        width = 0.8.dp,
                                        color = Color.White.copy(alpha = 0.25f),
                                        shape = pillShape
                                    )
                            }
                        )
                ) {
                    // Subtle physical glass specular streak
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .padding(horizontal = 14.dp)
                            .background(Color.White.copy(alpha = 0.30f))
                    )
                }
            }

            // ── Layer 3: Navigation Tab Items (Rendered CRISPLY ON TOP) ───────
            Row(
                modifier = Modifier
                    .selectableGroup()
                    .height(64.dp)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, destination ->
                    val distance = abs(currentBubblePosition - index).coerceIn(0f, 1f)
                    val activeFraction = 1f - distance

                    val inactiveColor = if (isDark) Color(0x9E9EADC0) else Color(0x9E4A5568)
                    val itemTint = lerp(inactiveColor, primaryColor, activeFraction)

                    LiquidNavItem(
                        destination = destination,
                        isPatchingActive = isPatchingActive && destination == LiquidNavDestination.PROGRESS,
                        tintColor = itemTint,
                        activeFraction = activeFraction,
                        onClick = {
                            onNavigateUpdated(destination)
                        },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 72.dp)
                            .weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Individual navigation tab item inside the Liquid Glass Bar.
 * Renders directly on top of the indicator bubble with 100% crispness.
 */
@Composable
private fun LiquidNavItem(
    destination: LiquidNavDestination,
    isPatchingActive: Boolean,
    tintColor: Color,
    activeFraction: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val iconScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else if (activeFraction > 0.5f) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "iconScale"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .semantics { role = Role.Tab },
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = iconScale
                scaleY = iconScale
            },
            contentAlignment = Alignment.TopEnd
        ) {
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
                        .background(AppColors.Success)
                )
            }
        }

        Text(
            text = destination.title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (activeFraction > 0.5f) FontWeight.SemiBold else FontWeight.Medium,
                letterSpacing = 0.1.sp
            ),
            color = tintColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
        )
    }
}
