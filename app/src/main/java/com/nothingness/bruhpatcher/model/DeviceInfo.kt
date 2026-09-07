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
     * Determines if device is Xiaomi Redmi Turbo 3 / Poco F6 (peridot)
     */
    val isPocoF6OrTurbo3: Boolean
        get() = deviceCodename.equals("peridot", ignoreCase = true) ||
                deviceName.contains("24069RA21C", ignoreCase = true) ||
                deviceName.contains("POCO F6", ignoreCase = true) ||
                deviceName.contains("Redmi Turbo 3", ignoreCase = true)

    /**
     * Determines if device is running HyperOS 4.0
     */
    val isHyperOS4: Boolean
        get() = isHyperOS && (hyperOsVersion?.contains("4.") == true ||
                versionName.contains("OS4.", ignoreCase = true) ||
                (isAndroid17 && isHyperOS))

    /**
     * Detected SoC name if known
     */
    val socDescription: String
        get() = when {
            isPocoF6OrTurbo3 -> "Snapdragon 8s Gen 3 (SM8635)"
            else -> ""
        }

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
