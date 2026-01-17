package com.example.slowdown.viewmodel

import org.junit.Assert.*
import org.junit.Test

/**
 * AppUsageData 数据类的单元测试
 */
class AppUsageDataTest {

    @Test
    fun `getUsagePercent returns 0 when dailyLimitMinutes is null`() {
        val data = AppUsageData(
            packageName = "com.example.app",
            appName = "Test App",
            appInfo = null,
            usageMinutes = 30,
            dailyLimitMinutes = null
        )
        assertEquals(0f, data.getUsagePercent(), 0.001f)
    }

    @Test
    fun `getUsagePercent returns 0 when dailyLimitMinutes is 0`() {
        val data = AppUsageData(
            packageName = "com.example.app",
            appName = "Test App",
            appInfo = null,
            usageMinutes = 30,
            dailyLimitMinutes = 0
        )
        assertEquals(0f, data.getUsagePercent(), 0.001f)
    }

    @Test
    fun `getUsagePercent returns correct percentage`() {
        val data = AppUsageData(
            packageName = "com.example.app",
            appName = "Test App",
            appInfo = null,
            usageMinutes = 30,
            dailyLimitMinutes = 60
        )
        assertEquals(0.5f, data.getUsagePercent(), 0.001f)
    }

    @Test
    fun `getUsagePercent returns 1 when usage equals limit`() {
        val data = AppUsageData(
            packageName = "com.example.app",
            appName = "Test App",
            appInfo = null,
            usageMinutes = 60,
            dailyLimitMinutes = 60
        )
        assertEquals(1f, data.getUsagePercent(), 0.001f)
    }

    @Test
    fun `getUsagePercent is clamped to 1 when usage exceeds limit`() {
        val data = AppUsageData(
            packageName = "com.example.app",
            appName = "Test App",
            appInfo = null,
            usageMinutes = 120,
            dailyLimitMinutes = 60
        )
        assertEquals(1f, data.getUsagePercent(), 0.001f)
    }

    @Test
    fun `getUsagePercent handles 80 percent threshold`() {
        val data = AppUsageData(
            packageName = "com.example.app",
            appName = "Test App",
            appInfo = null,
            usageMinutes = 48,
            dailyLimitMinutes = 60
        )
        assertEquals(0.8f, data.getUsagePercent(), 0.001f)
    }
}
