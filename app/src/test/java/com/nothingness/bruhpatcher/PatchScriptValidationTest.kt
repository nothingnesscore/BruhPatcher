package com.nothingness.bruhpatcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    }

    @Test
    fun testInstallerSourceSpoofScriptIntegrity() {
        val spoofScript = File(featuresDir, "installer_source_spoof.sh")
        assertTrue("installer_source_spoof.sh should exist", spoofScript.exists())
        val content = spoofScript.readText()

        assertTrue(content.contains("ComputerEngine"))
        assertTrue(content.contains("getInstallerPackageName"))
        assertTrue(content.contains("KaoriosHook;->filterInstallerPackageName"))
    }

    @Test
    fun testSettingsFilteringScriptIntegrity() {
        val settingsScript = File(featuresDir, "settings_filtering.sh")
        assertTrue("settings_filtering.sh should exist", settingsScript.exists())
        val content = settingsScript.readText()

        assertTrue(content.contains("NameValueCache.smali"))
        assertTrue(content.contains("getStringForUser"))
        assertTrue(content.contains("KaoriosHook;->filterSettingValue"))
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
}
