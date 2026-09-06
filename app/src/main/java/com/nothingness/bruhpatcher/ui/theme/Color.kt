package com.nothingness.bruhpatcher.ui.theme

import androidx.compose.ui.graphics.Color

// Dark theme colors
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// App-specific colors
object AppColors {
    // Primary accent - Electric blue
    val Primary = Color(0xFF6366F1)
    val PrimaryVariant = Color(0xFF4F46E5)
    val PrimaryLight = Color(0xFF818CF8)

    // Background colors
    val DarkBackground = Color(0xFF0F0F1A)
    val DarkSurface = Color(0xFF1A1A2E)
    val DarkSurfaceVariant = Color(0xFF252538)
    val DarkCard = Color(0xFF1E1E32)

    // Status colors
    val Success = Color(0xFF10B981)
    val SuccessVariant = Color(0xFF059669)
    val Error = Color(0xFFEF4444)
    val ErrorVariant = Color(0xFFDC2626)
    val Warning = Color(0xFFF59E0B)
    val WarningVariant = Color(0xFFD97706)
    val Info = Color(0xFF3B82F6)

    // Text colors
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)

    // Terminal colors
    val TerminalBackground = Color(0xFF0D0D14)
    val TerminalText = Color(0xFFE2E8F0)
    val TerminalSuccess = Color(0xFF22C55E)
    val TerminalError = Color(0xFFF87171)
    val TerminalWarning = Color(0xFFFBBF24)
    val TerminalInfo = Color(0xFF60A5FA)
    val TerminalExtract = Color(0xFFA78BFA)
    val TerminalUpload = Color(0xFF2DD4BF)
    val TerminalRemote = Color(0xFFF472B6)
    val TerminalDownload = Color(0xFF38BDF8)

    // Gradient colors
    val GradientStart = Color(0xFF007AFF) // HyperOS Electric Blue
    val GradientEnd = Color(0xFF7C3AED)   // Prismatic Violet

    // MIUIX HyperOS Colors
    val HyperOsBlue = Color(0xFF007AFF)
    val HyperOsCyan = Color(0xFF00C7BE)
    val HyperOsIndigo = Color(0xFF5856D6)
    val HyperOsPurple = Color(0xFFAF52DE)
    val HyperOsTitanium = Color(0xFF0B0C12)

    // Liquid Glass Floating Bar Colors
    val LiquidGlassSurface = Color(0x40161A29)
    val LiquidGlassSurfaceElevated = Color(0x661E2438)
    val LiquidGlassHighlight = Color(0x60FFFFFF)
    val LiquidGlassBorder = Color(0x33FFFFFF)
    val LiquidGlassPrismStart = Color(0x9900C7BE)
    val LiquidGlassPrismMid = Color(0x99007AFF)
    val LiquidGlassPrismEnd = Color(0x99AF52DE)

    // Dynamic contrast helpers
    val HighContrastBorder = Color(0x80FFFFFF)
    val HighContrastGlow = Color(0x4D007AFF)

    // Root status
    val RootAvailable = Success
    val RootUnavailable = Error
}