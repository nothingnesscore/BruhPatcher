package com.nothingness.bruhpatcher.model

/**
 * Feature categories for MIUIX organized layout
 */
enum class FeatureCategory(val title: String) {
    CORE_SECURITY("Core Security & Bypass"),
    PLAY_INTEGRITY("Play Integrity & Spoofing"),
    HYPEROS_TWEAKS("HyperOS & Xiaomi Tweaks"),
    PRIVACY_MEDIA("Media & System Tweaks")
}

/**
 * Represents a patchable feature with category, platform requirements, and metadata
 */
data class Feature(
    val id: String,
    val displayName: String,
    val description: String,
    val category: FeatureCategory = FeatureCategory.CORE_SECURITY,
    val isEnabled: Boolean = false,
    val isDefault: Boolean = false,
    val requiresMiui: Boolean = false,
    val requiresAndroid17: Boolean = false
) {
    companion object {
        val DISABLE_SIGNATURE_VERIFICATION = Feature(
            id = "disable_signature_verification",
            displayName = "Disable Signature Verification",
            description = "Bypasses APK signature, digest checks, and app downgrade limits (CorePatch A13-A17 & HyperOS 4)",
            category = FeatureCategory.CORE_SECURITY,
            isDefault = true
        )

        val DISABLE_SECURE_FLAG = Feature(
            id = "disable_secure_flag",
            displayName = "Disable Secure Flag",
            description = "Enables screenshots, screen recording, and display mirroring in banking and DRM-protected apps",
            category = FeatureCategory.CORE_SECURITY,
            isDefault = true
        )

        val KAORIOS_TOOLBOX = Feature(
            id = "kaorios_toolbox",
            displayName = "Kaorios Toolbox v2.0.6.0",
            description = "Hardware attestation keybox injection, Play Integrity bypass, game 120 FPS unlocker, and per-app stealth",
            category = FeatureCategory.PLAY_INTEGRITY,
            isDefault = true
        )

        val ANDROID17_BUILD_UNFINALIZE = Feature(
            id = "android17_build_unfinalize",
            displayName = "Android 17 Build Reflection Patch",
            description = "Unfinalizes static final fields on Build and Build\$VERSION for HyperOS 4 & Android 17 (Baklava)",
            category = FeatureCategory.PLAY_INTEGRITY,
            isDefault = false,
            requiresAndroid17 = false // Can be applied proactively or on A17
        )

        val CN_NOTIFICATION_FIX = Feature(
            id = "cn_notification_fix",
            displayName = "HyperOS CN Notification Fix",
            description = "Eliminates push notification delays and unfreezes background push services on China ROMs",
            category = FeatureCategory.HYPEROS_TWEAKS,
            requiresMiui = true
        )

        val GOOGLE_PHOTOS_UNLIMITED = Feature(
            id = "google_photos_unlimited",
            displayName = "Google Photos Unlimited Backup",
            description = "Enables unlimited original-quality photo & video cloud backup in Google Photos by spoofing Pixel XL",
            category = FeatureCategory.PRIVACY_MEDIA,
            isDefault = false
        )

        fun getAllFeatures(): List<Feature> = listOf(
            DISABLE_SIGNATURE_VERIFICATION.copy(isEnabled = true),
            DISABLE_SECURE_FLAG.copy(isEnabled = true),
            KAORIOS_TOOLBOX.copy(isEnabled = true),
            ANDROID17_BUILD_UNFINALIZE,
            CN_NOTIFICATION_FIX,
            GOOGLE_PHOTOS_UNLIMITED
        )
    }
}
