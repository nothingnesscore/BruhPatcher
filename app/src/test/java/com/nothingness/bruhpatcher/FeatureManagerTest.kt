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
        assertEquals(4, features.size)

        // Core patches default to enabled
        val googlePhotos = features.first { it.id == "google_photos_unlimited" }
        assertTrue(googlePhotos.isEnabled)

        val kaorios = features.first { it.id == "kaorios_toolbox" }
        assertTrue(kaorios.isEnabled)

        val secureFlag = features.first { it.id == "disable_secure_flag" }
        assertTrue(secureFlag.isEnabled)

        val a17Unfinalize = features.first { it.id == "android17_build_unfinalize" }
        assertTrue(a17Unfinalize.isEnabled)
    }

    @Test
    fun testUpdateFeature() {
        val initial = Feature.getAllFeatures()
        val updated = FeatureManager.updateFeature(initial, "android17_build_unfinalize", false)
        val a17Feature = updated.first { it.id == "android17_build_unfinalize" }
        assertFalse(a17Feature.isEnabled)

        val enabledAgain = FeatureManager.updateFeature(updated, "android17_build_unfinalize", true)
        assertTrue(enabledAgain.first { it.id == "android17_build_unfinalize" }.isEnabled)
    }

    @Test
    fun testEnabledFeaturesSummary() {
        val allDisabled = Feature.getAllFeatures().map { it.copy(isEnabled = false) }
        assertEquals("None selected", FeatureManager.getEnabledFeaturesSummary(allDisabled))

        val oneEnabled = FeatureManager.updateFeature(allDisabled, "kaorios_toolbox", true)
        assertEquals("Kaorios Toolbox v2.0.6.0", FeatureManager.getEnabledFeaturesSummary(oneEnabled))

        val twoEnabled = FeatureManager.updateFeature(oneEnabled, "google_photos_unlimited", true)
        assertEquals("2 features selected", FeatureManager.getEnabledFeaturesSummary(twoEnabled))
    }

    @Test
    fun testBuildFeatureString() {
        val features = listOf(
            Feature.GOOGLE_PHOTOS_UNLIMITED.copy(isEnabled = true),
            Feature.KAORIOS_TOOLBOX.copy(isEnabled = true),
            Feature.ANDROID17_BUILD_UNFINALIZE.copy(isEnabled = false)
        )
        val featureIds = FeatureManager.buildFeatureString(features)
        assertEquals("google_photos_unlimited,kaorios_toolbox", featureIds)
    }

    @Test
    fun testAutoPatcherPresetAllFeaturesEnabled() {
        val allFeatures = Feature.getAllFeatures().map { it.copy(isEnabled = true) }
        val featureIds = FeatureManager.buildFeatureString(allFeatures)

        assertTrue(featureIds.contains("google_photos_unlimited"))
        assertTrue(featureIds.contains("disable_secure_flag"))
        assertTrue(featureIds.contains("kaorios_toolbox"))
        assertTrue(featureIds.contains("android17_build_unfinalize"))
        assertEquals("4 features selected", FeatureManager.getEnabledFeaturesSummary(allFeatures))
    }
}
