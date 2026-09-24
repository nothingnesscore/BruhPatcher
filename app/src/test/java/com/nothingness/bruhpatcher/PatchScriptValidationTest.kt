package com.nothingness.bruhpatcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Test
import java.io.File

class PatchScriptValidationTest {

    private val featuresDir = File("src/main/assets/features")
    private val moduleTemplateDir = File("src/main/assets/module_template")

    @Test
    fun testKaoriosCoreScriptIntegrity() {
        val coreScript = File(featuresDir, "kaorios_core.sh")
        assertTrue("kaorios_core.sh should exist", coreScript.exists())
        val content = coreScript.readText()

        // Verify fundamental KaoriOS v3 hooks
        assertTrue(content.contains("KaoriosHook;->initContext"))
        assertTrue(content.contains("KaoriosHook;->hasSystemFeature"))
        assertTrue(content.contains("KaoriosHook;->initGenerateSoftwareKeyPair"))
        assertTrue(content.contains("KaoriosHook;->CertificateChainIfNeeded"))
        assertTrue(content.contains("KaoriosHook;->initActivityThread"))
        assertTrue(content.contains("KaoriosHook;->initSystemServer"))

        // Verify register safety (registers + 1, target reg at new_regs - 2)
        assertTrue(content.contains("new_regs = orig_regs + 1"))
        assertTrue(content.contains("target_reg = \"v\" (new_regs - 2)"))
        assertFalse("Should not hardcode .locals 15 overwriting registers", content.contains(".locals 15"))

        // Verify post-assignment ActivityThread and pre-loop SystemServer hooks
        assertTrue(content.contains("mBoundApplication:"))
        assertTrue(content.contains("initActivityThread(Ljava/lang/Object;)V"))
        assertTrue(content.contains("Looper;->loop"))
        assertTrue(content.contains("initSystemServer()V"))
        assertFalse(content.contains("/data/local/tmp/frameworkforge/kaorios"))

        // Verify multi-dex payload staging and companion app deployment
        assertTrue(content.contains("classes\${next_dex}.dex"))
        assertTrue(content.contains("Keybox.xml"))
        assertTrue(content.contains("KaoriosToolbox.apk"))
    }

    @Test
    fun testCorePatchDsvScriptIntegrity() {
        val dsvScript = File(featuresDir, "core_patch_dsv.sh")
        assertTrue("core_patch_dsv.sh should exist", dsvScript.exists())
        val content = dsvScript.readText()

        // Verify signature verification and downgrade bypasses across framework, services, and miui-services
        assertTrue(content.contains("PackageParser.smali"))
        assertTrue(content.contains("unsafeGetCertsWithoutVerification"))
        assertTrue(content.contains("SigningDetails.smali"))
        assertTrue(content.contains("ApkSignatureSchemeV2Verifier.smali"))
        assertTrue(content.contains("ApkSignatureSchemeV3Verifier.smali"))
        assertTrue(content.contains("ApkSigningBlockUtils.smali"))
        assertTrue(content.contains("InstallPackageHelper.smali"))
        assertTrue(content.contains("checkDowngrade"))
        assertTrue(content.contains("verifySignatures"))
    }

    @Test
    fun testDisableFlagSecureScriptIntegrity() {
        val flagSecureScript = File(featuresDir, "disable_flag_secure.sh")
        assertTrue("disable_flag_secure.sh should exist", flagSecureScript.exists())
        val content = flagSecureScript.readText()

        assertTrue(content.contains("DevicePolicyCacheImpl.smali"))
        assertTrue(content.contains("isScreenCaptureAllowed"))
        assertTrue(content.contains("WindowState.smali"))
        assertTrue(content.contains("isSecureLocked"))
        assertTrue(content.contains("setSecureLocked"))
        assertTrue(content.contains("WindowStateAnimator.smali"))
        assertTrue(content.contains("notAllowCaptureDisplay"))
        assertTrue(content.contains("KaoriosHook;->isSecureFlag"))
    }

    @Test
    fun testHideDeveloperAdbScriptIntegrity() {
        val devScript = File(featuresDir, "hide_developer_adb.sh")
        assertTrue("hide_developer_adb.sh should exist", devScript.exists())
        val content = devScript.readText()

        assertTrue(content.contains("NameValueCache.smali"))
        assertTrue(content.contains("getStringForUser"))
        assertTrue(content.contains("KaoriosHook;->shouldHideDevStatusFromNameValueCache"))
    }

    @Test
    fun testHideInstalledAppsScriptIntegrity() {
        val hideAppsScript = File(featuresDir, "hide_installed_apps.sh")
        assertTrue("hide_installed_apps.sh should exist", hideAppsScript.exists())
        val content = hideAppsScript.readText()

        assertTrue(content.contains("AppsFilterBase.smali"))
        assertTrue(content.contains("shouldFilterApplication"))
        assertTrue(content.contains("KaoriosHook;->shouldHideAppList"))
        assertTrue(content.contains("ComputerEngine"))
        assertTrue(content.contains("KaoriosHook;->shouldHideAppListForCaller"))
        assertTrue(content.contains(":cond_kaorios_hide_stock"))
    }

    @Test
    fun testInstallerSourceSpoofScriptIntegrity() {
        val spoofScript = File(featuresDir, "installer_source_spoof.sh")
        assertTrue("installer_source_spoof.sh should exist", spoofScript.exists())
        val content = spoofScript.readText()

        assertTrue(content.contains("ComputerEngine"))
        assertTrue(content.contains("getInstallerPackageName"))
        assertTrue(content.contains("KaoriosHook;->filterInstallerPackageName"))
        assertFalse(content.contains("vNull"))
        assertFalse(content.contains("vCallingUid"))
        assertTrue(content.contains("new_regs = orig_regs + 2"))
    }

    @Test
    fun testSettingsFilteringScriptIntegrity() {
        val settingsScript = File(featuresDir, "settings_filtering.sh")
        assertTrue("settings_filtering.sh should exist", settingsScript.exists())
        val content = settingsScript.readText()

        assertTrue(content.contains("NameValueCache.smali"))
        assertTrue(content.contains("getStringForUser"))
        assertTrue(content.contains("KaoriosHook;->filterSettingValue"))
        assertTrue(content.contains("SettingsProvider.smali"))
        assertTrue(content.contains("KaoriosHook;->filterSettingsCall"))
        assertTrue(content.contains(":cond_kaorios_settings_stock"))
        assertTrue(content.contains("#@requires framework.jar,services.jar"))
        assertFalse(content.contains("vNull"))
        assertFalse(content.contains("vNamespace"))
    }

    @Test
    fun testAndroid17BuildUnfinalizeScriptIntegrity() {
        val unfinalizeScript = File(featuresDir, "android17_build_unfinalize.sh")
        assertTrue("android17_build_unfinalize.sh should exist", unfinalizeScript.exists())
        val content = unfinalizeScript.readText()

        assertTrue(content.contains("Build.smali"))
        assertTrue(content.contains("Build\\\$VERSION.smali"))
        assertTrue(content.contains("BRAND_FOR_ATTESTATION"))
        assertTrue(content.contains("TIME:J"))
        assertTrue(content.contains("sub(\" final \", \" \")"))
        assertTrue(content.contains(" = null"))
    }

    @Test
    fun testModuleTemplateScriptsIntegrity() {
        val serviceScript = File(moduleTemplateDir, "service.sh")
        assertTrue("service.sh should exist", serviceScript.exists())
        val serviceContent = serviceScript.readText()
        assertTrue("service.sh should support SKIP_BOOT_WAIT", serviceContent.contains("SKIP_BOOT_WAIT"))
        assertTrue("service.sh should conditionally install companion app", serviceContent.contains("! pm path com.kousei.kaorios"))

        val customizeScript = File(moduleTemplateDir, "customize.sh")
        assertTrue("customize.sh should exist", customizeScript.exists())
        val customizeContent = customizeScript.readText()
        assertTrue("customize.sh should pass SKIP_BOOT_WAIT=1 to service.sh", customizeContent.contains("SKIP_BOOT_WAIT=1"))

        val actionScript = File(moduleTemplateDir, "action.sh")
        assertTrue("action.sh should exist", actionScript.exists())
        val actionContent = actionScript.readText()
        assertTrue("action.sh should pass SKIP_BOOT_WAIT=1 to service.sh", actionContent.contains("SKIP_BOOT_WAIT=1"))
    }

    @Test
    fun testAllAssetsHaveStrictLfEndings() {
        val assetsDir = File("src/main/assets")
        assertTrue("assets directory should exist", assetsDir.exists())
        val textExtensions = setOf("sh", "bash", "config", "txt", "json", "xml", "prop")
        val specialNames = setOf("core", "setup", "update-binary", "updater-script", ".gitattributes", ".gitignore")

        val crlfFiles = mutableListOf<String>()
        assetsDir.walkTopDown().forEach { file ->
            if (file.isFile && (file.extension in textExtensions || file.name in specialNames)) {
                val bytes = file.readBytes()
                if (bytes.contains(13.toByte())) {
                    crlfFiles.add(file.path)
                }
            }
        }
        assertTrue("Found CRLF in text assets: ${crlfFiles.joinToString()}", crlfFiles.isEmpty())
    }

    @Test
    fun testNoInvalidSmaliPseudoRegistersAcrossScripts() {
        val scripts = featuresDir.listFiles { _, name -> name.endsWith(".sh") } ?: emptyArray()
        assertTrue("Features directory should contain scripts", scripts.isNotEmpty())
        for (script in scripts) {
            val content = script.readText()
            assertFalse("Script ${script.name} should not contain pseudo-register vNull", content.contains("vNull"))
            assertFalse("Script ${script.name} should not contain pseudo-register vNamespace", content.contains("vNamespace"))
            assertFalse("Script ${script.name} should not contain pseudo-register vCallingUid", content.contains("vCallingUid"))
        }
    }

    @Test
    fun testCanonicalFourStepScriptHeaderMetadata() {
        val expectedScripts = listOf(
            "android17_build_unfinalize.sh",
            "kaorios_core.sh",
            "core_patch_dsv.sh",
            "disable_flag_secure.sh",
            "hide_developer_adb.sh",
            "hide_installed_apps.sh",
            "installer_source_spoof.sh",
            "settings_filtering.sh"
        )
        for (scriptName in expectedScripts) {
            val file = File(featuresDir, scriptName)
            assertTrue("Expected script $scriptName should exist", file.exists())
            val lines = file.readLines()
            assertTrue("$scriptName must have #@name header", lines.any { it.startsWith("#@name") })
            assertTrue("$scriptName must have #@description header", lines.any { it.startsWith("#@description") })
            assertTrue("$scriptName must have #@requires header", lines.any { it.startsWith("#@requires") })
        }
    }

    @Test
    fun testNoUnsafeRegisterAliasingInMultiParamHooks() {
        val spoofScript = File(featuresDir, "installer_source_spoof.sh").readText()
        assertFalse("installer_source_spoof should not alias new_regs - 3", spoofScript.contains("new_regs - 3"))
        assertFalse("installer_source_spoof should not alias new_regs - 2", spoofScript.contains("new_regs - 2"))
        assertTrue("installer_source_spoof must allocate orig_regs - 3", spoofScript.contains("orig_regs - 3"))
        assertTrue("installer_source_spoof must allocate orig_regs - 2", spoofScript.contains("orig_regs - 2"))

        val settingsScript = File(featuresDir, "settings_filtering.sh").readText()
        assertFalse("settings_filtering should not use new_regs - 2", settingsScript.contains("new_regs - 2"))
        assertTrue("settings_filtering must allocate orig_regs - 4", settingsScript.contains("orig_regs - 4"))

        val hideAppsScript = File(featuresDir, "hide_installed_apps.sh").readText()
        assertFalse("hide_installed_apps should not use new_regs - 2", hideAppsScript.contains("new_regs - 2"))
        assertTrue("hide_installed_apps must use param_count offset", hideAppsScript.contains("orig_regs - param_count"))

        val hideDevScript = File(featuresDir, "hide_developer_adb.sh").readText()
        assertTrue("hide_developer_adb must allocate orig_regs - 4", hideDevScript.contains("orig_regs - 4"))
    }

    @Test
    fun testNoGnuAwkBackslashSInScripts() {
        val scripts = featuresDir.listFiles { _, name -> name.endsWith(".sh") } ?: emptyArray()
        for (script in scripts) {
            val content = script.readText()
            assertFalse(
                "Script ${script.name} must not contain \\s regex tokens (breaks on Android toybox/busybox awk)",
                content.contains("\\s")
            )
        }
    }

    @Test
    fun testNoForcedEncryptedCryptoStateInModuleTemplates() {
        val serviceScript = File(moduleTemplateDir, "service.sh").readText()
        assertFalse("service.sh must not force ro.crypto.state=encrypted", serviceScript.contains("ro.crypto.state=encrypted"))

        val systemProp = File(moduleTemplateDir, "system.prop").readText()
        assertFalse("system.prop must not force ro.crypto.state=encrypted", systemProp.contains("ro.crypto.state=encrypted"))
    }

    @Test
    fun testNoBrokenPositionalArgInMvCommands() {
        val scripts = featuresDir.listFiles { _, name -> name.endsWith(".sh") } ?: emptyArray()
        assertTrue("Features directory should contain scripts", scripts.isNotEmpty())
        val mvToPositionalArgRegex = Regex("""mv\s+["']?\$?[a-zA-Z0-9_{}]+(?:\.tmp)?["']?\s+["']?\$1\b""")
        for (script in scripts) {
            val content = script.readText()
            assertFalse(
                "Script ${script.name} must not move temp files to \$1 (empty positional argument when sourced)",
                mvToPositionalArgRegex.containsMatchIn(content)
            )
            val tmpMatches = Regex("""> "(\$\{?([a-zA-Z0-9_]+)\}?)\.tmp"\s*&&\s*mv "(\$\{?([a-zA-Z0-9_]+)\}?)\.tmp"\s+"(\$\{?([a-zA-Z0-9_]+)\}?)"""").findAll(content)
            for (match in tmpMatches) {
                val srcVar = match.groupValues[2]
                val destVar = match.groupValues[6]
                org.junit.Assert.assertEquals(
                    "In ${script.name}, destination variable should match source variable",
                    srcVar,
                    destVar
                )
            }
        }
    }

    // =========================================================================
    // DYNAMIC AWK EXECUTION & DALVIK SEMANTIC VALIDATION INFRASTRUCTURE
    // =========================================================================

    /**
     * Resolves the host awk binary dynamically across Windows (Git toolchain),
     * Linux, and macOS environments.
     */
    private fun findAwk(): String? {
        val os = (System.getProperty("os.name") ?: "").lowercase()
        val isWindows = os.contains("win")
        val awkBinary = if (isWindows) "awk.exe" else "awk"

        // 1. Check system PATH
        val pathDirs = (System.getenv("PATH") ?: "").split(File.pathSeparator)
        for (dir in pathDirs) {
            val candidate = File(dir, awkBinary)
            if (candidate.isFile && candidate.canExecute()) {
                return candidate.absolutePath
            }
        }

        // 2. Windows: dynamically resolve from git.exe root
        if (isWindows) {
            try {
                val proc = ProcessBuilder("where.exe", "git").start()
                val gitPath = proc.inputStream.bufferedReader().readText().trim().lines().firstOrNull()
                if (!gitPath.isNullOrBlank()) {
                    val gitDir = File(gitPath).parentFile?.parentFile
                    if (gitDir != null) {
                        val gitAwk = File(gitDir, "usr\\bin\\awk.exe")
                        if (gitAwk.isFile) return gitAwk.absolutePath
                    }
                }
            } catch (_: Exception) {}

            val commonWinPaths = listOf(
                "C:\\Program Files\\Git\\usr\\bin\\awk.exe",
                "C:\\Program Files (x86)\\Git\\usr\\bin\\awk.exe",
                "C:\\Git\\usr\\bin\\awk.exe",
                System.getenv("LOCALAPPDATA")?.let { "$it\\Programs\\Git\\usr\\bin\\awk.exe" }
            ).filterNotNull()

            for (p in commonWinPaths) {
                val f = File(p)
                if (f.isFile) return f.absolutePath
            }
        }

        // 3. Fallback: test direct command invocation
        return try {
            val p = ProcessBuilder("awk", "--version").start()
            if (p.waitFor() == 0) "awk" else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extracts an exact awk script body from a feature shell script matching [methodOrKeyword].
     */
    private fun extractAwkScript(scriptName: String, methodOrKeyword: String): String {
        val file = File(featuresDir, scriptName)
        assertTrue("Script $scriptName should exist", file.exists())
        val content = file.readText()
        val awkBlockRegex = Regex("""awk\s+'([\s\S]*?)'\s+"?\$""")
        val match = awkBlockRegex.findAll(content).firstOrNull {
            it.groupValues[1].contains(methodOrKeyword)
        } ?: throw IllegalArgumentException("No awk script block found matching '$methodOrKeyword' in $scriptName")
        return match.groupValues[1]
    }

    /**
     * Executes an awk script on [inputSmali] via temporary files to avoid shell escaping anomalies.
     */
    private fun runAwk(awkBody: String, inputSmali: String): String {
        val awkExe = findAwk()
        assumeNotNull("awk executable required for semantic patch validation", awkExe)

        val tempAwk = File.createTempFile("patch_awk_", ".awk")
        val tempSmali = File.createTempFile("smali_in_", ".smali")
        try {
            tempAwk.writeText(awkBody)
            tempSmali.writeText(inputSmali)
            val pb = ProcessBuilder(awkExe!!, "-f", tempAwk.absolutePath, tempSmali.absolutePath)
            val proc = pb.start()
            val stdout = proc.inputStream.bufferedReader().readText()
            val stderr = proc.errorStream.bufferedReader().readText()
            val exitCode = proc.waitFor()
            assertTrue("awk failed with code $exitCode: $stderr", exitCode == 0)
            return stdout
        } finally {
            tempAwk.delete()
            tempSmali.delete()
        }
    }

    /**
     * Asserts Dalvik parameter register safety:
     * 1. No parameter register (p0..p(P-1) or Dalvik aliases v(N-P)..v(N-1)) is assigned.
     * 2. On fallback branch to [fallbackLabel], all parameter registers remain 100% untouched.
     */
    private fun assertRegisterSafety(
        outputSmali: String,
        paramCount: Int,
        fallbackLabel: String? = null
    ) {
        val regMatch = Regex("""^\s*\.registers\s+([0-9]+)""", RegexOption.MULTILINE).find(outputSmali)
        val locMatch = Regex("""^\s*\.locals\s+([0-9]+)""", RegexOption.MULTILINE).find(outputSmali)

        assertTrue(
            "Transformed Smali must contain .registers or .locals declaration:\n$outputSmali",
            regMatch != null || locMatch != null
        )

        val paramNames = (0 until paramCount).map { "p$it" }.toSet()
        val forbiddenRegisters: Set<String>
        val localCount: Int

        if (regMatch != null) {
            val totalRegs = regMatch.groupValues[1].toInt()
            localCount = totalRegs - paramCount
            val dalvikParamRegs = (0 until paramCount).map { "v${totalRegs - paramCount + it}" }.toSet()
            forbiddenRegisters = paramNames + dalvikParamRegs
        } else {
            localCount = locMatch!!.groupValues[1].toInt()
            forbiddenRegisters = paramNames
        }

        // Instructions that assign to destination register as first operand
        val destRegRegex = Regex(
            """^\s*(?:move-result(?:-object|-wide)?|const(?:-string|-class|/[0-9a-z]+)?|new-instance|[is]get(?:-[a-z]+)?|move(?:-object|-wide)?)\s+([vp][0-9]+)"""
        )

        val lines = outputSmali.lines()

        // 1. Assert no parameter register is ever assigned in the hook
        for (line in lines) {
            val match = destRegRegex.find(line) ?: continue
            val destReg = match.groupValues[1]

            assertFalse(
                "Dalvik parameter clobbering detected: register '$destReg' in line '$line' is an alias for a parameter register ($forbiddenRegisters)!\nTransformed Smali:\n$outputSmali",
                forbiddenRegisters.contains(destReg)
            )

            if (locMatch != null && destReg.startsWith("v")) {
                val idx = destReg.substring(1).toInt()
                assertTrue(
                    "Local register overflow: '$destReg' exceeds declared .locals $localCount in line '$line'!\nTransformed Smali:\n$outputSmali",
                    idx < localCount
                )
            }
        }

        // 2. Assert fallback branch to stock label preserves all parameter registers intact
        if (fallbackLabel != null) {
            assertTrue("Smali must define fallback label $fallbackLabel", outputSmali.contains(fallbackLabel))
            val linesBeforeStock = mutableListOf<String>()
            for (line in lines) {
                if (line.trim().startsWith(fallbackLabel)) break
                linesBeforeStock.add(line)
            }

            for (line in linesBeforeStock) {
                val match = destRegRegex.find(line) ?: continue
                val destReg = match.groupValues[1]
                assertFalse(
                    "Fallback branch corrupted: parameter register '$destReg' was modified before jumping to '$fallbackLabel' in line '$line'!\nTransformed Smali:\n$outputSmali",
                    forbiddenRegisters.contains(destReg)
                )
            }
        }
    }

    // =========================================================================
    // SEMANTIC TEST CASES
    // =========================================================================

    @Test
    fun testSettingsProviderCallRegisterSafety_WithRegisters() {
        val awk = extractAwkScript("settings_filtering.sh", "filterSettingsCall")
        val mockSmali = """
            .method public call(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;)Landroid/os/Bundle;
                .registers 8
                const/4 v0, 0x0
                return-object v0
            .end method
        """.trimIndent()
        val result = runAwk(awk, mockSmali)
        // 4 parameters: p0=this, p1=method, p2=arg, p3=extras
        assertRegisterSafety(result, paramCount = 4, fallbackLabel = ":cond_kaorios_settings_stock")
    }

    @Test
    fun testSettingsProviderCallRegisterSafety_WithLocals() {
        val awk = extractAwkScript("settings_filtering.sh", "filterSettingsCall")
        val mockSmali = """
            .method public call(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;)Landroid/os/Bundle;
                .locals 4
                const/4 v0, 0x0
                return-object v0
            .end method
        """.trimIndent()
        val result = runAwk(awk, mockSmali)
        assertRegisterSafety(result, paramCount = 4, fallbackLabel = ":cond_kaorios_settings_stock")
    }

    @Test
    fun testSettingsNameValueCacheRegisterSafety_WithRegisters() {
        val awk = extractAwkScript("settings_filtering.sh", "filterSettingValue")
        val mockSmali = """
            .method public getStringForUser(Landroid/content/ContentResolver;Ljava/lang/String;I)Ljava/lang/String;
                .registers 8
                const-string v0, "val"
                return-object v0
            .end method
        """.trimIndent()
        val result = runAwk(awk, mockSmali)
        // 4 parameters: p0=this, p1=cr, p2=name, p3=userHandle
        assertRegisterSafety(result, paramCount = 4)
    }

    @Test
    fun testSettingsNameValueCacheRegisterSafety_WithLocals() {
        val awk = extractAwkScript("settings_filtering.sh", "filterSettingValue")
        val mockSmali = """
            .method public getStringForUser(Landroid/content/ContentResolver;Ljava/lang/String;I)Ljava/lang/String;
                .locals 4
                const-string v0, "val"
                return-object v0
            .end method
        """.trimIndent()
        val result = runAwk(awk, mockSmali)
        assertRegisterSafety(result, paramCount = 4)
    }

    @Test
    fun testHideInstalledAppsComputerEngineRegisterSafety_4Params_WithRegisters() {
        val awk = extractAwkScript("hide_installed_apps.sh", "shouldHideAppListForCaller")
        val mockSmali = """
            .method public shouldFilterApplication(ILcom/android/server/pm/pkg/PackageStateInternal;I)Z
                .registers 8
                const/4 v0, 0x0
                return v0
            .end method
        """.trimIndent()
        val result = runAwk(awk, mockSmali)
        // 4 parameters: p0=this, p1=callingUid, p2=packageState, p3=userId
        assertRegisterSafety(result, paramCount = 4, fallbackLabel = ":cond_kaorios_hide_stock")
    }

    @Test
    fun testHideInstalledAppsComputerEngineRegisterSafety_5Params_WithRegisters() {
        val awk = extractAwkScript("hide_installed_apps.sh", "shouldHideAppListForCaller")
        val mockSmali = """
            .method public shouldFilterApplication(Ljava/lang/Object;ILcom/android/server/pm/pkg/PackageStateInternal;I)Z
                .registers 9
                const/4 v0, 0x0
                return v0
            .end method
        """.trimIndent()
        val result = runAwk(awk, mockSmali)
        // 5 parameters: p0=this, p1=setting, p2=callingUid, p3=packageState, p4=userId
        assertRegisterSafety(result, paramCount = 5, fallbackLabel = ":cond_kaorios_hide_stock")
    }

    @Test
    fun testHideInstalledAppsComputerEngineRegisterSafety_WithLocals() {
        val awk = extractAwkScript("hide_installed_apps.sh", "shouldHideAppListForCaller")
        val mockSmali = """
            .method public shouldFilterApplication(ILcom/android/server/pm/pkg/PackageStateInternal;I)Z
                .locals 4
                const/4 v0, 0x0
                return v0
            .end method
        """.trimIndent()
        val result = runAwk(awk, mockSmali)
        assertRegisterSafety(result, paramCount = 4, fallbackLabel = ":cond_kaorios_hide_stock")
    }

    @Test
    fun testInstallerSourceSpoofRegisterSafety_WithRegisters() {
        val awk = extractAwkScript("installer_source_spoof.sh", "getInstallerPackageName")
        val mockSmali = """
            .method public getInstallerPackageName(Ljava/lang/String;I)Ljava/lang/String;
                .registers 6
                const-string v0, "com.android.vending"
                return-object v0
            .end method
        """.trimIndent()
        val result = runAwk(awk, mockSmali)
        // 3 parameters: p0=this, p1=packageName, p2=userId
        assertRegisterSafety(result, paramCount = 3)
    }

    @Test
    fun testInstallerSourceSpoofRegisterSafety_WithLocals() {
        val awk = extractAwkScript("installer_source_spoof.sh", "getInstallerPackageName")
        val mockSmali = """
            .method public getInstallerPackageName(Ljava/lang/String;I)Ljava/lang/String;
                .locals 3
                const-string v0, "com.android.vending"
                return-object v0
            .end method
        """.trimIndent()
        val result = runAwk(awk, mockSmali)
        assertRegisterSafety(result, paramCount = 3)
    }

    @Test
    fun testKaoriosCoreGenerateKeyPairRegisterSafety() {
        val awk = extractAwkScript("kaorios_core.sh", "generateKeyPair")
        val mockSmali = """
            .method public generateKeyPair()Ljava/security/KeyPair;
                .registers 3
                const/4 v0, 0x0
                return-object v0
            .end method
        """.trimIndent()
        val result = runAwk(awk, mockSmali)
        // 1 parameter: p0=this
        assertRegisterSafety(result, paramCount = 1, fallbackLabel = ":cond_kaorios_gen_stock")
    }

    @Test
    fun testHideDeveloperAdbRegisterSafety_WithRegisters() {
        val awk = extractAwkScript("hide_developer_adb.sh", "shouldHideDevStatusFromNameValueCache")
        val mockSmali = """
            .method public getStringForUser(Landroid/content/ContentResolver;Ljava/lang/String;I)Ljava/lang/String;
                .registers 8
                const-string v0, "0"
                return-object v0
            .end method
        """.trimIndent()
        val result = runAwk(awk, mockSmali)
        // 4 parameters: p0=this, p1=cr, p2=name, p3=userHandle
        assertRegisterSafety(result, paramCount = 4, fallbackLabel = ":cond_kaorios_dev_stock")
    }

    @Test
    fun testHideDeveloperAdbRegisterSafety_WithLocals() {
        val awk = extractAwkScript("hide_developer_adb.sh", "shouldHideDevStatusFromNameValueCache")
        val mockSmali = """
            .method public getStringForUser(Landroid/content/ContentResolver;Ljava/lang/String;I)Ljava/lang/String;
                .locals 4
                const-string v0, "0"
                return-object v0
            .end method
        """.trimIndent()
        val result = runAwk(awk, mockSmali)
        assertRegisterSafety(result, paramCount = 4, fallbackLabel = ":cond_kaorios_dev_stock")
    }
}


// forced-recompile
