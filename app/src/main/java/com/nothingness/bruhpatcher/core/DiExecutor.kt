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

        var fatalErrorDetected = false
        var fatalErrorSnippet = ""

        val fatalPatterns = listOf(
            "FATAL ERROR",
            "ERROR: Cant decompile",
            "ERROR: Cant compile",
            "ERROR: Cant decode",
            "brut.androlib.exceptions.",
            "CommandLine\$UnmatchedArgumentException",
            "[!] Failed to decompile",
            "[!] Failed to recompile",
            "Exception in thread",
            "NullPointerException"
        )

        val stdoutCallback = object : CallbackList<String>() {
            override fun onAddElement(e: String) {
                if (fatalPatterns.any { e.contains(it, ignoreCase = true) }) {
                    fatalErrorDetected = true
                    fatalErrorSnippet = e
                }
                log(e)
            }
        }

        val stderrCallback = object : CallbackList<String>() {
            override fun onAddElement(e: String) {
                if (fatalPatterns.any { e.contains(it, ignoreCase = true) }) {
                    fatalErrorDetected = true
                    fatalErrorSnippet = e
                }
                log("[ERR] $e")
            }
        }

        log("Executing job: ${jobDir.name}")

        val diBash = "/data/tmp/di/bin/bash"
        Shell.cmd("chmod -R 755 /data/tmp/di/bin /data/local/di 2>/dev/null || true").exec()
        val isBashExecutable = Shell.cmd("test -x $diBash").exec().isSuccess
        val execCmd = if (isBashExecutable) "$diBash ${runScript.absolutePath}" else "sh ${runScript.absolutePath}"
        val result = Shell.cmd(execCmd)
            .to(stdoutCallback, stderrCallback)
            .exec()

        if (result.isSuccess && !fatalErrorDetected) {
            log("Job script finished execution successfully")
            0
        } else {
            val code = if (result.code != 0) result.code else 1
            if (fatalErrorDetected) {
                log("CRITICAL: Fatal patching error detected in output stream: $fatalErrorSnippet")
            }
            log("Job failed with exit code: $code")
            code
        }
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
            appendLine("export dalvik_memory=\"2048m\"")
            appendLine("export DI_DALVIK_MEMORY=\"2048m\"")

            appendLine("if [ -f \"\$DI_TMP/core\" ]; then . \"\$DI_TMP/core\"; else echo '[!] FATAL ERROR: DI Core missing at \$DI_TMP/core'; exit 1; fi")
            
            // Source the workspace management library
            appendLine("")
            appendLine("# Source workspace management library")
            appendLine("cp -f /data/local/di/smali_workspace.sh \"\$DI_TMP/smali_workspace.sh\" 2>/dev/null || true")
            appendLine("chmod 755 \"\$DI_TMP/smali_workspace.sh\" 2>/dev/null || true")
            appendLine("if [ -f \"\$DI_TMP/smali_workspace.sh\" ]; then")
            appendLine("    . \"\$DI_TMP/smali_workspace.sh\"")
            appendLine("else")
            appendLine("    echo '[!] FATAL ERROR: smali_workspace.sh not found'")
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
                    appendLine("if ! decompile_jar \"$name\" \"\$$envVar\"; then")
                    appendLine("    echo \"[!] FATAL ERROR: Failed to decompile $name! Aborting to prevent incomplete patching.\"")
                    appendLine("    cleanup_workspaces")
                    appendLine("    exit 1")
                    appendLine("fi")
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
                    appendLine("if [ -z \"\$$envVar\" ] || [ ! -d \"\$$envVar\" ]; then")
                    appendLine("    echo \"[!] FATAL ERROR: Workspace directory for $name is missing or invalid!\"")
                    appendLine("    cleanup_workspaces")
                    appendLine("    exit 1")
                    appendLine("fi")
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
            appendLine("FEATURE_FAILURES=0")
            features.forEachIndexed { index, feature ->
                appendLine("echo '[*] [${index + 1}/${features.size}] Feature: ${feature.name}'")
                appendLine("f_status=0")
                appendLine(". \"${feature.runtimePath}\" || f_status=\$?")
                appendLine("if [ \$f_status -ne 0 ]; then")
                appendLine("    echo \"[!] ERROR: Feature script '${feature.name}' returned non-zero exit status \$f_status!\"")
                appendLine("    ((FEATURE_FAILURES++))")
                appendLine("fi")
            }
            appendLine("")
            appendLine("if [ \$FEATURE_FAILURES -gt 0 ]; then")
            appendLine("    echo \"[!] FATAL ERROR: \$FEATURE_FAILURES feature script(s) failed during execution! Aborting to prevent corrupt/partial patch.\"")
            appendLine("    cleanup_workspaces")
            appendLine("    exit 1")
            appendLine("fi")
            appendLine("")

            // Phase 3: Recompile all modified workspaces
            appendLine("# Phase 3: Recompile all modified JARs")
            appendLine("echo '[*] Recompiling modified JARs...'")
            appendLine("if ! recompile_all; then")
            appendLine("    echo '[!] FATAL ERROR: Recompilation failed!'")
            appendLine("    cleanup_workspaces")
            appendLine("    exit 1")
            appendLine("fi")
            appendLine("")

            // Verify modifications and copy to output
            appendLine("# Verify modifications and copy to output")
            appendLine("echo '[*] Verifying patches...'")
            appendLine("MODIFIED_COUNT=0")
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
                    appendLine("    ((MODIFIED_COUNT++))")
                    appendLine("else")
                    appendLine("    echo '[INFO] $name was not modified by selected patches.'")
                    appendLine("fi")
                }
            }
            appendLine("if [ \$MODIFIED_COUNT -eq 0 ]; then")
            appendLine("    echo '[!] ERROR: No JAR files were modified by any feature patch!'")
            appendLine("    cleanup_workspaces")
            appendLine("    exit 1")
            appendLine("fi")
            
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