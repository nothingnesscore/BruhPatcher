package com.nothingness.bruhpatcher.ui.components

// Authentic iOS-Style Liquid Glass Floating Navigation Bar
// Designed with optical realism based on:
// 1. Kyant0/AndroidLiquidGlass (Apache 2.0) — SDF rounded-rect refraction AGSL, damped spring physics
// 2. Kashif-E/KMPLiquidGlass (Apache 2.0) — RoundedRectRefractionShader + chromatic dispersion pipeline
// 3. SukiSU-Ultra (GPL-3.0 / Apache 2.0) — Floating capsule architecture, specular bloom
// 4. Apple iOS 18 HIG — Translucent frosted acrylic, directional specular bevel, SF typography
//
// Refraction pipeline (API 33+):
//   RuntimeShader(AGSL) -> graphicsLayer renderEffect ->
//   SDF lens distortion (circleMap) + 7-sample chromatic dispersion
// Frosted glass fallback (API 31-32):
//   RenderEffect.createBlurEffect() -> graphicsLayer renderEffect
// Legacy fallback (API 26-30):
//   Brush.verticalGradient translucent overlay

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import com.nothingness.bruhpatcher.ui.components.liquid.DampedDragAnimation
import com.nothingness.bruhpatcher.ui.components.liquid.InteractiveHighlight
import com.nothingness.bruhpatcher.ui.components.liquid.LiquidGlassShaders
import com.nothingness.bruhpatcher.ui.theme.AppColors
import kotlinx.coroutines.launch
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

// ─── AGSL RuntimeShader cache (lazily initialised once per process) ───────────

private val refractionShader: RuntimeShader? by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        runCatching {
            RuntimeShader(
                LiquidGlassShaders.ROUNDED_RECT_REFRACTION_WITH_DISPERSION
            )
        }.getOrNull()
    } else null
}

// ─── Helper: build the pill RenderEffect ──────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun buildRefractionEffect(
    shader: RuntimeShader,
    pillWidthPx: Float,
    pillHeightPx: Float,
    cornerRadiusPx: Float
): RenderEffect {
    val halfW = pillWidthPx * 0.5f
    val halfH = pillHeightPx * 0.5f

    shader.setFloatUniform("size", pillWidthPx, pillHeightPx)
    shader.setFloatUniform("offset", 0f, 0f)
    // cornerRadii: topLeft, topRight, bottomRight, bottomLeft (all equal for capsule)
    shader.setFloatUniform("cornerRadii", cornerRadiusPx, cornerRadiusPx, cornerRadiusPx, cornerRadiusPx)
    // refractionHeight: how deep the lens bends (px from edge inward)
    shader.setFloatUniform("refractionHeight", minOf(halfW, halfH) * 0.55f)
    // refractionAmount: max pixel displacement at edge
    shader.setFloatUniform("refractionAmount", minOf(halfW, halfH) * 0.18f)
    // depthEffect: 0 = pure surface-normal refraction; 1 = depth-bias toward center
    shader.setFloatUniform("depthEffect", 0.40f)
    // chromaticAberration: prismatic dispersion at corners
    shader.setFloatUniform("chromaticAberration", 0.35f)

    return RenderEffect.createRuntimeShaderEffect(shader, "content")
}

/**
 * iOS-Style Liquid Glass Floating Bottom Navigation Bar.
 *
 * Features:
 * - Frosted dark obsidian glass surface with specular hairline border
 * - Deep ambient elevation drop shadow (20dp)
 * - Sliding indicator pill with REAL optical refraction:
 *     API 33+: AGSL RuntimeShader SDF lens — circleMap displacement + 7-sample chromatic dispersion
 *     API 31-32: hardware RenderEffect Gaussian blur (frosted glass)
 *     API <31: translucent gradient simulation
 * - Kyant0 damped spring physics: 78/56 press ratio, velocity inertia, rubber-band edges
 * - Interactive touch-following AGSL specular bloom (API 33+) / radial-gradient fallback
 * - SF-style typography, HyperOS Cyan active tint, tactile haptic feedback
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
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    val onNavigateUpdated by rememberUpdatedState(onNavigate)

    val tabsCount = items.size
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceIn(0, tabsCount - 1)

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }
    var currentIndex by remember { mutableIntStateOf(selectedIndex) }

    // Edge rubber-band elasticity
    val offsetAnimation = remember { Animatable(0f) }
    val rubberBandPx = with(density) { 5.dp.toPx() }
    val panelOffset by remember(rubberBandPx) {
        derivedStateOf {
            if (totalWidthPx == 0f) 0f
            else {
                val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
            }
        }
    }

    fun indexAt(positionX: Float): Int {
        if (tabWidthPx == 0f) return currentIndex
        val horizontalPaddingPx = with(density) { 6.dp.toPx() }
        val relativeX = (positionX - horizontalPaddingPx).coerceIn(0f, totalWidthPx)
        val rawIndex = (relativeX / tabWidthPx).toInt()
        val clampedIndex = rawIndex.coerceIn(0, tabsCount - 1)
        return if (isLtr) clampedIndex else (tabsCount - 1 - clampedIndex)
    }

    // Kyant0 DampedDragAnimation controller
    val dampedDrag = remember(animationScope, tabsCount, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1.0f,
            pressedScale = 78f / 56f,
            canDrag = { position -> position.x in 0f..totalWidthPx },
            onDragStarted = { position ->
                val target = indexAt(position.x)
                updateValue(target.toFloat())
            },
            onDragStopped = {
                val targetIndex = targetValue.fastRoundToInt().coerceIn(0, tabsCount - 1)
                if (currentIndex != targetIndex) {
                    currentIndex = targetIndex
                    onNavigateUpdated(items[targetIndex])
                }
                updateValue(targetIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0f && dragAmount.x != 0f) {
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            }
        )
    }

    // Synchronize route changes from outside
    LaunchedEffect(selectedIndex) {
        if (currentIndex != selectedIndex) {
            currentIndex = selectedIndex
            dampedDrag.animateToValue(selectedIndex.toFloat())
        }
    }

    // Interactive specular highlight bloom tracking touch/drag coordinates
    val interactiveHighlight = remember(animationScope, isLtr, dampedDrag) {
        InteractiveHighlight(
            animationScope = animationScope,
            position = { size, _ ->
                Offset(
                    x = if (isLtr) (dampedDrag.value + 0.5f) * tabWidthPx + panelOffset
                        else size.width - (dampedDrag.value + 0.5f) * tabWidthPx + panelOffset,
                    y = size.height / 2f
                )
            }
        )
    }

    val pillShape = CircleShape

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Container: Authentic Frosted Dark Glass Surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .graphicsLayer { translationX = panelOffset }
                // Deep ambient drop shadow
                .shadow(
                    elevation = 20.dp,
                    shape = pillShape,
                    ambientColor = Color.Black.copy(alpha = 0.50f),
                    spotColor = Color.Black.copy(alpha = 0.70f)
                )
                .clip(pillShape)
                // Layer 1: Translucent frosted obsidian acrylic backdrop
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xE6141721), // Frosted dark glass
                            Color(0xF20D0F17)  // Deep rich obsidian base
                        )
                    )
                )
                // Layer 2: Directional specular hairline border
                .border(
                    width = 0.8.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.26f),
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    ),
                    shape = pillShape
                )
                .then(interactiveHighlight.modifier)
                .then(interactiveHighlight.gestureModifier)
                .then(dampedDrag.modifier)
                .padding(horizontal = 5.dp, vertical = 5.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Measure total bar dimensions
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords ->
                        totalWidthPx = coords.size.width.toFloat()
                        val contentWidthPx = totalWidthPx - with(density) { 10.dp.toPx() }
                        tabWidthPx = (contentWidthPx / tabsCount).coerceAtLeast(0f)
                    }
            )

            // ── Refractive Indicator Pill ──────────────────────────────────────
            if (tabWidthPx > 0f) {
                val tabWidthDp = with(density) { tabWidthPx.toDp() }
                val pillHeightPx = with(density) { 54.dp.toPx() }
                val progressOffset = dampedDrag.value * tabWidthPx
                val pillOffsetX = if (isLtr) progressOffset + with(density) { 5.dp.toPx() }
                                  else totalWidthPx - tabWidthPx - progressOffset - with(density) { 5.dp.toPx() }

                // Corner radius for a perfect capsule pill = half height
                val pillCornerRadiusPx = pillHeightPx * 0.5f

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = pillOffsetX
                            scaleX = dampedDrag.scaleX
                            scaleY = dampedDrag.scaleY
                            // Velocity inertia momentum deformation
                            val v = dampedDrag.velocity / 12f
                            scaleX /= 1f - (v * 0.65f).fastCoerceIn(-0.16f, 0.16f)
                            scaleY *= 1f - (v * 0.20f).fastCoerceIn(-0.16f, 0.16f)
                        }
                        .height(54.dp)
                        .width(tabWidthDp)
                        // ── Apply refractive RenderEffect ────────────────────
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val shader = refractionShader
                                if (shader != null) {
                                    Modifier.graphicsLayer {
                                        val effect = buildRefractionEffect(
                                            shader = shader,
                                            pillWidthPx = tabWidthPx * dampedDrag.scaleX,
                                            pillHeightPx = pillHeightPx * dampedDrag.scaleY,
                                            cornerRadiusPx = pillCornerRadiusPx
                                        )
                                        renderEffect = effect.asComposeRenderEffect()
                                        clip = true
                                        shape = pillShape
                                    }
                                } else Modifier
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                // API 31-32: hardware blur as frosted glass
                                Modifier.graphicsLayer {
                                    @Suppress("DEPRECATION")
                                    renderEffect = RenderEffect
                                        .createBlurEffect(18f, 18f, Shader.TileMode.CLAMP)
                                        .asComposeRenderEffect()
                                    clip = true
                                    shape = pillShape
                                }
                            } else {
                                Modifier
                            }
                        )
                        .clip(pillShape)
                        // Elevated lens-glass fill (visible on all API levels; on 33+ it's beneath the shader)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    Color.White.copy(alpha = 0.09f)
                                )
                            ),
                            shape = pillShape
                        )
                        // Dual-peak specular edge ring (top bright → mid shadow → bottom glint)
                        .border(
                            width = 0.8.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.55f), // Top specular peak
                                    Color.White.copy(alpha = 0.06f), // Mid shadow
                                    Color.White.copy(alpha = 0.22f)  // Bottom glint
                                )
                            ),
                            shape = pillShape
                        )
                ) {
                    // Top curved optical refraction highlight streak
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.2.dp)
                            .padding(horizontal = 12.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.55f),
                                        Color.White.copy(alpha = 0.55f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Bottom curved caustic reflection streak (API 33+ only — subtle prismatic teal)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(0.8.dp)
                                .padding(horizontal = 20.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            AppColors.HyperOsCyan.copy(alpha = 0.28f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }
            }

            // Tab Items Row
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, destination ->
                    val isSelected = currentIndex == index

                    LiquidNavItem(
                        destination = destination,
                        isSelected = isSelected,
                        isPatchingActive = isPatchingActive && destination == LiquidNavDestination.PROGRESS,
                        onClick = {
                            if (currentIndex != index) {
                                currentIndex = index
                                onNavigateUpdated(destination)
                            }
                            dampedDrag.animateToValue(index.toFloat())
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Authentic iOS-style individual navigation tab item inside the Liquid Glass Bar
 */
@Composable
private fun LiquidNavItem(
    destination: LiquidNavDestination,
    isSelected: Boolean,
    isPatchingActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Tactile press compression (subtle 0.94x scale down on tap)
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "PressScale"
    )

    // Animated color transition between active and inactive states
    val activeColor = AppColors.HyperOsCyan
    val inactiveColor = Color(0x8A9EADC0) // Apple Secondary Slate

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 180),
        label = "ContentColor"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .semantics {
                selected = isSelected
                role = Role.Tab
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = destination.title,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )

                // Minimalist iOS-style status indicator badge dot
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
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    letterSpacing = 0.15.sp
                ),
                color = contentColor,
                maxLines = 1
            )
        }
    }
}
