package com.nothingness.bruhpatcher.core

import android.content.Context
import android.os.Build
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates Magisk/KernelSU compatible module ZIPs from patched framework JARs
 * Uses the template from assets/module_template
 */
object ModuleGenerator {

    private const val MODULE_ID = "bruhpatcher_mod"
    private const val TEMPLATE_ASSETS_PATH = "module_template"

    /**
     * Creates a Magisk module ZIP from patched JAR files using the template
     * @param context Android context
     * @param patchedJars Map of JAR names to their patched file paths
     * @param deviceCodename Device codename for module naming
     * @param androidVersion Android version string
     * @param log Callback for logging progress
     * @return Result containing the path to the generated ZIP file
     */
    suspend fun generateModule(
        context: Context,
        patchedJars: Map<String, File>,
        deviceCodename: String,
        androidVersion: String,
        jobOutputDir: File? = null,  // Job output directory containing module_extras.conf
        patchLog: String? = null,
        log: (String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val moduleName = "BruhPatcher_${deviceCodename}_${timestamp}"
            val workDir = File("/data/local/tmp/bruhpatcher/module_$timestamp")
            val outputZip = File(context.cacheDir, "$moduleName.zip")

            log("Creating module directory...")
            Shell.cmd("rm -rf ${workDir.absolutePath}").exec()
            Shell.cmd("mkdir -p ${workDir.absolutePath} && chmod -R 777 ${workDir.absolutePath}").exec()

            // Copy template files from assets
            log("Copying template files...")
            copyTemplateFromAssets(context, workDir, log)

            // Update module.prop with device-specific values
            log("Updating module.prop...")
            updateModuleProp(context, workDir, deviceCodename, androidVersion, timestamp)

            // Create system/framework directory
            log("Setting up framework directory structure...")
            Shell.cmd("mkdir -p ${workDir.absolutePath}/system/framework && chmod -R 777 ${workDir.absolutePath}/system").exec()

            // Copy patched JARs
            for ((name, jarFile) in patchedJars) {
                if (jarFile.exists()) {
                    log("Adding $name to module...")
                    val destPath = when {
                        name == "miui-services.jar" -> "${workDir.absolutePath}/system/system_ext/framework/$name"
                        else -> "${workDir.absolutePath}/system/framework/$name"
                    }

                    if (name == "miui-services.jar") {
                        Shell.cmd("mkdir -p ${workDir.absolutePath}/system/system_ext/framework").exec()
                        Shell.cmd("mkdir -p ${workDir.absolutePath}/system_ext/framework").exec()
                    }

                    val copyResult = Shell.cmd("cp ${jarFile.absolutePath} $destPath").exec()
                    if (!copyResult.isSuccess) {
                        return@withContext Result.failure(Exception("Failed to copy $name: ${copyResult.err.joinToString()}"))
                    }
                    Shell.cmd("chmod 644 $destPath").exec()

                    // Also mirror to /system_ext/framework for direct VFS path redirection on Android 12+
                    if (name == "miui-services.jar") {
                        val mirrorPath = "${workDir.absolutePath}/system_ext/framework/$name"
                        Shell.cmd("cp ${jarFile.absolutePath} $mirrorPath").exec()
                        Shell.cmd("chmod 644 $mirrorPath").exec()
                    }
                }
            }
            
            // Embed patch.log in the module package using cacheDir staging to avoid root EACCES
            if (!patchLog.isNullOrBlank()) {
                try {
                    val tempLogFile = File(context.cacheDir, "temp_patch_${timestamp}.log")
                    tempLogFile.writeText(patchLog)
                    val moduleLogFile = File(workDir, "patch.log")
                    Shell.cmd("cp ${tempLogFile.absolutePath} ${moduleLogFile.absolutePath} && chmod 644 ${moduleLogFile.absolutePath}").exec()
                    tempLogFile.delete()
                    log("Embedded patch.log inside module package")
                } catch (e: Exception) {
                    log("Warning: Could not embed patch.log: ${e.message}")
                }
            }
            
            // Process module extras from feature scripts (APKs, XMLs, libs, etc.)
            // Use jobOutputDir if provided, otherwise try to derive from patchedJars
            val extrasDir = jobOutputDir ?: patchedJars.values.firstOrNull()?.parentFile
            val extrasConfigPath = extrasDir?.let { "${it.absolutePath}/${DiExecutor.MODULE_EXTRAS_CONFIG}" }
            
            log("DEBUG: extrasDir = ${extrasDir?.absolutePath}")
            log("DEBUG: extrasConfigPath = $extrasConfigPath")
            
            // Use shell to check file existence since it's in a root-owned directory
            if (extrasConfigPath != null) {
                val checkResult = Shell.cmd("[ -f '$extrasConfigPath' ] && echo 'exists' || echo 'not_found'").exec()
                log("DEBUG: file check result = ${checkResult.out.joinToString()}")
                
                if (checkResult.out.any { it.contains("exists") }) {
                    log("Processing module extras...")
                    processModuleExtras(File(extrasConfigPath), workDir, log)
                } else {
                    log("DEBUG: module_extras.conf not found, skipping extras")
                }
            }

            // Create the module ZIP
            log("Creating module ZIP...")
            
            val tempZipPath = "/data/local/tmp/bruhpatcher/${outputZip.name}"
            Shell.cmd("mkdir -p /data/local/tmp/bruhpatcher").exec()
            Shell.cmd("rm -f $tempZipPath").exec()
            
            // Find zip location with root
            val whichResult = Shell.cmd("su -c 'which zip'").exec()
            val zipPath = whichResult.out.firstOrNull()?.trim()
            
            var zipSuccess = false
            if (!zipPath.isNullOrBlank()) {
                log("Found zip at: $zipPath")
                val zipResult = Shell.cmd(
                    "su -c 'cd ${workDir.absolutePath} && $zipPath -r $tempZipPath .'"
                ).exec()
                
                if (zipResult.isSuccess) {
                    Shell.cmd(
                        "cp $tempZipPath ${outputZip.absolutePath}",
                        "chmod 644 ${outputZip.absolutePath}"
                    ).exec()
                    zipSuccess = outputZip.exists() && outputZip.length() > 0
                    if (zipSuccess) {
                        log("ZIP created with shell: ${outputZip.length() / 1024} KB")
                    }
                }
            }
            
            // Fallback to Java ZIP
            if (!zipSuccess) {
                log("Shell zip failed, using Java ZIP...")
                val localWorkDir = File(context.cacheDir, "module_work")
                localWorkDir.deleteRecursively()
                localWorkDir.mkdirs()
                
                Shell.cmd(
                    "cp -r ${workDir.absolutePath}/* ${localWorkDir.absolutePath}/",
                    "chmod -R 644 ${localWorkDir.absolutePath}",
                    "chmod -R +X ${localWorkDir.absolutePath}"
                ).exec()
                
                try {
                    java.util.zip.ZipOutputStream(java.io.FileOutputStream(outputZip)).use { zos ->
                        localWorkDir.walkTopDown().forEach { file ->
                            if (file.isFile) {
                                val entryName = file.relativeTo(localWorkDir).path
                                zos.putNextEntry(java.util.zip.ZipEntry(entryName))
                                file.inputStream().use { input ->
                                    input.copyTo(zos)
                                }
                                zos.closeEntry()
                            }
                        }
                    }
                    log("ZIP created with Java: ${outputZip.length() / 1024} KB")
                    zipSuccess = true
                } catch (e: Exception) {
                    log("Java ZIP failed: ${e.message}")
                }
                localWorkDir.deleteRecursively()
            }

            // Cleanup
            Shell.cmd("rm -rf ${workDir.absolutePath}").exec()

            // Verify ZIP was created
            if (!outputZip.exists() || outputZip.length() == 0L) {
                return@withContext Result.failure(Exception("ZIP file was not created properly"))
            }

            log("Module created: ${outputZip.name} (${outputZip.length() / 1024} KB)")
            Result.success(outputZip)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Copies template files from assets to the work directory
     */
    private fun copyTemplateFromAssets(context: Context, workDir: File, log: (String) -> Unit) {
        val assetManager = context.assets
        
        fun copyAssetDir(assetPath: String, destDir: File) {
            val files = assetManager.list(assetPath) ?: return
            
            if (files.isEmpty()) {
                // It's a file, copy it
                val destFile = File(destDir, File(assetPath).name)
                assetManager.open(assetPath).use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                // Copy to workDir via root
                Shell.cmd("cp ${destFile.absolutePath} ${workDir.absolutePath}/${File(assetPath).name}").exec()
                destFile.delete()
            } else {
                // It's a directory
                val subDir = if (assetPath == TEMPLATE_ASSETS_PATH) {
                    workDir
                } else {
                    val relativePath = assetPath.removePrefix("$TEMPLATE_ASSETS_PATH/")
                    File(workDir, relativePath).also {
                        Shell.cmd("mkdir -p ${it.absolutePath}").exec()
                    }
                }
                
                for (file in files) {
                    // Skip git-related files
                    if (file.startsWith(".git")) continue
                    
                    val fullPath = "$assetPath/$file"
                    val subFiles = assetManager.list(fullPath) ?: emptyArray()
                    
                    if (subFiles.isEmpty()) {
                        // It's a file
                        val tempFile = File(context.cacheDir, file)
                        assetManager.open(fullPath).use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        val destPath = "${subDir.absolutePath}/$file"
                        Shell.cmd("cp ${tempFile.absolutePath} $destPath").exec()
                        tempFile.delete()
                    } else {
                        // It's a subdirectory
                        copyAssetDir(fullPath, workDir)
                    }
                }
            }
        }
        
        copyAssetDir(TEMPLATE_ASSETS_PATH, workDir)
        
        // Set proper permissions
        Shell.cmd("chmod -R 755 ${workDir.absolutePath}").exec()
        Shell.cmd("chmod 644 ${workDir.absolutePath}/module.prop").exec()
        Shell.cmd("chmod 644 ${workDir.absolutePath}/system.prop").exec()
        Shell.cmd("chmod 755 ${workDir.absolutePath}/service.sh").exec()
        Shell.cmd("chmod 755 ${workDir.absolutePath}/post-fs-data.sh").exec()
        Shell.cmd("chmod 755 ${workDir.absolutePath}/customize.sh").exec()
        Shell.cmd("chmod 755 ${workDir.absolutePath}/uninstall.sh").exec()
    }

    /**
     * Updates module.prop with device-specific values
     */
    private fun updateModuleProp(
        context: Context,
        workDir: File,
        deviceCodename: String,
        androidVersion: String,
        timestamp: String
    ) {
        val apiLevel = Build.VERSION.SDK_INT
        val versionCode = System.currentTimeMillis() / 1000
        
        val moduleProp = """
            id=$MODULE_ID
            name=Bruh Patcher Patched Framework
            version=v2.3.0_$timestamp
            versionCode=$versionCode
            author=Bruh Patcher (nothingnesscore)
            description=Universal patched framework for $deviceCodename (Android $androidVersion) with Kaorios v2.0.6.0 & CorePatch [NoMount VFS Compatible]
            minMagisk=20400
            ksu=1
            minKsu=10904
            sufs=1
            minSufs=10000
            minApi=26
            maxApi=37
            requireReboot=true
            support=https://github.com/nothingnesscore/BruhPatcher
        """.trimIndent()

        val propFile = File(context.cacheDir, "module.prop")
        propFile.writeText(moduleProp)
        Shell.cmd("cp ${propFile.absolutePath} ${workDir.absolutePath}/module.prop").exec()
        propFile.delete()
    }

    /**
     * Moves the generated module to the public Downloads folder
     */
    suspend fun moveToDownloads(moduleFile: File): Result<File> {
        return RootManager.moveToDownloads(moduleFile, moduleFile.name)
    }
    
    /**
     * Processes module extras config file and copies files to the module
     * Config format: type|source_path|dest_path (one per line)
     * Types: apk, xml, lib, props, file
     */
    private fun processModuleExtras(configFile: File, workDir: File, log: (String) -> Unit) {
        try {
            // Read config via root since it's in /data/local/tmp
            val catResult = Shell.cmd("cat ${configFile.absolutePath}").exec()
            if (!catResult.isSuccess) {
                log("Could not read module extras config")
                return
            }
            
            val lines = catResult.out.filter { it.isNotBlank() }
            if (lines.isEmpty()) {
                log("No module extras to process")
                return
            }
            
            log("Found ${lines.size} module extra(s) to add")
            
            for (line in lines) {
                val parts = line.split("|")
                if (parts.size < 3) {
                    log("Invalid config line: $line")
                    continue
                }
                
                val type = parts[0]
                val sourcePath = parts[1]
                val destPath = parts[2]
                
                // Special handling for props - append to system.prop instead of copying
                if (type == "props") {
                    val systemPropPath = "${workDir.absolutePath}/system.prop"
                    val appendResult = Shell.cmd("cat \"$sourcePath\" >> \"$systemPropPath\"").exec()
                    if (appendResult.isSuccess) {
                        log("Appended props to system.prop")
                    } else {
                        log("Failed to append props: ${appendResult.err.joinToString()}")
                    }
                    continue
                }
                
                val fullDestPath = "${workDir.absolutePath}/$destPath"
                val destDir = File(fullDestPath).parent
                
                // Create destination directory
                Shell.cmd("mkdir -p \"$destDir\"").exec()
                
                // Copy file
                val copyResult = Shell.cmd("cp \"$sourcePath\" \"$fullDestPath\"").exec()
                if (copyResult.isSuccess) {
                    // Set appropriate permissions based on type
                    val perm = when (type) {
                        "apk" -> "644"
                        "xml" -> "644"
                        "lib" -> "755"
                        else -> "644"
                    }
                    Shell.cmd("chmod $perm \"$fullDestPath\"").exec()
                    log("Added $type: $destPath")
                } else {
                    log("Failed to add: $destPath - ${copyResult.err.joinToString()}")
                }
            }
        } catch (e: Exception) {
            log("Error processing module extras: ${e.message}")
        }
    }
}
