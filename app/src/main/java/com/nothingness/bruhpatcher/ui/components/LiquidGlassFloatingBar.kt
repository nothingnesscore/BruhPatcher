package com.nothingness.bruhpatcher.ui.components

// Authentic iOS / HyperOS Liquid Glass Floating Navigation Bar
// Refined Apple UI Optical Glass Architecture:
// - Hardware LayerBackdrop recording captured from underlying app screen
// - Sliding indicator bubble positioned BEHIND icons/labels for crystal-clear legibility
// - Direct instant tap responsiveness on all buttons with haptic feedback
// - Continuous real-time synchronization with HorizontalPager swipe gestures
// - Precise, subtle refraction (6dp) with minimal chromatic aberration (0.06f)
// - Damped spring physics without excessive gelatinous wobbling

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.lerp
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
        color = Color.White.copy(alpha = 0.18f),
        innerBlurRadius = 2.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.3f, -0.05f),
            color = Color.White,
            intensity = 1.1f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.5f),
            color = Color.White,
            intensity = 0.35f,
        ),
        dualPeak = true,
    ),
)

/**
 * Refined Liquid Glass Floating Navigation Bar for HyperOS / Android 17.
 * 
 * Features:
 * - Proper optical layering: Indicator pill is positioned BEHIND icons/labels
 * - Tapping any tab navigates immediately with haptic feedback
 * - Real-time synchronization with page swiping via targetPosition parameter
 * - Clean subtle refraction matching Apple UI guidelines (no wobbly distortion)
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
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val onNavigateUpdated by rememberUpdatedState(onNavigate)

    val tabsCount = items.size
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceIn(0, tabsCount - 1)

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    // Damped spring animation when targetPosition is not driving the bubble directly
    val animatedPosition = remember { Animatable(selectedIndex.toFloat()) }
    LaunchedEffect(selectedIndex) {
        if (targetPosition == null) {
            animatedPosition.animateTo(
                targetValue = selectedIndex.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = 500f
                )
            )
        }
    }

    // Direct continuous position when swiping with 0-frame latency; damped spring when discrete
    val currentBubblePosition = targetPosition ?: animatedPosition.value

    val pillShape = CircleShape
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val backdrop = LocalBarBlurBackdrop.current
    val primaryColor = MaterialTheme.colorScheme.primary

    val baseHighlight = rememberGravityHighlight(IndicatorSpecular, -45f)
    val pillHighlight = rememberGravityHighlight(IndicatorSpecular, 90f)

    val capsuleBgColor = if (isDark) {
        Color(0x3310131A)
    } else {
        Color(0x38FFFFFF)
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
            // ── Layer 1: Base Capsule (Outer Refractive Glass Frame) ───────────
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .shadow(
                        elevation = 10.dp,
                        shape = pillShape,
                        ambientColor = Color.Black.copy(alpha = if (isDark) 0.30f else 0.12f),
                        spotColor = primaryColor.copy(alpha = if (isDark) 0.25f else 0.10f)
                    )
                    .then(
                        if (backdrop != null) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { pillShape },
                                effects = {
                                    vibrancy()
                                    blur(4.dp.toPx(), 4.dp.toPx())
                                    lens(12.dp.toPx(), 12.dp.toPx())
                                },
                                highlight = { baseHighlight.copy(alpha = 0.6f) },
                                onDrawSurface = { drawRect(capsuleBgColor) }
                            )
                        } else {
                            Modifier
                                .clip(pillShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            if (isDark) Color(0xD9141822) else Color(0xCCFFFFFF),
                                            if (isDark) Color(0xEB0E1118) else Color(0xB8F2F4F8)
                                        )
                                    )
                                )
                                .border(
                                    width = 0.8.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = if (isDark) 0.22f else 0.60f),
                                            Color.White.copy(alpha = 0.05f)
                                        )
                                    ),
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
                                            chromaticAberration = 0.06f // Subtle, clean Apple optical glass
                                        )
                                    },
                                    highlight = { pillHighlight.copy(alpha = 0.55f) },
                                    onDrawSurface = {
                                        val tint = if (isDark) {
                                            Color.White.copy(alpha = 0.12f)
                                        } else {
                                            Color.White.copy(alpha = 0.50f)
                                        }
                                        drawRect(tint)
                                        drawRect(primaryColor.copy(alpha = 0.12f))
                                    }
                                )
                            } else {
                                Modifier
                                    .clip(pillShape)
                                    .background(primaryColor.copy(alpha = if (isDark) 0.20f else 0.12f))
                                    .border(
                                        width = 0.8.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.40f),
                                                Color.White.copy(alpha = 0.08f)
                                            )
                                        ),
                                        shape = pillShape
                                    )
                            }
                        )
                ) {
                    // Subtle specular rim streak (clean physical glass reflection)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .padding(horizontal = 14.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.35f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }

            // ── Layer 3: Navigation Tab Items (Rendered CRISPLY ON TOP) ───────
            Row(
                modifier = Modifier
                    .selectableGroup()
                    .onGloballyPositioned { coordinates ->
                        totalWidthPx = coordinates.size.width.toFloat()
                        tabWidthPx = ((totalWidthPx - with(density) { 8.dp.toPx() }) / tabsCount).coerceAtLeast(0f)
                    }
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
                            .defaultMinSize(minWidth = 76.dp)
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
