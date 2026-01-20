package com.sharonZ.slowdown.util

import org.junit.Assert.*
import org.junit.Test

/**
 * 时间格式化函数的单元测试
 */
class DurationFormatterTest {

    @Test
    fun `formatDuration with 0 minutes returns 0分钟`() {
        assertEquals("0分钟", formatDuration(0))
    }

    @Test
    fun `formatDuration with 30 minutes returns 30分钟`() {
        assertEquals("30分钟", formatDuration(30))
    }

    @Test
    fun `formatDuration with 59 minutes returns 59分钟`() {
        assertEquals("59分钟", formatDuration(59))
    }

    @Test
    fun `formatDuration with 60 minutes returns 1小时`() {
        assertEquals("1小时", formatDuration(60))
    }

    @Test
    fun `formatDuration with 90 minutes returns 1小时 30分钟`() {
        assertEquals("1小时 30分钟", formatDuration(90))
    }

    @Test
    fun `formatDuration with 120 minutes returns 2小时`() {
        assertEquals("2小时", formatDuration(120))
    }

    @Test
    fun `formatDuration with 150 minutes returns 2小时 30分钟`() {
        assertEquals("2小时 30分钟", formatDuration(150))
    }

    @Test
    fun `formatDuration with large value works correctly`() {
        assertEquals("10小时", formatDuration(600))
        assertEquals("24小时", formatDuration(1440))
    }
}
