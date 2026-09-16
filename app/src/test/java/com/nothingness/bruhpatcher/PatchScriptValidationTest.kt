package com.nothingness.bruhpatcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PatchScriptValidationTest {

    private val featuresDir = File("src/main/assets/features")
    private val moduleTemplateDir = File("src/main/assets/module_template")

    @Test
    fun testKaoriosToolboxScriptIntegrity() {
        val kaoriosScript = File(featuresDir, "kaorios_toolbox.sh")
        assertTrue("kaorios_toolbox.sh should exist", kaoriosScript.exists())
        val content = kaoriosScript.readText()

        // Verify required Kaorios hooks
        assertTrue(content.contains("KaoriosHook;->initContext"))
        assertTrue(content.contains("KaoriosHook;->hasSystemFeature"))
        assertTrue(content.contains("KaoriosHook;->initGenerateSoftwareKeyPair"))
        assertTrue(content.contains("KaoriosHook;->CertificateChainIfNeeded"))
        assertTrue(content.contains("KaoriosHook;->initActivityThread"))
        assertTrue(content.contains("KaoriosHook;->shouldHideDevStatusFromNameValueCache"))
        assertTrue(content.contains("KaoriosHook;->initSystemServer"))

        // Verify dynamic FLAG_SECURE hooks per Toolbox-docs Disable_Secure_Flag.md
        assertTrue(content.contains("DevicePolicyCacheImpl.smali"))
        assertTrue(content.contains("isScreenCaptureAllowed"))
        assertTrue(content.contains("WindowState.smali"))
        assertTrue(content.contains("setSecureLocked"))
        assertTrue(content.contains("WindowStateAnimator.smali"))
        assertTrue(content.contains("notAllowCaptureDisplay"))
        assertTrue(content.contains("KaoriosHook;->isSecureFlag"))

        // Verify register safety (registers + 1, target reg at new_regs - 2)
        assertTrue(content.contains("new_regs = orig_regs + 1"))
        assertTrue(content.contains("target_reg = \"v\" (new_regs - 2)"))
        assertFalse("Should not hardcode .locals 15 overwriting registers", content.contains(".locals 15"))

        // Verify multi-dex direct staging exists
        assertTrue(content.contains("classes\${next_dex}.dex"))
        assertTrue(content.contains("Keybox.xml"))
        assertTrue(content.contains("KaoriosToolbox.apk"))
    }

    @Test
    fun testGooglePhotosUnlimitedScriptIntegrity() {
        val photosScript = File(featuresDir, "google_photos_unlimited.sh")
        assertTrue("google_photos_unlimited.sh should exist", photosScript.exists())
        val content = photosScript.readText()

        assertTrue(content.contains("com.google.android.apps.photos.NEXUS_PRELOAD"))
        assertTrue(content.contains("com.google.android.feature.PIXEL_2016_EXPERIENCE"))
        assertTrue(content.contains("hasSystemFeature"))
        assertTrue(content.contains("persist.sys.pixel.photos=1"))
    }

    @Test
    fun testDisableFlagSecureScriptIntegrity() {
        val flagSecureScript = File(featuresDir, "disable_flag_secure.sh")
        assertTrue("disable_flag_secure.sh should exist", flagSecureScript.exists())
        val content = flagSecureScript.readText()

        assertTrue(content.contains("isSecureLocked"))
        assertTrue(content.contains("setSecureLocked"))
        assertTrue(content.contains("notAllowCaptureDisplay"))
        assertTrue(content.contains("isScreenCaptureAllowed"))
        assertTrue(content.contains("return_void"))
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
