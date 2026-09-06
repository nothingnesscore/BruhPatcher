package com.nothingness.bruhpatcher.core

import android.content.Context
import com.topjohnwu.superuser.Shell
import java.io.File

/**
 * Installs DynamicInstaller environment for on-device framework patching.
 * 
 * DI's `setup` script is designed for interactive use and starts bash,
 * which hangs in automated contexts. Instead, we manually:
 * 1. Copy META-INF to /data/local/di
 * 2. Extract BusyBox for the current architecture
 * 3. Extract static binaries (apktool, smali, etc.)
 * 4. Create an environment file that sets up PATH and sources core
 */
object DiInstaller {

    private const val DI_ROOT = "/data/local/di"
    private const val DI_ZBIN = "$DI_ROOT/META-INF/zbin"
    private const val DI_TMP = "/data/tmp/di"
    private const val DI_BIN = "$DI_TMP/bin"

    suspend fun installIfNeeded(context: Context, log: (String) -> Unit): Boolean {
        // Already installed?
        if (Shell.cmd("test -f $DI_ROOT/environment").exec().isSuccess) {
            log("DynamicInstaller already installed.")
            return true
        }

        log("Installing DynamicInstaller...")

        // Clean slate
        Shell.cmd("rm -rf $DI_ROOT $DI_TMP").exec()
        Shell.cmd("mkdir -p $DI_ROOT $DI_TMP $DI_BIN").exec()

        // Copy META-INF from assets
        copyFolderFromAssets(context, "di/META-INF", "$DI_ROOT/META-INF")
        
        // Copy smali_workspace.sh (it's at di/ root, not inside META-INF)
        copyFileFromAssets(context, "di/smali_workspace.sh", "$DI_ROOT/smali_workspace.sh")
        log("Copied DI assets.")

        // Make everything executable
        Shell.cmd("chmod -R 755 $DI_ROOT").exec()

        // Extract DI tools directly (bypass setup script)
        // The setup script would do this but starts interactive bash
        val extractCmd = """
            cd $DI_ZBIN || exit 1
                
                echo "[DIAG] Contents of zbin:"
                ls -la
                
                # Detect architecture
                ABI=$(getprop ro.product.cpu.abi)
                case "${'$'}ABI" in
                    arm64*) ARCH="arm64-v8a" ;;
                    armeabi*|arm*) ARCH="armeabi-v7a" ;;
                    x86_64*) ARCH="x86_64" ;;
                    x86*) ARCH="x86" ;;
                    *) ARCH="arm64-v8a" ;;
                esac
                echo "[DIAG] Architecture: ${'$'}ARCH"
                
                # Extract BusyBox for architecture
                if [ -d "arch/${'$'}ARCH" ]; then
                    cp -f "arch/${'$'}ARCH/busybox" "$DI_BIN/busybox"
                    chmod 755 "$DI_BIN/busybox"
                    
                    # Copy zipalign directly (bypass unzip issues)
                    if [ -f "arch/${'$'}ARCH/zipalign" ]; then
                        cp -f "arch/${'$'}ARCH/zipalign" "$DI_BIN/zipalign"
                        chmod 755 "$DI_BIN/zipalign"
                        echo "[DIAG] zipalign copied directly"
                    fi
                    
                    # Install busybox applets
                    cd "$DI_BIN"
                    ./busybox --install -s . 2>/dev/null || {
                        for cmd in ${'$'}(./busybox --list 2>/dev/null); do
                            ln -sf busybox "${'$'}cmd" 2>/dev/null || true
                        done
                    }
                    cd $DI_ZBIN
                    echo "[DIAG] BusyBox installed"
                else
                    echo "[DIAG] No arch dir for ${'$'}ARCH"
                fi
                
                # Extract static tools (apktool.jar, zipsigner.jar, etc)
                if [ -f "static" ]; then
                    echo "[DIAG] Extracting static..."
                    # Try busybox unzip first, then system unzip
                    if [ -x "$DI_BIN/unzip" ]; then
                        "$DI_BIN/unzip" -qo "static" -d "$DI_BIN" 2>/dev/null && echo "[DIAG] static extracted (busybox)" || echo "[DIAG] busybox unzip failed"
                    else
                        unzip -qo "static" -d "$DI_BIN" 2>/dev/null && echo "[DIAG] static extracted (system)" || echo "[DIAG] system unzip failed"
                    fi
                fi
                
                # Extract arch-specific binaries (bash, aapt, zip, zipalign)
                if [ -f "arch/${'$'}ARCH/bin" ]; then
                    echo "[DIAG] Extracting arch bin..."
                    if [ -x "$DI_BIN/unzip" ]; then
                        "$DI_BIN/unzip" -qo "arch/${'$'}ARCH/bin" -d "$DI_BIN" 2>/dev/null && echo "[DIAG] arch bin extracted (busybox)" || echo "[DIAG] busybox unzip failed for arch bin"
                    else
                        unzip -qo "arch/${'$'}ARCH/bin" -d "$DI_BIN" 2>/dev/null && echo "[DIAG] arch bin extracted (system)" || echo "[DIAG] system unzip failed for arch bin"
                    fi
                fi
                
                chmod -R 755 "$DI_BIN"
                
                # Copy core and configs
                cp -f core "$DI_TMP/" 2>/dev/null || true
                cp -f configs/* "$DI_BIN/" 2>/dev/null || true
                
                # Copy smali_workspace.sh library (for centralized JAR decompilation)
                # Note: smali_workspace.sh is at $DI_ROOT/../smali_workspace.sh (copied by copyFolderFromAssets to /data/local/di/)
                if [ -f "/data/local/di/smali_workspace.sh" ]; then
                    cp -f "/data/local/di/smali_workspace.sh" "$DI_TMP/"
                    chmod 755 "$DI_TMP/smali_workspace.sh"
                    echo "[DIAG] smali_workspace.sh copied from di root"
                else
                    echo "[DIAG] WARNING: smali_workspace.sh not found at /data/local/di/"
                fi
                
                # Copy baksmali.jar if present (for DEX decompilation)
                if [ -f "baksmali.jar" ]; then
                    cp -f "baksmali.jar" "$DI_BIN/"
                    echo "[DIAG] baksmali.jar copied to bin"
                fi
                
                # Verify critical files
                echo "[DIAG] DI_BIN contents:"
                ls "$DI_BIN" 2>/dev/null | head -20
                
                if [ -x "$DI_BIN/bash" ]; then
                    echo "[DIAG] bash FOUND and executable"
                    "$DI_BIN/bash" --version 2>/dev/null | head -1
                else
                    echo "[DIAG] bash NOT FOUND - extraction may have failed"
                fi
                
                if [ -f "$DI_BIN/apktool.jar" ]; then
                    echo "[DIAG] apktool.jar FOUND"
                else
                    echo "[DIAG] apktool.jar NOT FOUND"
                fi
                
                if [ -x "$DI_BIN/zipalign" ]; then
                    echo "[DIAG] zipalign FOUND"
                else
                    echo "[DIAG] zipalign NOT FOUND"
                fi
        """.trimIndent()

        val extractResult = Shell.cmd(extractCmd).exec()
        extractResult.out.forEach { log("[EXTRACT] $it") }

        // Create environment file using printf (heredoc doesn't work well in this context)
        val createEnvCmd = """
            printf "#!/system/bin/sh\n" > $DI_ROOT/environment
            printf "# DynamicInstaller Environment\n" >> $DI_ROOT/environment
            printf "export DI_ROOT=\"$DI_ZBIN\"\n" >> $DI_ROOT/environment
            printf "export DI_TMP=\"$DI_TMP\"\n" >> $DI_ROOT/environment
            printf "export DI_BIN=\"$DI_BIN\"\n" >> $DI_ROOT/environment
            printf "export PATH=\"$DI_BIN:\${'$'}PATH\"\n" >> $DI_ROOT/environment
            printf "export TMPDIR=\"$DI_TMP\"\n" >> $DI_ROOT/environment
            chmod 755 $DI_ROOT/environment
        """.trimIndent()

        Shell.cmd(createEnvCmd).exec()

        // Verify
        if (!Shell.cmd("test -f $DI_ROOT/environment").exec().isSuccess) {
            log("Failed to create environment file")
            return false
        }

        // Test that busybox is working
        val testResult = Shell.cmd("$DI_BIN/busybox --help >/dev/null 2>&1 && echo OK").exec()
        if (testResult.out.any { it.contains("OK") }) {
            log("BusyBox verified")
        } else {
            log("Warning: BusyBox may not be working")
        }

        log("DynamicInstaller installed successfully")
        return true
    }

    private fun copyFolderFromAssets(context: Context, src: String, dst: String) {
        val files = context.assets.list(src) ?: return
        Shell.cmd("mkdir -p $dst").exec()

        for (name in files) {
            val fullSrc = "$src/$name"
            val fullDst = "$dst/$name"

            val children = context.assets.list(fullSrc)
            if (!children.isNullOrEmpty()) {
                copyFolderFromAssets(context, fullSrc, fullDst)
            } else {
                val tmp = File.createTempFile("di_", name, context.cacheDir)
                context.assets.open(fullSrc).use { input ->
                    tmp.outputStream().use { output -> input.copyTo(output) }
                }
                Shell.cmd("cp ${tmp.absolutePath} $fullDst").exec()
                Shell.cmd("chmod 755 $fullDst").exec()
                tmp.delete()
            }
        }
    }

    private fun copyFileFromAssets(context: Context, src: String, dst: String) {
        val name = src.substringAfterLast("/")
        val tmp = File.createTempFile("di_", name, context.cacheDir)
        context.assets.open(src).use { input ->
            tmp.outputStream().use { output -> input.copyTo(output) }
        }
        Shell.cmd("cp ${tmp.absolutePath} $dst").exec()
        Shell.cmd("chmod 755 $dst").exec()
        tmp.delete()
    }
}
