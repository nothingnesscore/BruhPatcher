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
    fun testCorePatchScriptIntegrity() {
        val corepatchScript = File(featuresDir, "disable_signature_verification.sh")
        assertTrue("disable_signature_verification.sh should exist", corepatchScript.exists())
        val content = corepatchScript.readText()

        assertTrue(content.contains("collectCertificates"))
        assertTrue(content.contains("checkCapability"))
        assertTrue(content.contains("ApkSignatureSchemeV2Verifier"))
        assertTrue(content.contains("ApkSignatureSchemeV3Verifier"))
        assertTrue(content.contains("ApkSignatureSchemeV4Verifier"))
        
        // Critical: verifySignatures MUST return true on bypass (never return_false!)
        assertTrue(content.contains("smali_kit -c -m \"verifySignatures\" -re \"\$return_true\""))
        assertFalse("verifySignatures should never return_false", content.contains("smali_kit -c -m \"verifySignatures\" -re \"\$return_false\""))

        // Critical: checkDowngrade must be patched in InstallPackageHelper for Android 13-17
        assertTrue(content.contains("smali_kit -c -m \"checkDowngrade\" -re \"\$return_void\" -d \"\$SVC_WORK_DIR\" -name \"InstallPackageHelper.smali\""))

        // canBeUpdate returns boolean, must use return_true
        assertTrue(content.contains("smali_kit -c -m \"canBeUpdate\" -re \"\$return_true\""))
        assertFalse("canBeUpdate should not return_void due to Dalvik VerifyError", content.contains("smali_kit -c -m \"canBeUpdate\" -re \"\$return_void\""))

        // Dynamic register capture for digest comparison
        assertTrue(content.contains("match(clean_line, /move-result (v[0-9]+)/, m)"))
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
    fun testCnNotificationFixIntegrity() {
        val cnFixScript = File(featuresDir, "cn_notification_fix.sh")
        assertTrue("cn_notification_fix.sh should exist", cnFixScript.exists())
        val content = cnFixScript.readText()

        // Critical: sed -i blind replacement must NOT be present
        assertFalse("Blind sed replacement of all return instructions corrupts classes", 
            content.contains("sed -i 's/return v[0-9]/const\\/4 v0, 0x1\\n    return v0/g'"))
        
        // Targeted method patching
        assertTrue(content.contains("isAllowStart"))
        assertTrue(content.contains("isServiceRunning"))
    }

    @Test
    fun testAndroid17BuildUnfinalizeScriptIntegrity() {
        val unfinalizeScript = File(featuresDir, "android17_build_unfinalize.sh")
        assertTrue("android17_build_unfinalize.sh should exist", unfinalizeScript.exists())
        val content = unfinalizeScript.readText()

        assertTrue(content.contains("Build.smali"))
        assertTrue(content.contains("Build\\\$VERSION.smali"))
        assertTrue(content.contains("sub(\" static final \", \" static \")"))

        // Must not append "= null" which corrupts primitive fields like TIME:J or DEVICE_INITIAL_SDK_INT:I
        assertFalse("Should not append = null to primitives", content.contains("\$0 = \$0 \" = null\""))
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
}
