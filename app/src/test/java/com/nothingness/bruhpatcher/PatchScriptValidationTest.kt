package com.nothingness.bruhpatcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PatchScriptValidationTest {

    private val featuresDir = File("src/main/assets/features")

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

        // Verify that dangerous/broken patterns are NOT present
        assertFalse("Should not hardcode .locals 15 overwriting registers", content.contains(".locals 15"))
        assertFalse("Should not have broken AppsFilterImpl hook that passes non-String", content.contains("KaoriosHook;->shouldHideAppList(Landroid/content/Context;Ljava/lang/String;)Z\n            move-result v0\n            if-eqz v0, :cond_kaorios_filter_skip\n            const/4 v0, 0x1\n            return v0"))

        // Verify multi-dex direct staging exists
        assertTrue(content.contains("classes\${next_dex}.dex"))
        assertTrue(content.contains("Keybox.xml"))
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
        assertTrue(content.contains("checkDowngrade"))
        assertTrue(content.contains("compareSignatures"))
    }

    @Test
    fun testAndroid17BuildUnfinalizeScriptIntegrity() {
        val unfinalizeScript = File(featuresDir, "android17_build_unfinalize.sh")
        assertTrue("android17_build_unfinalize.sh should exist", unfinalizeScript.exists())
        val content = unfinalizeScript.readText()

        assertTrue(content.contains("Build.smali"))
        assertTrue(content.contains("Build\\\$VERSION.smali"))
        assertTrue(content.contains("sub(\" static final \", \" static \")"))
    }
}
