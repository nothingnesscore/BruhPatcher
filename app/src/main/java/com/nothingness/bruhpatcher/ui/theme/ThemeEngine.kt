package com.nothingness.bruhpatcher.ui.theme

/**
 * Theme Engine selector:
 * - HYPEROS_MIUIX: Authentic Xiaomi HyperOS 4 / MIUIX Alive Design with squircle containers and dynamic blur.
 * - AOSP_MATERIAL3: Standard Google Material 3 / Material You for generic AOSP and non-Xiaomi OEM ROMs.
 */
enum class ThemeEngine(val label: String, val description: String) {
    HYPEROS_MIUIX("HyperOS MIUIX", "Authentic Xiaomi HyperOS Alive Design with continuous squircle cards and MIUI animations"),
    AOSP_MATERIAL3("AOSP Material 3", "Clean Android 15/16/17 Material You design with Google dynamic color palettes")
}
