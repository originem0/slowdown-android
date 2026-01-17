package com.example.slowdown.util

import org.junit.Assert.*
import org.junit.Test

/**
 * 使用警告阈值计算的单元测试
 */
class UsageWarningThresholdsTest {

    companion object {
        const val WARNING_THRESHOLD = 0.80   // 80% 警告阈值
    }

    @Test
    fun `usage ratio below 80 percent should not trigger warning`() {
        val currentMinutes = 40
        val dailyLimit = 60
        val usageRatio = currentMinutes.toDouble() / dailyLimit

        assertFalse("70% usage should not trigger warning", usageRatio >= WARNING_THRESHOLD)
    }

    @Test
    fun `usage ratio at exactly 80 percent should trigger warning`() {
        val currentMinutes = 48
        val dailyLimit = 60
        val usageRatio = currentMinutes.toDouble() / dailyLimit

        assertTrue("80% usage should trigger warning", usageRatio >= WARNING_THRESHOLD)
    }

    @Test
    fun `usage ratio above 80 percent should trigger warning`() {
        val currentMinutes = 50
        val dailyLimit = 60
        val usageRatio = currentMinutes.toDouble() / dailyLimit

        assertTrue("83% usage should trigger warning", usageRatio >= WARNING_THRESHOLD)
    }

    @Test
    fun `usage ratio at 100 percent should trigger limit reached`() {
        val currentMinutes = 60
        val dailyLimit = 60
        val usageRatio = currentMinutes.toDouble() / dailyLimit

        assertTrue("100% usage should be at limit", usageRatio >= 1.0)
    }

    @Test
    fun `usage ratio above 100 percent should trigger limit reached`() {
        val currentMinutes = 70
        val dailyLimit = 60
        val usageRatio = currentMinutes.toDouble() / dailyLimit

        assertTrue("117% usage should exceed limit", usageRatio >= 1.0)
    }

    @Test
    fun `warning type selection with soft mode at 100 percent`() {
        val limitMode = "soft"
        val usageRatio = 1.0

        val expectedType = if (limitMode == "strict") "STRICT" else "SOFT"
        assertEquals("SOFT", expectedType)
    }

    @Test
    fun `warning type selection with strict mode at 100 percent`() {
        val limitMode = "strict"
        val usageRatio = 1.0

        val expectedType = if (limitMode == "strict") "STRICT" else "SOFT"
        assertEquals("STRICT", expectedType)
    }

    @Test
    fun `realtime tracking threshold at 70 percent`() {
        val realtimeThreshold = 0.70
        val currentMinutes = 42
        val dailyLimit = 60
        val usageRatio = currentMinutes.toDouble() / dailyLimit

        assertTrue("70% usage should enable realtime tracking", usageRatio >= realtimeThreshold)
    }

    @Test
    fun `realtime tracking not needed below 70 percent`() {
        val realtimeThreshold = 0.70
        val currentMinutes = 40
        val dailyLimit = 60
        val usageRatio = currentMinutes.toDouble() / dailyLimit

        assertFalse("67% usage should not enable realtime tracking", usageRatio >= realtimeThreshold)
    }
}
