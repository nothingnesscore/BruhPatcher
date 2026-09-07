package com.nothingness.bruhpatcher.ui.components.miuix

// MIUIX & HyperOS Alive Design Component System
// Inspired by compose-miuix-ui (Apache 2.0) and SukiSU-Ultra (Apache 2.0)

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.nothingness.bruhpatcher.ui.components.LiquidNavDestination
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothingness.bruhpatcher.ui.theme.AppColors

/**
 * Position inside a grouped card container for continuous squircle rounding
 */
enum class MiuixItemPosition {
    TOP, MIDDLE, BOTTOM, SINGLE
}

/**
 * MIUIX / HyperOS Squircle Card Container
 * 
 * Features:
 * - Continuous squircle curvature (20dp)
 * - Translucent obsidian glass background
 * - Hairline specular gradient border
 * - Interactive spring press bounce feedback
 */
@Composable
fun MiuixCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "CardPressScale"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = Color(0x33000000),
                spotColor = Color(0x33007AFF)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF161A26).copy(alpha = 0.90f),
                        Color(0xFF0F121C).copy(alpha = 0.90f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color(0xFF007AFF).copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = shape
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(18.dp)
    ) {
        Column(content = content)
    }
}

/**
 * Grouped card block container for MIUIX preference items
 */
@Composable
fun MiuixGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 5.dp,
                shape = shape,
                ambientColor = Color(0x26000000),
                spotColor = Color(0x26007AFF)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF151924).copy(alpha = 0.88f),
                        Color(0xFF0D1018).copy(alpha = 0.88f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color(0xFF007AFF).copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.04f)
                    )
                ),
                shape = shape
            )
    ) {
        Column(content = content)
    }
}

/**
 * MIUIX Preference Category Header
 */
@Composable
fun MiuixCategoryHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        ),
        color = AppColors.HyperOsBlue,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 6.dp)
    )
}

/**
 * MIUIX Preference List Item with optional icon badge, subtitle, and trailing widget
 */
@Composable
fun MiuixPreferenceItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = AppColors.HyperOsBlue,
    iconBackground: Color = Color(0x20007AFF),
    position: MiuixItemPosition = MiuixItemPosition.MIDDLE,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgAlpha by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.12f else 0.0f,
        animationSpec = tween(durationMillis = 150),
        label = "ItemPressBg"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = bgAlpha))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(iconBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        ),
                        color = AppColors.TextPrimary
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp
                            ),
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }

            if (trailing != null) {
                Spacer(modifier = Modifier.width(12.dp))
                trailing()
            } else if (onClick != null) {
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = AppColors.TextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Divider for grouped items (except bottom/single)
        if (position != MiuixItemPosition.BOTTOM && position != MiuixItemPosition.SINGLE) {
            HorizontalDivider(
                modifier = Modifier.padding(start = if (icon != null) 68.dp else 16.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = Color.White.copy(alpha = 0.08f)
            )
        }
    }
}

/**
 * MIUIX Switch Preference Item
 */
@Composable
fun MiuixSwitchPreference(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = AppColors.HyperOsBlue,
    iconBackground: Color = Color(0x20007AFF),
    position: MiuixItemPosition = MiuixItemPosition.MIDDLE,
    enabled: Boolean = true
) {
    MiuixPreferenceItem(
        title = title,
        subtitle = subtitle,
        icon = icon,
        iconTint = iconTint,
        iconBackground = iconBackground,
        position = position,
        onClick = if (enabled) { { onCheckedChange(!checked) } } else null,
        modifier = modifier,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AppColors.HyperOsBlue,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                    uncheckedTrackColor = Color(0x33FFFFFF)
                )
            )
        }
    )
}

/**
 * MIUIX Capsule Status Badge
 */
@Composable
fun MiuixStatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0x221677FF),
    contentColor: Color = AppColors.HyperOsCyan,
    showDot: Boolean = true,
    dotColor: Color = contentColor
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .border(0.8.dp, contentColor.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = contentColor
        )
    }
}

/**
 * HyperOS / MIUIX Top App Bar
 */
@Composable
fun MiuixTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    logoPainter: Painter? = null,
    actions: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0C0F17).copy(alpha = 0.95f),
                        Color(0xFF090B10).copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.12f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (logoPainter != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x221677FF))
                            .border(0.8.dp, Color(0x4400F0FF), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = logoPainter,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = AppColors.TextPrimary
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }

            if (actions != null) {
                actions()
            }
        }
    }
}

/**
 * Standard MIUIX Bottom Navigation Bar (HyperOS Alive Design)
 * 
 * Clean, docked bottom bar alternative to the floating Liquid Glass bar.
 * Features:
 * - Docked at screen bottom with navigationBarsPadding
 * - Translucent obsidian acrylic background with 0.5dp top border
 * - Active pill indicator with spring animation
 * - Green status dot on Terminal tab when patching is active
 * - Native haptic feedback on tab changes
 */
@Composable
fun MiuixNavigationBar(
    currentRoute: String,
    isPatchingActive: Boolean,
    onNavigate: (LiquidNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xF0141824),
                        Color(0xF80E111A)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.12f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(0.dp)
            )
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (destination in LiquidNavDestination.entries) {
                MiuixNavItem(
                    destination = destination,
                    isSelected = currentRoute == destination.route,
                    isPatchingActive = isPatchingActive,
                    onNavigate = onNavigate,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MiuixNavItem(
    destination: LiquidNavDestination,
    isSelected: Boolean,
    isPatchingActive: Boolean,
    onNavigate: (LiquidNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "TabScale"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else AppColors.TextMuted,
        animationSpec = tween(durationMillis = 200),
        label = "TabContentColor"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigate(destination)
                }
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Background capsule for selected tab
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                )
            }

            Icon(
                imageVector = destination.icon,
                contentDescription = destination.title,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )

            // Status dot for Terminal tab when background patching is in progress
            if (destination == LiquidNavDestination.PROGRESS && isPatchingActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(AppColors.Success)
                        .border(1.dp, Color(0xFF141824), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = destination.title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = contentColor
        )
    }
}

