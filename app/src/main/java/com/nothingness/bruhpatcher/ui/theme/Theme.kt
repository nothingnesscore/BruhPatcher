package com.nothingness.bruhpatcher.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.Primary,
    onPrimary = Color.White,
    primaryContainer = AppColors.PrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = AppColors.PrimaryLight,
    onSecondary = Color.White,
    tertiary = AppColors.Info,
    onTertiary = Color.White,
    background = AppColors.DarkBackground,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.DarkSurface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.DarkSurfaceVariant,
    onSurfaceVariant = AppColors.TextSecondary,
    error = AppColors.Error,
    onError = Color.White,
    errorContainer = AppColors.ErrorVariant,
    onErrorContainer = Color.White,
    outline = AppColors.TextMuted,
    outlineVariant = AppColors.DarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = AppColors.PrimaryVariant,
    onPrimary = Color.White,
    primaryContainer = AppColors.Primary,
    onPrimaryContainer = Color.White,
    secondary = AppColors.PrimaryLight,
    onSecondary = Color.Black,
    tertiary = AppColors.Info,
    onTertiary = Color.White,
    background = Color(0xFFFAFAFC),
    onBackground = Color(0xFF1E293B),
    surface = Color.White,
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    error = AppColors.Error,
    onError = Color.White
)

@Composable
fun BruhPatcherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled by default for consistent branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Alias for backward compatibility
@Composable
fun FrameworkForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = BruhPatcherTheme(darkTheme, dynamicColor, content)