package com.nothingness.bruhpatcher.core

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Root manager type detection
 */
enum class RootManagerType {
    MAGISK,
    KERNELSU,
    APATCH,
    UNKNOWN
}

/**
 * Manages root shell operations using libsu
 * Supports Magisk, KernelSU (including KernelSU Next), and APatch
 */
object RootManager {

    private var detectedManager: RootManagerType? = null

    /**
     * Checks if root access is available
     */
    fun isRootAvailable(): Boolean {
        return try {
            Shell.getShell().isRoot
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Requests root access (initializes shell)
     */
    suspend fun requestRoot(): Boolean = withContext(Dispatchers.IO) {
        try {
            val hasRoot = Shell.getShell().isRoot
            if (hasRoot) {
                // Detect root manager type
                detectedManager = detectRootManager()
            }
            hasRoot
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Detects which root manager is being used
     */
    private fun detectRootManager(): RootManagerType {
        // Check for KernelSU (including KernelSU Next)
        val ksuCheck = Shell.cmd("which ksud || which ksu || [ -f /data/adb/ksu/bin/busybox ]").exec()
        if (ksuCheck.isSuccess && ksuCheck.out.isNotEmpty()) {
            return RootManagerType.KERNELSU
        }
        
        // Also check for KernelSU modules path
        val ksuModulesCheck = Shell.cmd("[ -d /data/adb/ksu/modules ] && echo yes").exec()
        if (ksuModulesCheck.isSuccess && ksuModulesCheck.out.any { it.contains("yes") }) {
            return RootManagerType.KERNELSU
        }

        // Check for APatch
        val apatchCheck = Shell.cmd("which apd || [ -f /data/adb/apatch/config ]").exec()
        if (apatchCheck.isSuccess && apatchCheck.out.isNotEmpty()) {
            return RootManagerType.APATCH
        }

        // Check for Magisk
        val magiskCheck = Shell.cmd("which magisk").exec()
        if (magiskCheck.isSuccess && magiskCheck.out.isNotEmpty()) {
            return RootManagerType.MAGISK
        }

        return RootManagerType.UNKNOWN
    }

    /**
     * Gets the detected root manager type
     */
    fun getRootManagerType(): RootManagerType {
        return detectedManager ?: detectRootManager()
    }

    /**
     * Copies a system file to the app's private directory
     */
    suspend fun extractSystemFile(
        sourcePath: String,
        destinationDir: File,
        fileName: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val tempPath = "/data/local/tmp/$fileName"
            val destinationFile = File(destinationDir, fileName)

            // Ensure temp directory exists and copy with root
            val copyToTempResult = Shell.cmd(
                "su -c 'mkdir -p /data/local/tmp && cp \"$sourcePath\" \"$tempPath\" && chmod 644 \"$tempPath\"'"
            ).exec()

            if (!copyToTempResult.isSuccess) {
                // Log the actual error for debugging
                val errMsg = copyToTempResult.err.joinToString()
                return@withContext Result.failure(
                    Exception("Failed to copy file to temp: $errMsg")
                )
            }

            val copyToAppResult = Shell.cmd(
                "cp \"$tempPath\" \"${destinationFile.absolutePath}\"",
                "chmod 644 \"${destinationFile.absolutePath}\"",
                "rm -f \"$tempPath\""
            ).exec()

            if (!copyToAppResult.isSuccess) {
                val catResult = Shell.cmd(
                    "cat \"$tempPath\" > \"${destinationFile.absolutePath}\"",
                    "rm -f \"$tempPath\""
                ).exec()

                if (!catResult.isSuccess && !destinationFile.exists()) {
                    return@withContext Result.failure(
                        Exception("Failed to copy file to app directory: ${copyToAppResult.err.joinToString()}")
                    )
                }
            }

            if (destinationFile.exists() && destinationFile.length() > 0) {
                Result.success(destinationFile)
            } else {
                Result.failure(Exception("File copy verification failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Installs a module (supports Magisk, KernelSU, APatch)
     */
    suspend fun installMagiskModule(moduleZipPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val managerType = getRootManagerType()
            
            val result = when (managerType) {
                RootManagerType.KERNELSU -> {
                    // KernelSU / KernelSU Next installation
                    // Method 1: Use ksud module install
                    val ksudResult = Shell.cmd("ksud module install \"$moduleZipPath\"").exec()
                    if (ksudResult.isSuccess) {
                        ksudResult
                    } else {
                        // Method 2: Try ksu command
                        val ksuResult = Shell.cmd("ksu module install \"$moduleZipPath\"").exec()
                        if (ksuResult.isSuccess) {
                            ksuResult
                        } else {
                            // Method 3: Manual installation to KernelSU modules directory
                            installModuleManually(moduleZipPath, "/data/adb/ksu/modules")
                        }
                    }
                }
                RootManagerType.APATCH -> {
                    // APatch installation
                    val apdResult = Shell.cmd("apd module install \"$moduleZipPath\"").exec()
                    if (apdResult.isSuccess) {
                        apdResult
                    } else {
                        installModuleManually(moduleZipPath, "/data/adb/apatch/modules")
                    }
                }
                RootManagerType.MAGISK -> {
                    // Magisk installation
                    Shell.cmd("magisk --install-module \"$moduleZipPath\"").exec()
                }
                RootManagerType.UNKNOWN -> {
                    // Try Magisk first, then manual installation
                    val magiskResult = Shell.cmd("magisk --install-module \"$moduleZipPath\"").exec()
                    if (magiskResult.isSuccess) {
                        magiskResult
                    } else {
                        // Try manual installation to common paths
                        val ksuManual = installModuleManually(moduleZipPath, "/data/adb/ksu/modules")
                        if (ksuManual.isSuccess) ksuManual
                        else installModuleManually(moduleZipPath, "/data/adb/modules")
                    }
                }
            }

            if (result.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Module installation failed: ${result.err.joinToString("\n")}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Manual module installation by extracting to modules directory
     */
    private fun installModuleManually(zipPath: String, modulesDir: String): Shell.Result {
        // Extract module id from zip
        val moduleId = File(zipPath).nameWithoutExtension
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(50)
        
        val moduleDir = "$modulesDir/$moduleId"
        
        return Shell.cmd(
            "mkdir -p \"$moduleDir\"",
            "unzip -o \"$zipPath\" -d \"$moduleDir\"",
            "chmod -R 755 \"$moduleDir\"",
            // Create update flag for KernelSU
            "touch \"$modulesDir/update\"",
            "echo 'Module installed to $moduleDir'"
        ).exec()
    }

    /**
     * Triggers a device reboot
     */
    suspend fun reboot(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val result = Shell.cmd("reboot").exec()
            if (result.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Reboot failed: ${result.err.joinToString()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets root manager version string
     */
    fun getMagiskVersion(): String? {
        return try {
            val managerType = getRootManagerType()
            when (managerType) {
                RootManagerType.KERNELSU -> {
                    val result = Shell.cmd("ksud --version 2>/dev/null || cat /data/adb/ksu/.version 2>/dev/null || echo 'KernelSU'").exec()
                    if (result.isSuccess && result.out.isNotEmpty()) {
                        "KernelSU ${result.out.first()}"
                    } else {
                        "KernelSU"
                    }
                }
                RootManagerType.APATCH -> {
                    val result = Shell.cmd("apd --version 2>/dev/null || echo 'APatch'").exec()
                    if (result.isSuccess && result.out.isNotEmpty()) {
                        "APatch ${result.out.first()}"
                    } else {
                        "APatch"
                    }
                }
                RootManagerType.MAGISK -> {
                    val result = Shell.cmd("magisk -v").exec()
                    if (result.isSuccess && result.out.isNotEmpty()) {
                        "Magisk ${result.out.first()}"
                    } else {
                        "Magisk"
                    }
                }
                RootManagerType.UNKNOWN -> {
                    val result = Shell.cmd("magisk -v").exec()
                    if (result.isSuccess && result.out.isNotEmpty()) {
                        result.out.first()
                    } else {
                        "Root"
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Cleans up temporary files
     */
    suspend fun cleanup(filesDir: File) = withContext(Dispatchers.IO) {
        try {
            filesDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".jar")) {
                    file.delete()
                }
            }
            Shell.cmd("rm -f /data/local/tmp/*.jar").exec()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }


    /**
     * Gets the package name of the active root manager
     */
    fun getManagerPackageName(): String? {
        return when (getRootManagerType()) {
            RootManagerType.KERNELSU -> "vaumet.ndfdvv.ztmdui"
            RootManagerType.APATCH -> "me.bmax.apatch" // Common package name, but might vary
            RootManagerType.MAGISK -> "com.topjohnwu.magisk"
            RootManagerType.UNKNOWN -> "com.topjohnwu.magisk" // Default to Magisk
        }
    }

    /**
     * Moves a file to the public Downloads folder using root access
     */
    suspend fun moveToDownloads(sourceFile: File, destName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val destFile = File(downloadsDir, destName)
            
            // ensuring destination directory exists (though /sdcard/Download should always exist)
            // but just in case
            
            val result = Shell.cmd(
                "cp \"${sourceFile.absolutePath}\" \"${destFile.absolutePath}\"",
                "chmod 666 \"${destFile.absolutePath}\"", // World readable/writable
                "chown 0:9997 \"${destFile.absolutePath}\"" // root:everybody - standard for shared storage
            ).exec()

            if (result.isSuccess) {
                // Delete source if copy was successful
                sourceFile.delete()
                Result.success(destFile)
            } else {
                 Result.failure(Exception("Failed to move file: ${result.err.joinToString()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
