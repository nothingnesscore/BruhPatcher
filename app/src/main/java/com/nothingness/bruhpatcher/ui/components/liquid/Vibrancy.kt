package com.nothingness.bruhpatcher.ui.components.liquid

import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.colorControls

fun BackdropEffectScope.vibrancy(
    brightness: Float = 0f,
    contrast: Float = 1f,
    saturation: Float = 1.5f,
) {
    colorControls(brightness = brightness, contrast = contrast, saturation = saturation)
}
