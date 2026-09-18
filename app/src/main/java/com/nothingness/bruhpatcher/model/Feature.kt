package com.nothingness.bruhpatcher.model

/**
 * Feature categories representing the 4-step sequence from upstream KaoriOS v3.0.0
 */
enum class FeatureCategory(val title: String, val stepNumber: Int) {
    CORE_FOUNDATION("Step 1: Core Foundation & A17", 1),
    SIGNATURE_BYPASS("Step 2: Signature & Downgrade (CorePatch / DSV)", 2),
    PRIVACY_STEALTH("Step 3: Privacy, Stealth & FLAG_SECURE", 3),
    SYSTEM_IDENTITY("Step 4: System Identity & Provider Spoofing", 4)
}

/**
 * Represents a patchable feature with category, step order, and dependency requirements
 */
data class Feature(
    val id: String,
    val displayName: String,
    val description: String,
    val category: FeatureCategory = FeatureCategory.CORE_FOUNDATION,
    val isEnabled: Boolean = true,
    val isDefault: Boolean = true,
    val stepNumber: Int = category.stepNumber,
    val isCorePrerequisite: Boolean = false,
    val orderWarning: String? = null,
    val requiresMiui: Boolean = false,
    val requiresAndroid17: Boolean = false
) {
    companion object {
        val ANDROID17_BUILD_UNFINALIZE = Feature(
            id = "android17_build_unfinalize",
            displayName = "Android 17 Build Reflection Patch",
            description = "Unfinalizes static final fields on Build and Build\$VERSION for HyperOS 4 & Android 17 (Baklava / notes-a17)",
            category = FeatureCategory.CORE_FOUNDATION,
            isDefault = true
        )

        val KAORIOS_CORE = Feature(
            id = "kaorios_core",
            displayName = "KaoriOS Framework v3.0 Core",
            description = "Essential KaoriOS Core: payload DEX injection, Hardware Attestation Keybox hooks, SystemServer and ApplicationPackageManager integration",
            category = FeatureCategory.CORE_FOUNDATION,
            isCorePrerequisite = true,
            orderWarning = "Core Foundation must be patched and boot-tested first before enabling supplementary features!",
            isDefault = true
        )

        val CORE_PATCH_DSV = Feature(
            id = "core_patch_dsv",
            displayName = "DSV / CorePatch",
            description = "Bypasses signature verification, digest comparisons, and APK downgrade protection across framework.jar, services.jar, and miui-services.jar",
            category = FeatureCategory.SIGNATURE_BYPASS,
            isDefault = true
        )

        val DISABLE_SECURE_FLAG = Feature(
            id = "disable_flag_secure",
            displayName = "Disable FLAG_SECURE",
            description = "Dynamic FLAG_SECURE bypass allowing screenshots, screen recording, and mirroring in secure banking and DRM apps",
            category = FeatureCategory.PRIVACY_STEALTH,
            isDefault = true
        )

        val HIDE_DEVELOPER_ADB = Feature(
            id = "hide_developer_adb",
            displayName = "Hide Developer & ADB State",
            description = "Hides developer options, USB debugging, and ADB status from root detectors and banking apps via Settings\$NameValueCache",
            category = FeatureCategory.PRIVACY_STEALTH,
            isDefault = true
        )

        val HIDE_INSTALLED_APPS = Feature(
            id = "hide_installed_apps",
            displayName = "Hide Installed Apps",
            description = "Per-app caller package visibility isolation (HMA alternative) via AppsFilterBase / AppsFilterImpl hooks",
            category = FeatureCategory.PRIVACY_STEALTH,
            isDefault = true
        )

        val INSTALLER_SOURCE_SPOOF = Feature(
            id = "installer_source_spoof",
            displayName = "Installer-Source Spoofing",
            description = "Spoofs installer package name (e.g. Google Play Store) in ComputerEngine.getInstallerPackageName",
            category = FeatureCategory.SYSTEM_IDENTITY,
            isDefault = true
        )

        val SETTINGS_FILTERING = Feature(
            id = "settings_filtering",
            displayName = "Settings Value Provider Filtering",
            description = "Intercepts and filters system and secure settings values, allowing per-app override or removal of sensitive keys",
            category = FeatureCategory.SYSTEM_IDENTITY,
            isDefault = true
        )

        fun getAllFeatures(): List<Feature> = listOf(
            ANDROID17_BUILD_UNFINALIZE,
            KAORIOS_CORE,
            CORE_PATCH_DSV,
            DISABLE_SECURE_FLAG,
            HIDE_DEVELOPER_ADB,
            HIDE_INSTALLED_APPS,
            INSTALLER_SOURCE_SPOOF,
            SETTINGS_FILTERING
        )
    }
}
