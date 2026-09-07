package com.nothingness.bruhpatcher

import com.nothingness.bruhpatcher.core.FeatureManager
import com.nothingness.bruhpatcher.model.Feature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureManagerTest {

    @Test
    fun testAllFeaturesCountAndDefaults() {
        val features = Feature.getAllFeatures()
        assertEquals(6, features.size)

        // Core patches default to enabled
        val sigBypass = features.first { it.id == "disable_signature_verification" }
        assertTrue(sigBypass.isEnabled)

        val kaorios = features.first { it.id == "kaorios_toolbox" }
        assertTrue(kaorios.isEnabled)

        val secureFlag = features.first { it.id == "disable_secure_flag" }
        assertTrue(secureFlag.isEnabled)
    }

    @Test
    fun testMiuiFeaturesFiltering() {
        // Without MIUI services, CN_NOTIFICATION_fix should be disabled
        val nonMiuiFeatures = FeatureManager.getAvailableFeatures(hasMiuiServices = false)
        val cnFixNonMiui = nonMiuiFeatures.first { it.id == "cn_notification_fix" }
        assertFalse(cnFixNonMiui.isEnabled)

        // With MIUI services, it retains its initial state
        val miuiFeatures = FeatureManager.getAvailableFeatures(hasMiuiServices = true)
        val cnFixMiui = miuiFeatures.first { it.id == "cn_notification_fix" }
        assertEquals(Feature.CN_NOTIFICATION_FIX.isEnabled, cnFixMiui.isEnabled)
    }

    @Test
    fun testUpdateFeature() {
        val initial = Feature.getAllFeatures()
        val updated = FeatureManager.updateFeature(initial, "google_photos_unlimited", true)
        val photosFeature = updated.first { it.id == "google_photos_unlimited" }
        assertTrue(photosFeature.isEnabled)

        val disabledAgain = FeatureManager.updateFeature(updated, "google_photos_unlimited", false)
        assertFalse(disabledAgain.first { it.id == "google_photos_unlimited" }.isEnabled)
    }

    @Test
    fun testEnabledFeaturesSummary() {
        val allDisabled = Feature.getAllFeatures().map { it.copy(isEnabled = false) }
        assertEquals("None selected", FeatureManager.getEnabledFeaturesSummary(allDisabled))

        val oneEnabled = FeatureManager.updateFeature(allDisabled, "kaorios_toolbox", true)
        assertEquals("Kaorios Toolbox v2.0.6.0", FeatureManager.getEnabledFeaturesSummary(oneEnabled))

        val twoEnabled = FeatureManager.updateFeature(oneEnabled, "disable_signature_verification", true)
        assertEquals("2 features selected", FeatureManager.getEnabledFeaturesSummary(twoEnabled))
    }

    @Test
    fun testBuildFeatureString() {
        val features = listOf(
            Feature.DISABLE_SIGNATURE_VERIFICATION.copy(isEnabled = true),
            Feature.KAORIOS_TOOLBOX.copy(isEnabled = true),
            Feature.GOOGLE_PHOTOS_UNLIMITED.copy(isEnabled = false)
        )
        val featureIds = FeatureManager.buildFeatureString(features)
        assertEquals("disable_signature_verification,kaorios_toolbox", featureIds)
    }

    @Test
    fun testAutoPatcherPresetAllFeaturesEnabled() {
        val allFeatures = Feature.getAllFeatures().map { it.copy(isEnabled = true) }
        val featureIds = FeatureManager.buildFeatureString(allFeatures)

        assertTrue(featureIds.contains("disable_signature_verification"))
        assertTrue(featureIds.contains("disable_secure_flag"))
        assertTrue(featureIds.contains("kaorios_toolbox"))
        assertTrue(featureIds.contains("android17_build_unfinalize"))
        assertTrue(featureIds.contains("cn_notification_fix"))
        assertTrue(featureIds.contains("google_photos_unlimited"))
        assertEquals("6 features selected", FeatureManager.getEnabledFeaturesSummary(allFeatures))
    }
}
