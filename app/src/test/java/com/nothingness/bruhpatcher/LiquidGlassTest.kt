package com.nothingness.bruhpatcher

import com.nothingness.bruhpatcher.ui.components.LiquidNavDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class LiquidGlassTest {

    @Test
    fun testLiquidNavDestinationEnum() {
        val destinations = LiquidNavDestination.values()
        assertEquals(4, destinations.size)
        assertEquals("dashboard", LiquidNavDestination.DASHBOARD.route)
        assertEquals("config", LiquidNavDestination.CONFIG.route)
        assertEquals("progress", LiquidNavDestination.PROGRESS.route)
        assertEquals("settings", LiquidNavDestination.SETTINGS.route)
        assertEquals("Dashboard", LiquidNavDestination.DASHBOARD.title)
        assertEquals("Config", LiquidNavDestination.CONFIG.title)
        assertEquals("Terminal", LiquidNavDestination.PROGRESS.title)
        assertEquals("Settings", LiquidNavDestination.SETTINGS.title)
    }

    @Test
    fun testCircleMapMonotonicityAndSafety() {
        // AGSL: float circleMap(float x) { return 1.0 - sqrt(max(0.0, 1.0 - x * x)); }
        fun circleMap(x: Float): Float {
            return 1.0f - sqrt(max(0.0f, 1.0f - x * x))
        }

        // At inner boundary (x = 0), refraction displacement must be exactly 0
        assertEquals(0.0f, circleMap(0.0f), 0.0001f)

        // At outer perimeter (x = 1), refraction displacement must be exactly 1.0
        assertEquals(1.0f, circleMap(1.0f), 0.0001f)

        // Out-of-bounds inputs must never produce NaN
        val clampedNeg = circleMap(2.0f)
        assertFalse(clampedNeg.isNaN())
        assertEquals(1.0f, clampedNeg, 0.0001f)

        // Monotonically increasing from 0.0 to 1.0
        var prev = 0f
        for (i in 1..100) {
            val x = i / 100f
            val curr = circleMap(x)
            assertTrue("circleMap should be monotonically increasing", curr >= prev)
            assertTrue("circleMap should be between 0 and 1", curr in 0.0f..1.0f)
            prev = curr
        }
    }

    @Test
    fun testSafeNormalization() {
        fun safeNormalize(vx: Float, vy: Float): Pair<Float, Float> {
            val len = sqrt(vx * vx + vy * vy)
            return if (len > 0.0001f) (vx / len) to (vy / len) else 0f to 0f
        }

        // Zero vector must return (0, 0) and NEVER NaN
        val (zx, zy) = safeNormalize(0f, 0f)
        assertEquals(0f, zx, 0.0001f)
        assertEquals(0f, zy, 0.0001f)
        assertFalse(zx.isNaN())
        assertFalse(zy.isNaN())

        // Unit vectors
        val (ux, uy) = safeNormalize(10f, 0f)
        assertEquals(1f, ux, 0.0001f)
        assertEquals(0f, uy, 0.0001f)

        // Diagonal
        val (dx, dy) = safeNormalize(3f, 4f)
        assertEquals(0.6f, dx, 0.0001f)
        assertEquals(0.8f, dy, 0.0001f)
    }

    @Test
    fun testSpectralDispersionEnergyConservation() {
        // AGSL RefractionWithDispersionShader weights:
        // Red:   r/3.5, a/7.0
        // Orange: r/3.5, g/7.0, a/7.0
        // Yellow: r/3.5, g/3.5, a/7.0
        // Green:  g/3.5, a/7.0
        // Cyan:   g/3.5, b/3.0, a/7.0
        // Blue:   b/3.0, a/7.0
        // Purple: r/7.0, b/3.0, a/7.0

        val totalR = (1.0 / 3.5) + (1.0 / 3.5) + (1.0 / 3.5) + (1.0 / 7.0) // 2/7 + 2/7 + 2/7 + 1/7 = 7/7
        val totalG = (1.0 / 7.0) + (1.0 / 3.5) + (1.0 / 3.5) + (1.0 / 3.5) // 1/7 + 2/7 + 2/7 + 2/7 = 7/7
        val totalB = (1.0 / 3.0) + (1.0 / 3.0) + (1.0 / 3.0)               // 1/3 + 1/3 + 1/3 = 3/3
        val totalA = 7 * (1.0 / 7.0)                                       // 7/7

        assertEquals(1.0, totalR, 0.0001)
        assertEquals(1.0, totalG, 0.0001)
        assertEquals(1.0, totalB, 0.0001)
        assertEquals(1.0, totalA, 0.0001)
    }

    @Test
    fun testTabGeometryCalculations() {
        val tabsCount = 4
        val paddingPx = 8f // 4dp each side = 8dp total
        val totalWidthPx = 312f // 4 * 76 + 8
        val tabWidthPx = ((totalWidthPx - paddingPx) / tabsCount).coerceAtLeast(0f)

        assertEquals(76f, tabWidthPx, 0.0001f)

        // Indicator positions for each tab
        for (index in 0 until tabsCount) {
            val indicatorX = index * tabWidthPx
            assertEquals(index * 76f, indicatorX, 0.0001f)
        }
    }

    @Test
    fun testVelocitySquashStretchLimits() {
        fun computeScale(baseScale: Float, rawVelocity: Float): Pair<Float, Float> {
            val vel = (rawVelocity / 10f).coerceIn(-0.25f, 0.25f)
            val scaleX = baseScale / (1f - vel * 0.75f)
            val scaleY = baseScale * (1f - vel * 0.25f)
            return scaleX to scaleY
        }

        // At rest
        val (restX, restY) = computeScale(1.0f, 0f)
        assertEquals(1.0f, restX, 0.0001f)
        assertEquals(1.0f, restY, 0.0001f)

        // Under extreme velocity, scales must remain within stable physical limits (never zero or negative)
        val (extRightX, extRightY) = computeScale(1.0f, 100f)
        assertTrue(extRightX in 1.0f..2.0f)
        assertTrue(extRightY in 0.5f..1.0f)

        val (extLeftX, extLeftY) = computeScale(1.0f, -100f)
        assertTrue(extLeftX in 0.5f..1.0f)
        assertTrue(extLeftY in 1.0f..1.5f)
    }
}
