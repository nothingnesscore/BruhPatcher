package com.nothingness.bruhpatcher

import com.nothingness.bruhpatcher.model.DeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceInfoTest {

    @Test
    fun testPocoF6RedmiTurbo3Detection() {
        val pocoF6 = DeviceInfo(
            apiLevel = 37,
            androidVersion = "17",
            deviceCodename = "peridot",
            deviceName = "POCO F6",
            versionName = "OS4.0.1.0.UNACNXM",
            hyperOsVersion = "HyperOS 4.0",
            isHyperOS = true,
            hasFrameworkJar = true,
            hasServicesJar = true,
            hasMiuiServicesJar = true
        )

        assertTrue(pocoF6.isPocoF6OrTurbo3)
        assertTrue(pocoF6.isAndroid17)
        assertTrue(pocoF6.isHyperOS4)
        assertEquals("Snapdragon 8s Gen 3 (SM8635)", pocoF6.socDescription)
        assertEquals("android17", pocoF6.workflowVersion)
        assertEquals("HyperOS 4.0", pocoF6.osBadgeText)
    }

    @Test
    fun testRedmiTurbo3ModelNumber() {
        val redmiTurbo3 = DeviceInfo(
            apiLevel = 37,
            androidVersion = "17",
            deviceCodename = "peridot",
            deviceName = "24069RA21C",
            versionName = "OS4.0.2.0.UNCCNXM",
            hyperOsVersion = "HyperOS 4.0",
            isHyperOS = true,
            hasFrameworkJar = true,
            hasServicesJar = true,
            hasMiuiServicesJar = true
        )

        assertTrue(redmiTurbo3.isPocoF6OrTurbo3)
        assertTrue(redmiTurbo3.isAndroid17)
        assertTrue(redmiTurbo3.isHyperOS4)
        assertEquals("Snapdragon 8s Gen 3 (SM8635)", redmiTurbo3.socDescription)
    }

    @Test
    fun testGenericAospDevice() {
        val pixel8 = DeviceInfo(
            apiLevel = 34,
            androidVersion = "14",
            deviceCodename = "shiba",
            deviceName = "Pixel 8",
            versionName = "UP1A.231105.003",
            hyperOsVersion = null,
            isHyperOS = false,
            hasFrameworkJar = true,
            hasServicesJar = true,
            hasMiuiServicesJar = false
        )

        assertFalse(pixel8.isPocoF6OrTurbo3)
        assertFalse(pixel8.isAndroid17)
        assertFalse(pixel8.isHyperOS4)
        assertEquals("", pixel8.socDescription)
        assertEquals("Android 14", pixel8.osBadgeText)
    }

    @Test
    fun testEmptyDeviceInfoBoundary() {
        val empty = DeviceInfo.Empty
        assertFalse(empty.isPocoF6OrTurbo3)
        assertFalse(empty.isAndroid17)
        assertFalse(empty.isHyperOS4)
        assertEquals("", empty.socDescription)
    }
}
