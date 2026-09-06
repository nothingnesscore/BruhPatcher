package com.nothingness.bruhpatcher.core

import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Executes DI (DynamicInstaller) job scripts with live output streaming.
 */
object DiExecutor {

    private const val DI_ENVIRONMENT = "/data/local/di/environment"
    
    /** Config file for module extras - feature scripts write to this */
    const val MODULE_EXTRAS_CONFIG = "module_extras.conf"

    /**
     * Runs the job's run.sh script with live log streaming.
     */
    suspend fun runJob(
        jobDir: File,
        log: (String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val runScript = File(jobDir, "run.sh")
        
        if (!runScript.exists()) {
            log("ERROR: run.sh not found in ${jobDir.absolutePath}")
            return@withContext 1
        }

        val stdoutCallback = object : CallbackList<String>() {
            override fun onAddElement(e: String) { log(e) }
        }

        val stderrCallback = object : CallbackList<String>() {
            override fun onAddElement(e: String) { log("[ERR] $e") }
        }

        log("Executing job: ${jobDir.name}")

        val diBash = "/data/tmp/di/bin/bash"
        val result = Shell.cmd("su -c '$diBash ${runScript.absolutePath}'")
            .to(stdoutCallback, stderrCallback)
            .exec()

        if (result.isSuccess) {
            log("Job script finished execution")
        } else {
            log("Job failed with exit code: ${result.code}")
        }

        result.code
    }

    /**
     * Generates a run.sh script that handles multiple JARs and verifies modification.
     */
    fun generateRunScript(
        context: android.content.Context,
        jobDir: File,
        inputFiles: Map<String, String>,
        features: List<PatchFeature>,
        apiLevel: Int,
        deviceCodename: String
    ): File {
        val runScript = File(jobDir, "run.sh")

        val scriptContent = buildString {
            appendLine("#!/data/tmp/di/bin/bash")
            appendLine("set +e")

            appendLine("# Job context")
            appendLine("export API_LEVEL=$apiLevel")
            appendLine("export DEVICE_CODENAME=\"$deviceCodename\"")
            appendLine("export JOB_DIR=\"${jobDir.absolutePath}\"")
            appendLine("export WORK_DIR=\"${jobDir.absolutePath}/work\"")
            appendLine("export OUTPUT_DIR=\"${jobDir.absolutePath}/output\"")

            // Export ALL input files so feature scripts can find them
            inputFiles.forEach { (name, path) ->
                when(name) {
                    "framework.jar" -> appendLine("export FRAMEWORK_JAR=\"$path\"")
                    "services.jar" -> appendLine("export SERVICES_JAR=\"$path\"")
                    "miui-services.jar" -> appendLine("export MIUI_SERVICES_JAR=\"$path\"")
                }
            }

            appendLine("")
            appendLine("# Create dirs")
            appendLine("mkdir -p \"\$WORK_DIR\"")
            appendLine("mkdir -p \"\$OUTPUT_DIR\"")
            appendLine("")
            
            // Module extras configuration file
            appendLine("# Module extras config (feature scripts can add files to module)")
            appendLine("export MODULE_EXTRAS_CONFIG=\"\$OUTPUT_DIR/$MODULE_EXTRAS_CONFIG\"")
            appendLine("rm -f \"\$MODULE_EXTRAS_CONFIG\"")
            appendLine("touch \"\$MODULE_EXTRAS_CONFIG\"")
            appendLine("")
            
            // Add the add_to_module shell function
            appendLine("# Generic function to add files to module")
            appendLine("# Usage: add_to_module <source_path> <dest_path_in_module> [type]")
            appendLine("# Types: apk, xml, lib, file (default)")
            appendLine("# Example: add_to_module \"/tmp/MyApp.apk\" \"system/priv-app/MyApp/MyApp.apk\" \"apk\"")
            appendLine("add_to_module() {")
            appendLine("    local src=\"\$1\"")
            appendLine("    local dest=\"\$2\"")
            appendLine("    local type=\"\${3:-file}\"")
            appendLine("    if [ -z \"\$src\" ] || [ -z \"\$dest\" ]; then")
            appendLine("        echo \"[!] add_to_module: missing source or destination\"")
            appendLine("        return 1")
            appendLine("    fi")
            appendLine("    if [ ! -e \"\$src\" ]; then")
            appendLine("        echo \"[!] add_to_module: source not found: \$src\"")
            appendLine("        return 1")
            appendLine("    fi")
            appendLine("    echo \"\$type|\$src|\$dest\" >> \"\$MODULE_EXTRAS_CONFIG\"")
            appendLine("    echo \"[+] Module extra registered: \$dest (\$type)\"")
            appendLine("    return 0")
            appendLine("}")
            appendLine("")
            appendLine("# Function to extract and add native libs from APK")
            appendLine("# Usage: add_apk_libs <apk_path> <lib_dest_dir>")
            appendLine("add_apk_libs() {")
            appendLine("    local apk=\"\$1\"")
            appendLine("    local dest_base=\"\$2\"")
            appendLine("    local extract_dir=\"\$TMP/lib_extract_\$\$\"")
            appendLine("    create_dir \"\$extract_dir\"")
            appendLine("    unzip -q -o \"\$apk\" \"lib/*\" -d \"\$extract_dir\" 2>/dev/null || return 0")
            appendLine("    if [ -d \"\$extract_dir/lib\" ]; then")
            appendLine("        # Map Android ABI names to Magisk module names")
            appendLine("        for abi_dir in \"\$extract_dir/lib/\"*; do")
            appendLine("            [ -d \"\$abi_dir\" ] || continue")
            appendLine("            local abi=\$(basename \"\$abi_dir\")")
            appendLine("            local target_abi=\"\$abi\"")
            appendLine("            case \"\$abi\" in")
            appendLine("                armeabi-v7a) target_abi=\"arm\" ;;")
            appendLine("                arm64-v8a) target_abi=\"arm64\" ;;")
            appendLine("            esac")
            appendLine("            for so_file in \"\$abi_dir\"/*.so; do")
            appendLine("                [ -f \"\$so_file\" ] || continue")
            appendLine("                local so_name=\$(basename \"\$so_file\")")
            appendLine("                add_to_module \"\$so_file\" \"\$dest_base/lib/\$target_abi/\$so_name\" \"lib\"")
            appendLine("            done")
            appendLine("        done")
            appendLine("        echo \"[+] Native libraries extracted and registered\"")
            appendLine("    fi")
            appendLine("    rm -rf \"\$extract_dir\"")
            appendLine("}")
            appendLine("")
            appendLine("export DI_BIN=\"/data/tmp/di/bin\"")
            appendLine("export DI_TMP=\"/data/tmp/di\"")
            appendLine("export TMP=\"/data/tmp/di\"")
            appendLine("export TMPDIR=\"/data/tmp/di\"")
            appendLine("export PATH=\"\$DI_BIN:\$PATH\"")
            appendLine("export l=\"\$DI_BIN\"")

            appendLine("if [ -f \"\$DI_TMP/core\" ]; then . \"\$DI_TMP/core\"; else echo '[!] DI Core missing'; fi")
            
            // Source the workspace management library
            appendLine("")
            appendLine("# Source workspace management library")
            appendLine("if [ -f \"\$DI_TMP/smali_workspace.sh\" ]; then")
            appendLine("    . \"\$DI_TMP/smali_workspace.sh\"")
            appendLine("else")
            appendLine("    echo '[!] ERROR: smali_workspace.sh not found'")
            appendLine("    exit 1")
            appendLine("fi")
            appendLine("")
            
            // Initialize workspace system
            appendLine("# Initialize workspace system")
            appendLine("init_workspace")
            appendLine("")
            
            // Phase 1: Decompile all required JARs upfront
            appendLine("# Phase 1: Decompile all required JARs")
            appendLine("echo '[*] Decompiling JARs...'")
            inputFiles.forEach { (name, path) ->
                val envVar = when(name) {
                    "framework.jar" -> "FRAMEWORK_JAR"
                    "services.jar" -> "SERVICES_JAR"
                    "miui-services.jar" -> "MIUI_SERVICES_JAR"
                    else -> ""
                }
                if (envVar.isNotEmpty()) {
                    appendLine("decompile_jar \"$name\" \"\$$envVar\"")
                }
            }
            appendLine("")
            
            // Export workspace paths for convenience
            appendLine("# Export workspace paths for feature scripts")
            inputFiles.keys.forEach { name ->
                val envVar = when(name) {
                    "framework.jar" -> "FRAMEWORK_WORKSPACE"
                    "services.jar" -> "SERVICES_WORKSPACE"
                    "miui-services.jar" -> "MIUI_SERVICES_WORKSPACE"
                    else -> ""
                }
                if (envVar.isNotEmpty()) {
                    appendLine("export $envVar=\$(get_workspace_path \"$name\")")
                }
            }
            appendLine("")

            // Calculate initial checksums
            appendLine("# Calculate initial checksums")
            appendLine("echo '[*] Calculating initial checksums...'")
            inputFiles.keys.forEach { name ->
                val envVar = when(name) {
                    "framework.jar" -> "FRAMEWORK_JAR"
                    "services.jar" -> "SERVICES_JAR"
                    "miui-services.jar" -> "MIUI_SERVICES_JAR"
                    else -> ""
                }
                if (envVar.isNotEmpty()) {
                    appendLine("${envVar}_MD5_PRE=$(md5sum \"$$envVar\" | cut -d' ' -f1)")
                }
            }

            // 2. Run Features
            appendLine("")
            appendLine("echo '[*] Applying ${features.size} features...'")
            features.forEachIndexed { index, feature ->
                appendLine("echo '[*] [${index + 1}/${features.size}] Feature: ${feature.name}'")
                // Source the script. We pass FRAMEWORK_JAR as arg 1 for backward compatibility,
                // but scripts should preferably use env vars now.
                appendLine(". \"${feature.runtimePath}\"")
            }
            appendLine("")

            // Phase 3: Recompile all modified workspaces
            appendLine("# Phase 3: Recompile all modified JARs")
            appendLine("echo '[*] Recompiling modified JARs...'")
            appendLine("if ! recompile_all; then")
            appendLine("    echo '[!] ERROR: Recompilation failed'")
            appendLine("    cleanup_workspaces")
            appendLine("    exit 1")
            appendLine("fi")
            appendLine("")

            // Verify modifications and copy to output
            appendLine("# Verify modifications and copy to output")
            appendLine("echo '[*] Verifying patches...'")
            inputFiles.keys.forEach { name ->
                val envVar = when(name) {
                    "framework.jar" -> "FRAMEWORK_JAR"
                    "services.jar" -> "SERVICES_JAR"
                    "miui-services.jar" -> "MIUI_SERVICES_JAR"
                    else -> ""
                }

                if (envVar.isNotEmpty()) {
                    appendLine("${envVar}_MD5_POST=$(md5sum \"$$envVar\" | cut -d' ' -f1)")

                    // Compare MD5
                    appendLine("if [ \"\$${envVar}_MD5_PRE\" != \"\$${envVar}_MD5_POST\" ]; then")
                    appendLine("    echo '[SUCCESS] $name was modified by patches.'")
                    appendLine("    cp \"$$envVar\" \"\$OUTPUT_DIR/$name\"")
                    appendLine("else")
                    appendLine("    echo '[WARNING] $name was NOT modified. Copying original to output.'")
                    // Still copy it so the module generator has the file, but warn the user
                    appendLine("    cp \"$$envVar\" \"\$OUTPUT_DIR/$name\"")
                    appendLine("fi")
                }
            }
            
            // Cleanup workspaces
            appendLine("")
            appendLine("# Cleanup workspaces")
            appendLine("cleanup_workspaces")

            appendLine("echo '[*] Job finished'")
            appendLine("exit 0")
        }

        val tmpFile = File.createTempFile("run_", ".sh", context.cacheDir)
        tmpFile.writeText(scriptContent)

        Shell.cmd(
            "cp ${tmpFile.absolutePath} ${runScript.absolutePath}",
            "chmod 755 ${runScript.absolutePath}"
        ).exec()

        tmpFile.delete()
        return runScript
    }
}