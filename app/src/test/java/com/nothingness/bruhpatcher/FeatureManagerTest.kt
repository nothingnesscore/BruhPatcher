package com.nothingness.bruhpatcher

import com.nothingness.bruhpatcher.core.FeatureManager
import com.nothingness.bruhpatcher.model.Feature
import com.nothingness.bruhpatcher.model.FeatureCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureManagerTest {

    @Test
    fun testAllFeaturesCountAndDefaults() {
        val features = Feature.getAllFeatures()
        assertEquals(8, features.size)

        // All 8 features default to enabled
        val a17Unfinalize = features.first { it.id == "android17_build_unfinalize" }
        assertTrue(a17Unfinalize.isEnabled)
        assertEquals(FeatureCategory.CORE_FOUNDATION, a17Unfinalize.category)
        assertEquals(1, a17Unfinalize.stepNumber)

        val kaoriosCore = features.first { it.id == "kaorios_core" }
        assertTrue(kaoriosCore.isEnabled)
        assertTrue(kaoriosCore.isCorePrerequisite)
        assertEquals(1, kaoriosCore.stepNumber)

        val corePatch = features.first { it.id == "core_patch_dsv" }
        assertTrue(corePatch.isEnabled)
        assertEquals(FeatureCategory.SIGNATURE_BYPASS, corePatch.category)
        assertEquals(2, corePatch.stepNumber)

        val secureFlag = features.first { it.id == "disable_flag_secure" }
        assertTrue(secureFlag.isEnabled)
        assertEquals(FeatureCategory.PRIVACY_STEALTH, secureFlag.category)
        assertEquals(3, secureFlag.stepNumber)

        val hideDevAdb = features.first { it.id == "hide_developer_adb" }
        assertTrue(hideDevAdb.isEnabled)
        assertEquals(3, hideDevAdb.stepNumber)

        val hideApps = features.first { it.id == "hide_installed_apps" }
        assertTrue(hideApps.isEnabled)
        assertEquals(3, hideApps.stepNumber)

        val installerSpoof = features.first { it.id == "installer_source_spoof" }
        assertTrue(installerSpoof.isEnabled)
        assertEquals(FeatureCategory.SYSTEM_IDENTITY, installerSpoof.category)
        assertEquals(4, installerSpoof.stepNumber)

        val settingsFilter = features.first { it.id == "settings_filtering" }
        assertTrue(settingsFilter.isEnabled)
        assertEquals(4, settingsFilter.stepNumber)
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

        val oneEnabled = FeatureManager.updateFeature(allDisabled, "kaorios_core", true)
        assertEquals("KaoriOS Framework v3.0 Core", FeatureManager.getEnabledFeaturesSummary(oneEnabled))

        val twoEnabled = FeatureManager.updateFeature(oneEnabled, "core_patch_dsv", true)
        assertEquals("2 features selected", FeatureManager.getEnabledFeaturesSummary(twoEnabled))

        val allEnabled = Feature.getAllFeatures().map { it.copy(isEnabled = true) }
        assertEquals("8 features selected", FeatureManager.getEnabledFeaturesSummary(allEnabled))
    }

    @Test
    fun testBuildFeatureString() {
        val features = listOf(
            Feature.KAORIOS_CORE.copy(isEnabled = true),
            Feature.CORE_PATCH_DSV.copy(isEnabled = true),
            Feature.ANDROID17_BUILD_UNFINALIZE.copy(isEnabled = false)
        )
        val featureIds = FeatureManager.buildFeatureString(features)
        assertEquals("kaorios_core,core_patch_dsv", featureIds)
    }

    @Test
    fun testCanonicalFeatureOrderingAndStepAssignment() {
        val canonicalOrder = FeatureManager.CANONICAL_FEATURE_ORDER
        assertEquals(8, canonicalOrder.size)
        assertEquals("android17_build_unfinalize", canonicalOrder[0])
        assertEquals("kaorios_core", canonicalOrder[1])
        assertEquals("core_patch_dsv", canonicalOrder[2])
        assertEquals("disable_flag_secure", canonicalOrder[3])
        assertEquals("hide_developer_adb", canonicalOrder[4])
        assertEquals("hide_installed_apps", canonicalOrder[5])
        assertEquals("installer_source_spoof", canonicalOrder[6])
        assertEquals("settings_filtering", canonicalOrder[7])

        // Verify Step Info mapping
        for (id in canonicalOrder) {
            val (step, cat, warn) = FeatureManager.getFeatureStepInfo(id)
            assertTrue(step in 1..4)
            assertTrue(cat.isNotEmpty())
            if (id == "kaorios_core") {
                assertNotNull(warn)
            }
        }
    }

    @Test
    fun testAutoPatcherPresetAllFeaturesEnabled() {
        val allFeatures = Feature.getAllFeatures().map { it.copy(isEnabled = true) }
        val featureIds = FeatureManager.buildFeatureString(allFeatures)

        assertTrue(featureIds.contains("android17_build_unfinalize"))
        assertTrue(featureIds.contains("kaorios_core"))
        assertTrue(featureIds.contains("core_patch_dsv"))
        assertTrue(featureIds.contains("disable_flag_secure"))
        assertTrue(featureIds.contains("hide_developer_adb"))
        assertTrue(featureIds.contains("hide_installed_apps"))
        assertTrue(featureIds.contains("installer_source_spoof"))
        assertTrue(featureIds.contains("settings_filtering"))
        assertEquals("8 features selected", FeatureManager.getEnabledFeaturesSummary(allFeatures))
    }
}
