package com.nothingness.bruhpatcher.core

import android.content.Context
import com.topjohnwu.superuser.Shell
import java.io.File

/**
 * Represents a patch feature script with metadata
 */
data class PatchFeature(
    val id: String,
    val name: String,
    val description: String,
    val runtimePath: String, // Path in the safe runtime directory
    val isUserFeature: Boolean
)

/**
 * Represents a local patch feature for UI display (toggleable) with sequence step metadata
 */
data class LocalPatchFeature(
    val id: String,
    val name: String,
    val description: String,
    val requiredJars: List<String> = listOf("framework.jar"),
    val isEnabled: Boolean = true,
    val isUserFeature: Boolean = false,
    val stepNumber: Int = 1,
    val categoryName: String = "Step 1: Core Foundation & A17",
    val isCorePrerequisite: Boolean = false,
    val orderWarning: String? = null
)

/**
 * Manages feature discovery and deployment to safe runtime directories
 * 
 * All feature scripts are copied to /data/local/tmp/frameworkforge/features/
 * with proper chmod 755 to ensure they can be executed regardless of SELinux context.
 */
object FeatureManager {

    private const val BUILTIN_ASSETS_PATH = "features"
    private const val USER_STORAGE_PATH = "features_user"
    private const val UPDATED_STORAGE_PATH = "features_updated"
    
    // Safe runtime directories (noexec-safe)
    private const val BRUH_RUNTIME_ROOT = "/data/local/tmp/bruhpatcher"
    private const val FEATURES_RUNTIME_DIR = "$BRUH_RUNTIME_ROOT/features"
    private const val BUILTIN_RUNTIME_DIR = "$FEATURES_RUNTIME_DIR/builtin"
    private const val USER_RUNTIME_DIR = "$FEATURES_RUNTIME_DIR/user"
    private const val KAORIOS_ASSETS_PATH = "kaorios"
    private const val KAORIOS_RUNTIME_DIR = "$BRUH_RUNTIME_ROOT/kaorios"

    /**
     * Clears cached updated scripts from disk so built-in assets take precedence
     */
    fun clearUpdatedFeatures(context: Context) {
        try {
            val updatedDir = File(context.filesDir, UPDATED_STORAGE_PATH)
            if (updatedDir.exists()) {
                updatedDir.deleteRecursively()
            }
        } catch (_: Exception) {}
    }

    val CANONICAL_FEATURE_ORDER = listOf(
        "android17_build_unfinalize",
        "kaorios_core",
        "core_patch_dsv",
        "disable_flag_secure",
        "hide_developer_adb",
        "hide_installed_apps",
        "installer_source_spoof",
        "settings_filtering"
    )

    fun getFeatureStepInfo(id: String): Triple<Int, String, String?> {
        return when (id) {
            "android17_build_unfinalize" -> Triple(1, "Step 1: Core Foundation & A17", null)
            "kaorios_core" -> Triple(1, "Step 1: Core Foundation & A17", "⚠️ Core Foundation must be patched and boot-tested first before enabling supplementary features!")
            "core_patch_dsv" -> Triple(2, "Step 2: Signature & Downgrade (CorePatch / DSV)", null)
            "disable_flag_secure" -> Triple(3, "Step 3: Privacy, Stealth & FLAG_SECURE", null)
            "hide_developer_adb" -> Triple(3, "Step 3: Privacy, Stealth & FLAG_SECURE", null)
            "hide_installed_apps" -> Triple(3, "Step 3: Privacy, Stealth & FLAG_SECURE", null)
            "installer_source_spoof" -> Triple(4, "Step 4: System Identity & Provider Spoofing", null)
            "settings_filtering" -> Triple(4, "Step 4: System Identity & Provider Spoofing", null)
            else -> Triple(5, "Custom / User Scripts", null)
        }
    }

    /**
     * Gets available patch features from assets/updated directory without deploying them
     * Used for UI display in local patching mode
     * 
     * Priority: features_updated (downloaded updates) -> assets (built-in)
     */
    fun getLocalPatchFeatures(context: Context): List<LocalPatchFeature> {
        val result = mutableListOf<LocalPatchFeature>()
        val processedIds = mutableSetOf<String>()
        
        // 1. Check for updated features first (downloaded from GitHub)
        val updatedDir = File(context.filesDir, UPDATED_STORAGE_PATH)
        updatedDir.listFiles()?.filter { it.name.endsWith(".sh") }?.forEach { file ->
            try {
                val metadata = parseFeatureMetadata(file)
                val id = file.name.removeSuffix(".sh")
                val (step, cat, warn) = getFeatureStepInfo(id)
                result.add(LocalPatchFeature(
                    id = id,
                    name = metadata.name,
                    description = metadata.description,
                    requiredJars = metadata.requiredJars,
                    isEnabled = true,
                    isUserFeature = false,  // Updated built-in, not user feature
                    stepNumber = step,
                    categoryName = cat,
                    isCorePrerequisite = (id == "kaorios_core"),
                    orderWarning = warn
                ))
                processedIds.add(id)
            } catch (_: Exception) { }
        }
        
        // 2. Get built-in features from assets (skip if already updated)
        val files = context.assets.list(BUILTIN_ASSETS_PATH) ?: emptyArray()
        files.filter { it.endsWith(".sh") }.forEach { filename ->
            val id = filename.removeSuffix(".sh")
            if (id in processedIds) return@forEach  // Skip if updated version exists
            
            try {
                val cacheFile = File(context.cacheDir, filename)
                context.assets.open("$BUILTIN_ASSETS_PATH/$filename").use { input ->
                    cacheFile.outputStream().use { os -> input.copyTo(os) }
                }
                val metadata = parseFeatureMetadata(cacheFile)
                cacheFile.delete()
                
                val (step, cat, warn) = getFeatureStepInfo(id)
                result.add(LocalPatchFeature(
                    id = id,
                    name = metadata.name,
                    description = metadata.description,
                    requiredJars = metadata.requiredJars,
                    isEnabled = true,
                    isUserFeature = false,
                    stepNumber = step,
                    categoryName = cat,
                    isCorePrerequisite = (id == "kaorios_core"),
                    orderWarning = warn
                ))
                processedIds.add(id)
            } catch (_: Exception) { }
        }
        
        // 3. Get user-imported features (custom scripts)
        val userDir = File(context.filesDir, USER_STORAGE_PATH)
        userDir.listFiles()?.filter { it.name.endsWith(".sh") }?.forEach { file ->
            val id = file.name.removeSuffix(".sh")
            if (id in processedIds) return@forEach  // Skip duplicates
            
            try {
                val metadata = parseFeatureMetadata(file)
                val (step, cat, warn) = getFeatureStepInfo(id)
                result.add(LocalPatchFeature(
                    id = id,
                    name = metadata.name,
                    description = metadata.description,
                    requiredJars = metadata.requiredJars,
                    isEnabled = true,
                    isUserFeature = true,
                    stepNumber = step,
                    categoryName = cat,
                    isCorePrerequisite = false,
                    orderWarning = warn
                ))
            } catch (_: Exception) { }
        }
        
        return result.sortedWith(compareBy<LocalPatchFeature> {
            val idx = CANONICAL_FEATURE_ORDER.indexOf(it.id)
            if (idx != -1) idx else 100
        }.thenBy { it.name })
    }

    /**
     * Deploys all features to the safe runtime directory and returns their references
     * This must be called BEFORE executing any patches
     */
    fun deployFeatures(context: Context): List<PatchFeature> {
        // Create runtime directories with proper permissions
        Shell.cmd(
            "mkdir -p $BUILTIN_RUNTIME_DIR",
            "mkdir -p $USER_RUNTIME_DIR",
            "mkdir -p $KAORIOS_RUNTIME_DIR",
            "chmod -R 755 $BRUH_RUNTIME_ROOT"
        ).exec()
        
        // Deploy Kaorios DEX and attestation files for on-device injection
        deployKaoriosAssets(context)
        
        val result = mutableListOf<PatchFeature>()
        result += deployBuiltinFeatures(context)
        result += deployUserFeatures(context)
        return result
    }

    /**
     * Deploys Kaorios framework DEX, configuration, and companion APK/libraries to runtime directory
     */
    private fun deployKaoriosAssets(context: Context) {
        fun copyAssetTree(assetSubDir: String) {
            val fullAssetPath = if (assetSubDir.isEmpty()) KAORIOS_ASSETS_PATH else "$KAORIOS_ASSETS_PATH/$assetSubDir"
            val entries = context.assets.list(fullAssetPath) ?: return
            for (entry in entries) {
                val relPath = if (assetSubDir.isEmpty()) entry else "$assetSubDir/$entry"
                val entryAssetPath = "$KAORIOS_ASSETS_PATH/$relPath"
                val subEntries = context.assets.list(entryAssetPath) ?: emptyArray()

                if (subEntries.isEmpty()) {
                    // Leaf file
                    try {
                        val customKeybox = if (entry == "Keybox.xml") KeyboxManager.getLocalKeybox(context) else null
                        val sourceFile = if (customKeybox != null && customKeybox.exists()) {
                            customKeybox
                        } else {
                            val cacheFile = File(context.cacheDir, "k_${entry.replace('/', '_')}")
                            context.assets.open(entryAssetPath).use { input ->
                                cacheFile.outputStream().use { os -> input.copyTo(os) }
                            }
                            cacheFile
                        }

                        val runtimePath = "$KAORIOS_RUNTIME_DIR/$relPath"
                        val parentDir = File(runtimePath).parent
                        if (parentDir != null) {
                            Shell.cmd("mkdir -p \"$parentDir\"").exec()
                        }
                        Shell.cmd(
                            "cp \"${sourceFile.absolutePath}\" \"$runtimePath\"",
                            "chmod 644 \"$runtimePath\""
                        ).exec()

                        if (sourceFile != customKeybox) {
                            sourceFile.delete()
                        }
                    } catch (e: Exception) { }
                } else {
                    // Directory: recurse
                    copyAssetTree(relPath)
                }
            }
        }

        try {
            copyAssetTree("")
        } catch (e: Exception) { }
    }

    /**
     * Deploys built-in features from assets/updated to runtime directory
     * Priority: features_updated (downloaded) -> assets (built-in)
     */
    private fun deployBuiltinFeatures(context: Context): List<PatchFeature> {
        val result = mutableListOf<PatchFeature>()
        val processedIds = mutableSetOf<String>()
        
        // 1. Deploy updated features first (downloaded from GitHub)
        val updatedDir = File(context.filesDir, UPDATED_STORAGE_PATH)
        updatedDir.listFiles()?.filter { it.name.endsWith(".sh") }?.forEach { file ->
            try {
                // Ensure strict LF line endings
                val rawText = file.readText(Charsets.UTF_8)
                val normalized = rawText.replace("\r\n", "\n").replace("\r", "\n")
                file.writeBytes(normalized.toByteArray(Charsets.UTF_8))

                val metadata = parseFeatureMetadata(file)
                val id = file.name.removeSuffix(".sh")
                
                // Copy to safe runtime directory via root
                val runtimePath = "$BUILTIN_RUNTIME_DIR/${file.name}"
                val copyResult = Shell.cmd(
                    "cp ${file.absolutePath} $runtimePath",
                    "chmod 755 $runtimePath",
                    "dos2unix $runtimePath 2>/dev/null || sed -i 's/\\r\$//' $runtimePath 2>/dev/null || true"
                ).exec()
                
                if (copyResult.isSuccess) {
                    result.add(PatchFeature(
                        id = id,
                        name = metadata.name,
                        description = metadata.description,
                        runtimePath = runtimePath,
                        isUserFeature = false
                    ))
                    processedIds.add(id)
                }
            } catch (_: Exception) { }
        }
        
        // 2. Deploy assets (skip if already updated)
        val files = context.assets.list(BUILTIN_ASSETS_PATH) ?: return result
        
        files.filter { it.endsWith(".sh") }.forEach { filename ->
            val id = filename.removeSuffix(".sh")
            if (id in processedIds) return@forEach  // Skip if updated version exists
            
            try {
                // First copy to cache (accessible from app context) with strict LF normalization
                val cacheFile = File(context.cacheDir, filename)
                val rawText = context.assets.open("$BUILTIN_ASSETS_PATH/$filename").bufferedReader(Charsets.UTF_8).use { it.readText() }
                val normalized = rawText.replace("\r\n", "\n").replace("\r", "\n")
                cacheFile.writeBytes(normalized.toByteArray(Charsets.UTF_8))
                
                // Parse metadata from cache file
                val metadata = parseFeatureMetadata(cacheFile)
                
                // Copy to safe runtime directory via root
                val runtimePath = "$BUILTIN_RUNTIME_DIR/$filename"
                val copyResult = Shell.cmd(
                    "cp ${cacheFile.absolutePath} $runtimePath",
                    "chmod 755 $runtimePath",
                    "dos2unix $runtimePath 2>/dev/null || sed -i 's/\\r\$//' $runtimePath 2>/dev/null || true"
                ).exec()
                
                // Clean up cache file
                cacheFile.delete()
                
                if (copyResult.isSuccess) {
                    result.add(PatchFeature(
                        id = id,
                        name = metadata.name,
                        description = metadata.description,
                        runtimePath = runtimePath,
                        isUserFeature = false
                    ))
                }
            } catch (_: Exception) { }
        }
        
        return result
    }

    /**
     * Deploys user-imported features to runtime directory
     */
    private fun deployUserFeatures(context: Context): List<PatchFeature> {
        val userDir = File(context.filesDir, USER_STORAGE_PATH)
        if (!userDir.exists()) userDir.mkdirs()

        return userDir.listFiles()?.filter { it.name.endsWith(".sh") }?.mapNotNull { file ->
            try {
                // Ensure strict LF line endings on imported custom scripts
                val rawText = file.readText(Charsets.UTF_8)
                val normalized = rawText.replace("\r\n", "\n").replace("\r", "\n")
                file.writeBytes(normalized.toByteArray(Charsets.UTF_8))

                val metadata = parseFeatureMetadata(file)
                
                // Copy to safe runtime directory
                val runtimePath = "$USER_RUNTIME_DIR/${file.name}"
                val copyResult = Shell.cmd(
                    "cp ${file.absolutePath} $runtimePath",
                    "chmod 755 $runtimePath",
                    "dos2unix $runtimePath 2>/dev/null || sed -i 's/\\r\$//' $runtimePath 2>/dev/null || true"
                ).exec()
                
                if (copyResult.isSuccess) {
                    PatchFeature(
                        id = file.name.removeSuffix(".sh"),
                        name = metadata.name,
                        description = metadata.description,
                        runtimePath = runtimePath,
                        isUserFeature = true
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()
    }

    /**
     * Feature metadata holder
     */
    data class FeatureMetadata(
        val name: String,
        val description: String,
        val requiredJars: List<String>
    )

    /**
     * Parses feature metadata from script header comments
     * Supports comma-separated requires (e.g., #@requires framework.jar,services.jar)
     */
    private fun parseFeatureMetadata(file: File): FeatureMetadata {
        var name = file.nameWithoutExtension.replace("_", " ")
        var desc = "No description"
        val requires = mutableListOf<String>()

        file.forEachLine { line ->
            when {
                line.startsWith("#@name") -> name = line.removePrefix("#@name").trim()
                line.startsWith("#@description") -> desc = line.removePrefix("#@description").trim()
                line.startsWith("#@requires") -> {
                    // Support both single and comma-separated requires
                    val reqValue = line.removePrefix("#@requires").trim()
                    requires.addAll(reqValue.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                }
            }
        }

        // Default to framework.jar if no requires specified
        if (requires.isEmpty()) requires.add("framework.jar")

        return FeatureMetadata(name, desc, requires)
    }

    /**
     * Gets enabled feature scripts matching the selected UI feature IDs in strict canonical order
     */
    fun getEnabledScripts(context: Context, enabledFeatureIds: List<String>): List<PatchFeature> {
        val allFeatures = deployFeatures(context)
        val matched = allFeatures.filter { feature ->
            enabledFeatureIds.any { id ->
                feature.id.contains(id, ignoreCase = true) ||
                feature.name.replace(" ", "_").lowercase().contains(id.lowercase())
            }
        }
        return matched.sortedWith(compareBy { feature ->
            val idx = CANONICAL_FEATURE_ORDER.indexOfFirst { feature.id.contains(it, ignoreCase = true) }
            if (idx != -1) idx else 100
        })
    }

    /**
     * Deletes a user-imported feature script
     */
    fun deleteUserFeature(context: Context, featureId: String): Boolean {
        val userDir = File(context.filesDir, USER_STORAGE_PATH)
        val file = File(userDir, "$featureId.sh")
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    /**
     * Cleans up runtime feature directory
     */
    fun cleanup() {
        Shell.cmd("rm -rf $BRUH_RUNTIME_ROOT").exec()
    }

    /**
     * Builds a comma-separated string of feature names for logging
     */
    fun buildFeatureString(features: List<PatchFeature>): String {
        return features.joinToString(", ") { it.name }
    }

    /**
     * Builds a comma-separated string of feature IDs for workflow (for UI Feature model)
     */
    @JvmName("buildFeatureIdString")
    fun buildFeatureString(features: List<com.nothingness.bruhpatcher.model.Feature>): String {
        return features.filter { it.isEnabled }.joinToString(",") { it.id }
    }

    /**
     * Gets available UI features based on device capabilities
     */
    fun getAvailableFeatures(hasMiuiServices: Boolean): List<com.nothingness.bruhpatcher.model.Feature> {
        return com.nothingness.bruhpatcher.model.Feature.getAllFeatures().map { feature ->
            if (feature.requiresMiui && !hasMiuiServices) {
                feature.copy(isEnabled = false)
            } else {
                feature
            }
        }
    }

    /**
     * Updates a feature's enabled state
     */
    fun updateFeature(
        features: List<com.nothingness.bruhpatcher.model.Feature>,
        featureId: String,
        enabled: Boolean
    ): List<com.nothingness.bruhpatcher.model.Feature> {
        return features.map { feature ->
            if (feature.id == featureId) {
                feature.copy(isEnabled = enabled)
            } else {
                feature
            }
        }
    }

    /**
     * Gets a summary string of enabled features for display
     */
    fun getEnabledFeaturesSummary(features: List<com.nothingness.bruhpatcher.model.Feature>): String {
        val enabled = features.filter { it.isEnabled }
        return when {
            enabled.isEmpty() -> "None selected"
            enabled.size == 1 -> enabled.first().displayName
            else -> "${enabled.size} features selected"
        }
    }
}
