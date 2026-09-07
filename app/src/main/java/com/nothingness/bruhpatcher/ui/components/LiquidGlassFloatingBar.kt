package com.nothingness.bruhpatcher.ui.components

// Adapted from:
// 1. Kyant0/AndroidLiquidGlass — https://github.com/Kyant0/AndroidLiquidGlass (Apache 2.0)
// 2. compose-miuix-ui — https://github.com/compose-miuix-ui/miuix (Apache 2.0)
// 3. SukiSU-Ultra — https://github.com/SukiSU-Ultra/SukiSU-Ultra (manager FloatingBottomBar.kt)

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.util.lerp
import com.nothingness.bruhpatcher.ui.components.liquid.DampedDragAnimation
import com.nothingness.bruhpatcher.ui.components.liquid.InteractiveHighlight
import com.nothingness.bruhpatcher.ui.theme.AppColors
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * Navigation items for the Liquid Glass Floating Bar
 */
enum class LiquidNavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Status", Icons.Rounded.Dashboard),
    CONFIG("config", "Patches", Icons.Rounded.Build),
    PROGRESS("progress", "Terminal", Icons.Rounded.Terminal),
    SETTINGS("settings", "Settings", Icons.Rounded.Settings)
}

/**
 * SukiSU Manager / MIUIX Liquid Glass Floating Bottom Navigation Bar
 * 
 * Features implemented directly from SukiSU-Ultra and compose-miuix-ui:
 * 1. DampedDragAnimation with spring physics and scale expansion (78/56 ratio)
 * 2. Sliding indicator pill with spring damping and inertia velocity
 * 3. Edge rubber-banding resistance on over-drag
 * 4. Interactive touch-driven specular bloom highlight (InteractiveHighlight)
 * 5. Multi-layer titanium glass surface with dual-peak Fresnel lens specular highlights
 * 6. Chromatic aberration prismatic perimeter border
 * 7. Live status pulsing orb on the Terminal tab during patching
 */
@Composable
fun LiquidGlassFloatingBar(
    currentRoute: String,
    onNavigate: (LiquidNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    isHighDynamicContrast: Boolean = true,
    isPatchingActive: Boolean = false
) {
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    val onNavigateUpdated by rememberUpdatedState(onNavigate)

    val items = LiquidNavDestination.entries
    val tabsCount = items.size
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceIn(0, tabsCount - 1)

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }
    var currentIndex by remember { mutableIntStateOf(selectedIndex) }

    // Dynamic contrast factor alphas
    val surfaceAlpha = if (isHighDynamicContrast) 0.86f else 0.72f
    val borderAlpha = if (isHighDynamicContrast) 0.50f else 0.32f
    val refractionGlow = if (isHighDynamicContrast) 0.45f else 0.25f

    // Ambient light refraction shimmer across glass surface
    val infiniteTransition = rememberInfiniteTransition(label = "LiquidGlassShimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RefractionOffset"
    )

    // Live terminal tab pulse when patching is active
    val terminalPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "TerminalPulse"
    )

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
        val logicalX = if (isLtr) positionX else totalWidthPx - positionX
        return ((logicalX - horizontalPaddingPx) / tabWidthPx)
            .toInt()
            .coerceIn(0, tabsCount - 1)
    }

    // SukiSU-Ultra DampedDragAnimation controller
    val dampedDrag = remember(animationScope, tabsCount, density, isLtr) {
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

    // SukiSU InteractiveHighlight tracking pointer touch
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
            .padding(horizontal = 22.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Container: Frosted Titanium-Glass surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .graphicsLayer { translationX = panelOffset }
                .shadow(
                    elevation = if (isHighDynamicContrast) 18.dp else 10.dp,
                    shape = pillShape,
                    ambientColor = Color(0x66007AFF),
                    spotColor = Color(0x9900C7BE)
                )
                .clip(pillShape)
                // Layer 1: Frosted titanium-glass base surface
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF141824).copy(alpha = surfaceAlpha),
                            Color(0xFF090B12).copy(alpha = surfaceAlpha)
                        )
                    )
                )
                // Layer 2: Dual-peak Fresnel specular highlight & dynamic light refraction
                .drawWithCache {
                    val width = size.width
                    val height = size.height
                    val shimmerPos = shimmerOffset * width

                    val refractionBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x20FFFFFF),
                            Color(0x4500C7BE),
                            Color(0x60FFFFFF),
                            Color(0x357000FF),
                            Color.Transparent
                        ),
                        start = Offset(shimmerPos - 120f, 0f),
                        end = Offset(shimmerPos + 120f, height)
                    )

                    val topFresnelBrush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x25FFFFFF),
                            Color.White.copy(alpha = refractionGlow),
                            Color(0x5000C7BE),
                            Color.White.copy(alpha = refractionGlow),
                            Color(0x20FFFFFF)
                        )
                    )

                    onDrawWithContent {
                        drawContent()
                        // Refractive specular dispersion
                        drawRect(
                            brush = refractionBrush,
                            blendMode = BlendMode.Screen
                        )
                        // Dual-peak top lens highlight hairline
                        drawLine(
                            brush = topFresnelBrush,
                            start = Offset(28f, 1f),
                            end = Offset(width - 28f, 1f),
                            strokeWidth = 1.6.dp.toPx()
                        )
                    }
                }
                // Layer 3: Chromatic dispersion prismatic border
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = borderAlpha),
                            Color(0xFF00C7BE).copy(alpha = borderAlpha * 0.9f),
                            Color(0xFF1677FF).copy(alpha = borderAlpha * 0.85f),
                            Color(0xFF7000FF).copy(alpha = borderAlpha * 0.9f),
                            Color.White.copy(alpha = borderAlpha * 0.45f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(800f, 200f)
                    ),
                    shape = pillShape
                )
                .then(interactiveHighlight.modifier)
                .then(interactiveHighlight.gestureModifier)
                .then(dampedDrag.modifier)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Measure total bar dimensions
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords ->
                        totalWidthPx = coords.size.width.toFloat()
                        val contentWidthPx = totalWidthPx - with(density) { 12.dp.toPx() }
                        tabWidthPx = (contentWidthPx / tabsCount).coerceAtLeast(0f)
                    }
            )

            // SukiSU-Ultra Sliding Indicator Pill with spring damping
            if (tabWidthPx > 0f) {
                val tabWidthDp = with(density) { tabWidthPx.toDp() }
                val progressOffset = dampedDrag.value * tabWidthPx
                val pillOffsetX = if (isLtr) progressOffset + with(density) { 6.dp.toPx() }
                                  else totalWidthPx - tabWidthPx - progressOffset - with(density) { 6.dp.toPx() }

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = pillOffsetX
                            scaleX = dampedDrag.scaleX
                            scaleY = dampedDrag.scaleY
                            val v = dampedDrag.velocity / 12f
                            scaleX /= 1f - (v * 0.65f).fastCoerceIn(-0.18f, 0.18f)
                            scaleY *= 1f - (v * 0.20f).fastCoerceIn(-0.18f, 0.18f)
                        }
                        .clip(pillShape)
                        // Layer 1: Translucent liquid pill surface
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1677FF).copy(alpha = 0.38f),
                                    Color(0xFF7000FF).copy(alpha = 0.24f)
                                )
                            ),
                            shape = pillShape
                        )
                        // Layer 2: Tactile inner specular glow
                        .border(
                            width = 1.1.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.55f),
                                    Color(0xFF00F0FF).copy(alpha = 0.40f),
                                    Color.White.copy(alpha = 0.20f)
                                )
                            ),
                            shape = pillShape
                        )
                        .height(54.dp)
                        .width(tabWidthDp)
                )
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
                        terminalPulseAlpha = terminalPulse,
                        pressProgress = dampedDrag.pressProgress,
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
 * Individual navigation tab item inside the Liquid Glass Bar
 */
@Composable
private fun LiquidNavItem(
    destination: LiquidNavDestination,
    isSelected: Boolean,
    isPatchingActive: Boolean,
    terminalPulseAlpha: Float,
    pressProgress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Animated scale on selection and drag press
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "IconScale"
    )

    // Animated color transition
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else AppColors.TextSecondary.copy(alpha = 0.70f),
        animationSpec = tween(durationMillis = 200),
        label = "ContentColor"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
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
                scaleX = iconScale
                scaleY = iconScale
            }
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = destination.title,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )

                // SukiSU Live status glowing indicator badge
                if (isPatchingActive) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF30D158).copy(alpha = terminalPulseAlpha))
                    )
                }
            }

            Text(
                text = destination.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = contentColor,
                maxLines = 1
            )
        }
    }
}
