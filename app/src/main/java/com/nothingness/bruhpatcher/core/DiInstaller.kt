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
        val isInstalled = Shell.cmd("test -f $DI_ROOT/environment").exec().isSuccess
        if (isInstalled) {
            log("DynamicInstaller already installed.")
        } else {
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

            // Create environment file using printf
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
        }

        // Always guarantee that all critical binaries (zip, zipalign, bash, busybox, aapt)
        // are deployed and refreshed into DI_BIN directly from app assets
        deployRuntimeBinaries(context, log)
        extractStaticIfNeeded(context)

        // Verify critical files
        log("[DIAG] Verifying DI_BIN tools:")
        val verifyCmd = """
            [ -x "$DI_BIN/bash" ] && echo "[DIAG] bash FOUND" || echo "[DIAG] bash MISSING"
            [ -x "$DI_BIN/zip" ] && echo "[DIAG] zip FOUND" || echo "[DIAG] zip MISSING"
            [ -x "$DI_BIN/zipalign" ] && echo "[DIAG] zipalign FOUND" || echo "[DIAG] zipalign MISSING"
            [ -f "$DI_BIN/apktool.jar" ] && echo "[DIAG] apktool.jar FOUND" || echo "[DIAG] apktool.jar MISSING"
        """.trimIndent()
        val verifyResult = Shell.cmd(verifyCmd).exec()
        verifyResult.out.forEach { log(it) }

        log("DynamicInstaller ready")
        return true
    }

    private fun deployRuntimeBinaries(context: Context, log: (String) -> Unit) {
        val abi = Shell.cmd("getprop ro.product.cpu.abi").exec().out.firstOrNull() ?: "arm64-v8a"
        val arch = when {
            abi.startsWith("arm64") -> "arm64-v8a"
            abi.startsWith("armeabi") || abi.startsWith("arm") -> "armeabi-v7a"
            abi.startsWith("x86_64") -> "x86_64"
            abi.startsWith("x86") -> "x86"
            else -> "arm64-v8a"
        }

        Shell.cmd("mkdir -p $DI_ROOT $DI_TMP $DI_BIN $DI_ZBIN/arch/$arch").exec()

        // 1. Always refresh smali_workspace.sh to DI_ROOT and DI_TMP
        copyFileFromAssets(context, "di/smali_workspace.sh", "$DI_ROOT/smali_workspace.sh")
        Shell.cmd("cp -f $DI_ROOT/smali_workspace.sh $DI_TMP/smali_workspace.sh 2>/dev/null; chmod 755 $DI_ROOT/smali_workspace.sh $DI_TMP/smali_workspace.sh 2>/dev/null").exec()

        // 2. Directly copy standalone arch binaries (zip, zipalign, bash, busybox, aapt) from assets to DI_BIN
        listOf("zip", "zipalign", "bash", "busybox", "aapt").forEach { binName ->
            val assetPath = "di/META-INF/zbin/arch/$arch/$binName"
            copyFileFromAssets(context, assetPath, "$DI_BIN/$binName")
            copyFileFromAssets(context, assetPath, "$DI_ZBIN/arch/$arch/$binName")
            Shell.cmd("chmod 755 $DI_BIN/$binName $DI_ZBIN/arch/$arch/$binName 2>/dev/null").exec()
        }

        // 3. Ensure core, configs, and baksmali.jar
        copyFileFromAssets(context, "di/META-INF/zbin/core", "$DI_TMP/core")
        copyFileFromAssets(context, "di/META-INF/zbin/baksmali.jar", "$DI_BIN/baksmali.jar")
        copyFolderFromAssets(context, "di/META-INF/zbin/configs", "$DI_BIN")

        // 4. Ensure BusyBox applets are linked
        Shell.cmd("""
            if [ -x "$DI_BIN/busybox" ]; then
                cd "$DI_BIN"
                ./busybox --install -s . 2>/dev/null || {
                    for cmd in ${'$'}(./busybox --list 2>/dev/null); do
                        ln -sf busybox "${'$'}cmd" 2>/dev/null || true
                    done
                }
            fi
            chmod -R 755 "$DI_BIN"
        """.trimIndent()).exec()
    }

    private fun extractStaticIfNeeded(context: Context) {
        val check = Shell.cmd("test -f $DI_BIN/apktool.jar").exec().isSuccess
        if (check) return

        try {
            context.assets.open("di/META-INF/zbin/static").use { input ->
                java.util.zip.ZipInputStream(input).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val fileName = File(entry.name).name
                            val tmp = File.createTempFile("stat_", fileName, context.cacheDir)
                            tmp.outputStream().use { os -> zis.copyTo(os) }
                            Shell.cmd("cp ${tmp.absolutePath} $DI_BIN/$fileName && chmod 755 $DI_BIN/$fileName").exec()
                            tmp.delete()
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (_: Exception) {}
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
