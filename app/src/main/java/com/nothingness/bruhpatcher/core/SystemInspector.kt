package com.nothingness.bruhpatcher.core

import android.os.Build
import com.nothingness.bruhpatcher.model.DeviceInfo
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Inspects device properties and framework JARs, with specialized HyperOS 4 and Android 17 detection
 */
object SystemInspector {

    private const val FRAMEWORK_JAR_PATH = "/system/framework/framework.jar"
    private const val SERVICES_JAR_PATH = "/system/framework/services.jar"

    // Alternative paths for MIUI / HyperOS services
    private val MIUI_SERVICES_ALTERNATIVE_PATHS = listOf(
        "/system/system_ext/framework/miui-services.jar",
        "/system_ext/framework/miui-services.jar",
        "/system/framework/miui-services.jar"
    )

    // Alternative paths for MIUI / HyperOS framework
    private val MIUI_FRAMEWORK_ALTERNATIVE_PATHS = listOf(
        "/system/system_ext/framework/miui-framework.jar",
        "/system_ext/framework/miui-framework.jar",
        "/system/framework/miui-framework.jar"
    )

    /**
     * Collects comprehensive device information including HyperOS and Android 17 status
     */
    suspend fun getDeviceInfo(): DeviceInfo = withContext(Dispatchers.IO) {
        val apiLevel = Build.VERSION.SDK_INT
        val androidVersion = Build.VERSION.RELEASE
        val deviceCodename = getSystemProperty("ro.product.device") ?: Build.DEVICE
        val deviceName = getSystemProperty("ro.product.model") ?: Build.MODEL
        val versionName = getSystemProperty("ro.system.build.version.incremental")
            ?: getSystemProperty("ro.build.version.incremental")
            ?: Build.VERSION.INCREMENTAL

        // HyperOS / MIUI detection
        val miOsVersionName = getSystemProperty("ro.mi.os.version.name") // e.g., "OS2.0.10.0.VNACNXM", "OS4.0..."
        val miuiUiVersionName = getSystemProperty("ro.miui.ui.version.name") // e.g., "V816", "V15"
        val miOsIncremental = getSystemProperty("ro.mi.os.version.incremental")

        val isHyperOS = !miOsVersionName.isNullOrBlank() || 
                       miuiUiVersionName?.startsWith("V816", ignoreCase = true) == true ||
                       miuiUiVersionName?.contains("Hyper", ignoreCase = true) == true ||
                       getSystemProperty("ro.miui.has_real_blur") != null

        val hyperOsVersion = when {
            !miOsVersionName.isNullOrBlank() -> {
                val clean = miOsVersionName.trim()
                if (clean.startsWith("OS", ignoreCase = true)) {
                    "HyperOS " + clean.removePrefix("OS").substringBefore(".") + ".0"
                } else {
                    "HyperOS $clean"
                }
            }
            isHyperOS && miuiUiVersionName?.startsWith("V816", ignoreCase = true) == true -> "HyperOS 1.0 (V816)"
            isHyperOS -> "HyperOS"
            !miuiUiVersionName.isNullOrBlank() -> "MIUI $miuiUiVersionName"
            else -> null
        }

        val hasFrameworkJar = checkFileExists(FRAMEWORK_JAR_PATH)
        val hasServicesJar = checkFileExists(SERVICES_JAR_PATH)
        val hasMiuiServicesJar = MIUI_SERVICES_ALTERNATIVE_PATHS.any { checkFileExists(it) }
        val hasMiuiFrameworkJar = MIUI_FRAMEWORK_ALTERNATIVE_PATHS.any { checkFileExists(it) }

        DeviceInfo(
            apiLevel = apiLevel,
            androidVersion = androidVersion,
            deviceCodename = deviceCodename,
            deviceName = deviceName,
            versionName = versionName,
            hyperOsVersion = hyperOsVersion,
            isHyperOS = isHyperOS,
            hasFrameworkJar = hasFrameworkJar,
            hasServicesJar = hasServicesJar,
            hasMiuiServicesJar = hasMiuiServicesJar,
            hasMiuiFrameworkJar = hasMiuiFrameworkJar
        )
    }

    /**
     * Gets a system property using root shell if available, otherwise uses reflection
     */
    private fun getSystemProperty(property: String): String? {
        return try {
            val result = Shell.cmd("getprop $property").exec()
            if (result.isSuccess && result.out.isNotEmpty()) {
                result.out.first().takeIf { it.isNotBlank() }
            } else {
                // Fallback reflection for non-root
                val clazz = Class.forName("android.os.SystemProperties")
                val getMethod = clazz.getMethod("get", String::class.java)
                val value = getMethod.invoke(null, property) as? String
                value?.takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if a file exists using root shell or File API
     */
    private fun checkFileExists(path: String): Boolean {
        return try {
            val result = Shell.cmd("test -f \"$path\" && echo 'exists'").exec()
            if (result.isSuccess && result.out.any { it.contains("exists") }) {
                true
            } else {
                java.io.File(path).exists()
            }
        } catch (e: Exception) {
            java.io.File(path).exists()
        }
    }

    /**
     * Gets the actual path of MIUI/HyperOS services jar if it exists
     */
    fun getMiuiServicesPath(): String? {
        return MIUI_SERVICES_ALTERNATIVE_PATHS.firstOrNull { checkFileExists(it) }
    }

    /**
     * Gets the actual path of MIUI/HyperOS framework jar if it exists
     */
    fun getMiuiFrameworkPath(): String? {
        return MIUI_FRAMEWORK_ALTERNATIVE_PATHS.firstOrNull { checkFileExists(it) }
    }

    /**
     * Gets all available framework file paths
     */
    fun getAvailableFrameworkPaths(): Map<String, String> {
        val paths = mutableMapOf<String, String>()

        if (checkFileExists(FRAMEWORK_JAR_PATH)) {
            paths["framework.jar"] = FRAMEWORK_JAR_PATH
        }
        if (checkFileExists(SERVICES_JAR_PATH)) {
            paths["services.jar"] = SERVICES_JAR_PATH
        }
        getMiuiServicesPath()?.let { path ->
            paths["miui-services.jar"] = path
        }
        getMiuiFrameworkPath()?.let { path ->
            paths["miui-framework.jar"] = path
        }

        return paths
    }
}
