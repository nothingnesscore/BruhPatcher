package com.nothingness.bruhpatcher.ui.theme

import androidx.compose.ui.graphics.Color

// Default theme accents (used as fallback when Monet dynamic color is disabled)
val HyperOsBlue = Color(0xFF007AFF)
val HyperOsCyan = Color(0xFF00C7BE)
val HyperOsIndigo = Color(0xFF4F75FF)
val HyperOsSlate = Color(0xFF8E8E93)

// AppColors object: Clean, modern palette with zero legacy purple/violet gradients
object AppColors {
    // Primary accent - HyperOS Electric Blue
    val Primary = Color(0xFF007AFF)
    val PrimaryVariant = Color(0xFF0056B3)
    val PrimaryLight = Color(0xFF4DA3FF)

    // Neutral slate obsidian backgrounds & surfaces (clean fallback for Dark theme)
    val DarkBackground = Color(0xFF0C0E14)
    val DarkSurface = Color(0xFF141720)
    val DarkSurfaceVariant = Color(0xFF1C202C)
    val DarkCard = Color(0xFF161A24)

    // Status colors
    val Success = Color(0xFF30D158)       // Apple / HyperOS Alive Green
    val SuccessVariant = Color(0xFF248A3D)
    val Error = Color(0xFFFF453A)         // Apple / HyperOS Alive Red
    val ErrorVariant = Color(0xFFD70015)
    val Warning = Color(0xFFFF9F0A)       // Apple / HyperOS Alive Amber
    val WarningVariant = Color(0xFFC97800)
    val Info = Color(0xFF0A84FF)

    // Text colors
    val TextPrimary = Color(0xFFF2F2F7)
    val TextSecondary = Color(0xFF8E8E93)
    val TextMuted = Color(0xFF636366)

    // Terminal colors
    val TerminalBackground = Color(0xFF0A0C10)
    val TerminalText = Color(0xFFE5E5EA)
    val TerminalSuccess = Color(0xFF30D158)
    val TerminalError = Color(0xFFFF453A)
    val TerminalWarning = Color(0xFFFF9F0A)
    val TerminalInfo = Color(0xFF64D2FF)
    val TerminalExtract = Color(0xFF5E5CE6)
    val TerminalUpload = Color(0xFF30B0C7)
    val TerminalRemote = Color(0xFF0A84FF)
    val TerminalDownload = Color(0xFF40C8E0)

    // HyperOS Alive Design Accents
    val HyperOsBlue = Color(0xFF007AFF)
    val HyperOsCyan = Color(0xFF00C7BE)
    val HyperOsIndigo = Color(0xFF4F75FF)
    val HyperOsTitanium = Color(0xFF0A0C12)

    // Liquid Glass Floating Bar Colors (Clean Apple optical glass)
    val LiquidGlassSurface = Color(0x33141722)
    val LiquidGlassSurfaceElevated = Color(0x551C2030)
    val LiquidGlassHighlight = Color(0x40FFFFFF)
    val LiquidGlassBorder = Color(0x2BFFFFFF)
    val LiquidGlassPillTint = Color(0x26007AFF)

    // Dynamic contrast helpers
    val HighContrastBorder = Color(0x66FFFFFF)
    val HighContrastGlow = Color(0x33007AFF)

    // Root status
    val RootAvailable = Success
    val RootUnavailable = Error
}