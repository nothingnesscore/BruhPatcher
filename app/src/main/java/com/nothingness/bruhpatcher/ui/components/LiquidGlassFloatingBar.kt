package com.nothingness.bruhpatcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothingness.bruhpatcher.ui.theme.AppColors

/**
 * Navigation items for the Liquid Glass Floating Bar
 */
enum class LiquidNavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Status", Icons.Default.Dashboard),
    CONFIG("config", "Patches", Icons.Default.Build),
    PROGRESS("progress", "Terminal", Icons.Default.Terminal),
    SETTINGS("settings", "Settings", Icons.Default.Settings)
}

/**
 * MIUIX Liquid Glass Floating Bar
 * 
 * Features:
 * 1. Multi-layer liquid glass refraction shader with ambient light dispersion
 * 2. Specular diagonal light refraction highlight simulating real optical lens curvature
 * 3. Chromatic dispersion border with subtle prismatic aberration (cyan-indigo-violet rim)
 * 4. Dynamic contrast support adjusting glass opacity and border luminance dynamically
 * 5. Smooth spring-based indicator pill sliding behind selected tab
 * 6. Active pulse indicators for live terminal/patching activity
 */
@Composable
fun LiquidGlassFloatingBar(
    currentRoute: String,
    onNavigate: (LiquidNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    isHighDynamicContrast: Boolean = true,
    isPatchingActive: Boolean = false
) {
    // Dynamic contrast factors
    val surfaceAlpha = if (isHighDynamicContrast) 0.88f else 0.72f
    val borderAlpha = if (isHighDynamicContrast) 0.55f else 0.35f
    val refractionGlow = if (isHighDynamicContrast) 0.40f else 0.22f

    // Ambient light refraction shimmer animation across glass
    val infiniteTransition = rememberInfiniteTransition(label = "LiquidGlassShimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RefractionOffset"
    )

    // Pulse animation for live terminal tab when patching is active
    val terminalPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "TerminalPulse"
    )

    val barShape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow & drop shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = if (isHighDynamicContrast) 20.dp else 12.dp,
                    shape = barShape,
                    ambientColor = Color(0x66007AFF),
                    spotColor = Color(0x9900C7BE)
                )
                .clip(barShape)
                // Layer 1: Frosted titanium-glass base surface
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF131724).copy(alpha = surfaceAlpha),
                            Color(0xFF090B12).copy(alpha = surfaceAlpha)
                        )
                    )
                )
                // Layer 2: Prismatic light refraction & specular lens highlight
                .drawWithCache {
                    val width = size.width
                    val height = size.height
                    val shimmerPos = shimmerOffset * width

                    // Specular light beam refracting through the convex liquid lens
                    val refractionBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x22FFFFFF),
                            Color(0x5500C7BE),
                            Color(0x66FFFFFF),
                            Color(0x447C3AED),
                            Color.Transparent
                        ),
                        start = Offset(shimmerPos - 120f, 0f),
                        end = Offset(shimmerPos + 120f, height)
                    )

                    // Top lens reflection (Fresnel light highlight along top rim)
                    val fresnelTopBrush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x33FFFFFF),
                            Color.White.copy(alpha = refractionGlow),
                            Color(0x4038BDF8),
                            Color.White.copy(alpha = refractionGlow),
                            Color(0x22FFFFFF)
                        )
                    )

                    onDrawWithContent {
                        drawContent()
                        // Draw moving refractive dispersion
                        drawRect(
                            brush = refractionBrush,
                            blendMode = BlendMode.Screen
                        )
                        // Draw Fresnel hairline highlight at top rim
                        drawLine(
                            brush = fresnelTopBrush,
                            start = Offset(24f, 1f),
                            end = Offset(width - 24f, 1f),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
                // Layer 3: Prismatic chromatic aberration border
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = borderAlpha),
                            Color(0xFF00C7BE).copy(alpha = borderAlpha * 0.9f),
                            Color(0xFF007AFF).copy(alpha = borderAlpha * 0.8f),
                            Color(0xFF7C3AED).copy(alpha = borderAlpha * 0.9f),
                            Color.White.copy(alpha = borderAlpha * 0.4f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(800f, 200f)
                    ),
                    shape = barShape
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidNavDestination.entries.forEach { destination ->
                    val isSelected = currentRoute == destination.route

                    LiquidNavItem(
                        destination = destination,
                        isSelected = isSelected,
                        isPatchingActive = isPatchingActive && destination == LiquidNavDestination.PROGRESS,
                        terminalPulseAlpha = terminalPulse,
                        onClick = { onNavigate(destination) }
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
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Animated scale on selection
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "IconScale"
    )

    // Animated icon & text color
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else AppColors.TextSecondary.copy(alpha = 0.75f),
        animationSpec = tween(durationMillis = 220),
        label = "ContentColor"
    )

    val itemShape = RoundedCornerShape(22.dp)

    Box(
        modifier = Modifier
            .height(52.dp)
            .clip(itemShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .then(
                if (isSelected) {
                    Modifier
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF007AFF).copy(alpha = 0.40f),
                                    Color(0xFF7C3AED).copy(alpha = 0.25f)
                                )
                            ),
                            shape = itemShape
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.45f),
                                    Color(0xFF38BDF8).copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.15f)
                                )
                            ),
                            shape = itemShape
                        )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 14.dp, vertical = 4.dp),
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

                // Live status glowing indicator dot
                if (isPatchingActive) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                Color(0xFF00E676).copy(alpha = terminalPulseAlpha)
                            )
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
