package com.sharonZ.slowdown.viewmodel

import com.sharonZ.slowdown.data.local.entity.UsageRecord
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * AppDetailViewModel 逻辑的单元测试
 * 测试周平均使用时间计算
 */
class AppDetailViewModelTest {

    @Test
    fun `getWeeklyAverage returns 0 when no records`() {
        val records = emptyList<UsageRecord>()
        val average = calculateWeeklyAverage(records)
        assertEquals(0, average)
    }

    @Test
    fun `getWeeklyAverage returns correct average for single record`() {
        val records = listOf(
            UsageRecord(
                packageName = "com.example.app",
                date = LocalDate.now().toString(),
                usageMinutes = 60
            )
        )
        val average = calculateWeeklyAverage(records)
        assertEquals(60, average)
    }

    @Test
    fun `getWeeklyAverage returns correct average for multiple records`() {
        val records = listOf(
            UsageRecord(
                packageName = "com.example.app",
                date = LocalDate.now().toString(),
                usageMinutes = 60
            ),
            UsageRecord(
                packageName = "com.example.app",
                date = LocalDate.now().minusDays(1).toString(),
                usageMinutes = 30
            ),
            UsageRecord(
                packageName = "com.example.app",
                date = LocalDate.now().minusDays(2).toString(),
                usageMinutes = 90
            )
        )
        val average = calculateWeeklyAverage(records)
        assertEquals(60, average) // (60 + 30 + 90) / 3 = 60
    }

    @Test
    fun `getWeeklyAverage handles uneven division`() {
        val records = listOf(
            UsageRecord(
                packageName = "com.example.app",
                date = LocalDate.now().toString(),
                usageMinutes = 10
            ),
            UsageRecord(
                packageName = "com.example.app",
                date = LocalDate.now().minusDays(1).toString(),
                usageMinutes = 20
            )
        )
        val average = calculateWeeklyAverage(records)
        assertEquals(15, average) // (10 + 20) / 2 = 15
    }

    @Test
    fun `getWeeklyAverage with integer division rounds down`() {
        val records = listOf(
            UsageRecord(
                packageName = "com.example.app",
                date = LocalDate.now().toString(),
                usageMinutes = 10
            ),
            UsageRecord(
                packageName = "com.example.app",
                date = LocalDate.now().minusDays(1).toString(),
                usageMinutes = 11
            ),
            UsageRecord(
                packageName = "com.example.app",
                date = LocalDate.now().minusDays(2).toString(),
                usageMinutes = 12
            )
        )
        val average = calculateWeeklyAverage(records)
        assertEquals(11, average) // (10 + 11 + 12) / 3 = 11
    }

    /**
     * 辅助函数：计算周平均（复制自 AppDetailViewModel 的逻辑）
     */
    private fun calculateWeeklyAverage(records: List<UsageRecord>): Int {
        if (records.isEmpty()) return 0
        return records.sumOf { it.usageMinutes } / records.size
    }
}
