package com.nothingness.bruhpatcher.model

import kotlinx.serialization.Serializable

/**
 * Represents device information extracted from build.prop, supporting AOSP and Xiaomi HyperOS 4 / Android 17
 */
@Serializable
data class DeviceInfo(
    val apiLevel: Int,
    val androidVersion: String,
    val deviceCodename: String,
    val deviceName: String,
    val versionName: String,
    val hyperOsVersion: String? = null,
    val isHyperOS: Boolean = false,
    val hasFrameworkJar: Boolean = false,
    val hasServicesJar: Boolean = false,
    val hasMiuiServicesJar: Boolean = false,
    val hasMiuiFrameworkJar: Boolean = false
) {
    /**
     * Determines if device is running Android 17 (Baklava / API 37)
     */
    val isAndroid17: Boolean
        get() = apiLevel >= 37 || androidVersion.startsWith("17")

    /**
     * Gets the workflow version string based on API level
     */
    val workflowVersion: String
        get() = when (apiLevel) {
            33 -> "android13"
            34 -> "android14"
            35 -> "android15"
            36 -> "android16"
            37 -> "android17"
            else -> if (apiLevel > 37) "android17" else "android${apiLevel - 21}"
        }

    /**
     * Gets a safe version string for release tag matching
     */
    val safeVersionName: String
        get() = versionName.replace(Regex("[^a-zA-Z0-9._-]"), "_")

    /**
     * Gets a formatted OS badge string
     */
    val osBadgeText: String
        get() = when {
            isHyperOS && hyperOsVersion != null -> hyperOsVersion
            isHyperOS -> "HyperOS"
            isAndroid17 -> "Android 17"
            else -> "Android $androidVersion"
        }

    companion object {
        val Empty = DeviceInfo(
            apiLevel = 0,
            androidVersion = "Unknown",
            deviceCodename = "unknown",
            deviceName = "Unknown Device",
            versionName = "unknown",
            hyperOsVersion = null,
            isHyperOS = false
        )
    }
}
