package com.example.slowdown.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.slowdown.data.local.entity.MonitoredApp
import com.example.slowdown.data.local.entity.UsageRecord
import com.example.slowdown.data.repository.SlowDownRepository
import com.example.slowdown.util.AppInfo
import com.example.slowdown.util.PackageUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Statistics ViewModel - 统计页面的 ViewModel
 *
 * 提供今日、本周、本月的使用数据聚合
 */
class StatisticsViewModel(
    private val repository: SlowDownRepository,
    private val context: Context
) : ViewModel() {

    // 应用信息缓存
    private val _installedApps = MutableStateFlow<Map<String, AppInfo>>(emptyMap())
    val installedApps: StateFlow<Map<String, AppInfo>> = _installedApps.asStateFlow()

    // 监控的应用列表
    val monitoredApps: StateFlow<List<MonitoredApp>> = repository.monitoredApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 今日使用总计（分钟）
    private val _todayTotalMinutes = MutableStateFlow(0)
    val todayTotalMinutes: StateFlow<Int> = _todayTotalMinutes.asStateFlow()

    // 今日各应用使用详情
    private val _todayUsageByApp = MutableStateFlow<List<AppUsageData>>(emptyList())
    val todayUsageByApp: StateFlow<List<AppUsageData>> = _todayUsageByApp.asStateFlow()

    // 本周每日使用数据
    private val _weeklyUsage = MutableStateFlow<List<DayUsageData>>(emptyList())
    val weeklyUsage: StateFlow<List<DayUsageData>> = _weeklyUsage.asStateFlow()

    // 本月总使用时间
    private val _monthTotalMinutes = MutableStateFlow(0)
    val monthTotalMinutes: StateFlow<Int> = _monthTotalMinutes.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadInstalledApps()
        loadStatistics()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = PackageUtils.getInstalledApps(context)
            _installedApps.value = apps.associateBy { it.packageName }
        }
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            _isLoading.value = true

            // 观察监控应用变化并更新统计
            combine(
                repository.monitoredApps,
                _installedApps
            ) { monitored, installed ->
                Pair(monitored, installed)
            }.collectLatest { (monitoredApps, installedApps) ->
                if (monitoredApps.isEmpty()) {
                    _todayTotalMinutes.value = 0
                    _todayUsageByApp.value = emptyList()
                    _weeklyUsage.value = emptyList()
                    _monthTotalMinutes.value = 0
                    _isLoading.value = false
                    return@collectLatest
                }

                // 收集所有应用的使用数据
                val today = LocalDate.now()
                val todayStr = today.toString()

                // 获取本周开始日期（周一）
                val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

                // 获取本月开始日期
                val monthStart = today.withDayOfMonth(1)

                // 收集各应用今日数据
                val todayAppUsages = mutableListOf<AppUsageData>()
                var totalToday = 0

                for (app in monitoredApps) {
                    val record = repository.getUsageRecord(app.packageName, todayStr)
                    val minutes = record?.usageMinutes ?: 0
                    totalToday += minutes

                    if (minutes > 0) {
                        val appInfo = installedApps[app.packageName]
                        todayAppUsages.add(
                            AppUsageData(
                                packageName = app.packageName,
                                appName = appInfo?.appName ?: app.packageName.substringAfterLast('.'),
                                appInfo = appInfo,
                                usageMinutes = minutes,
                                dailyLimitMinutes = app.dailyLimitMinutes
                            )
                        )
                    }
                }

                _todayTotalMinutes.value = totalToday
                _todayUsageByApp.value = todayAppUsages.sortedByDescending { it.usageMinutes }

                // 收集本周每日数据
                val weekDays = mutableListOf<DayUsageData>()
                var dayOffset = 0L
                while (weekStart.plusDays(dayOffset) <= today) {
                    val date = weekStart.plusDays(dayOffset)
                    val dateStr = date.toString()
                    var dayTotal = 0

                    for (app in monitoredApps) {
                        val record = repository.getUsageRecord(app.packageName, dateStr)
                        dayTotal += record?.usageMinutes ?: 0
                    }

                    weekDays.add(
                        DayUsageData(
                            date = date,
                            dayOfWeek = getDayOfWeekChinese(date.dayOfWeek),
                            totalMinutes = dayTotal
                        )
                    )
                    dayOffset++
                }
                _weeklyUsage.value = weekDays

                // 计算本月总计
                var monthTotal = 0
                var monthDayOffset = 0L
                while (monthStart.plusDays(monthDayOffset) <= today) {
                    val dateStr = monthStart.plusDays(monthDayOffset).toString()
                    for (app in monitoredApps) {
                        val record = repository.getUsageRecord(app.packageName, dateStr)
                        monthTotal += record?.usageMinutes ?: 0
                    }
                    monthDayOffset++
                }
                _monthTotalMinutes.value = monthTotal

                _isLoading.value = false
            }
        }
    }

    /**
     * 刷新统计数据
     */
    fun refresh() {
        loadStatistics()
    }

    /**
     * 格式化分钟数为 "X小时 Y分钟" 格式
     */
    fun formatDuration(minutes: Int): String {
        return when {
            minutes >= 60 -> {
                val hours = minutes / 60
                val mins = minutes % 60
                if (mins > 0) "${hours}小时 ${mins}分钟" else "${hours}小时"
            }
            else -> "${minutes}分钟"
        }
    }

    private fun getDayOfWeekChinese(dayOfWeek: DayOfWeek): String {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> "一"
            DayOfWeek.TUESDAY -> "二"
            DayOfWeek.WEDNESDAY -> "三"
            DayOfWeek.THURSDAY -> "四"
            DayOfWeek.FRIDAY -> "五"
            DayOfWeek.SATURDAY -> "六"
            DayOfWeek.SUNDAY -> "日"
        }
    }

    class Factory(
        private val repository: SlowDownRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatisticsViewModel(repository, context) as T
        }
    }
}

/**
 * 单个应用的使用数据
 */
data class AppUsageData(
    val packageName: String,
    val appName: String,
    val appInfo: AppInfo?,
    val usageMinutes: Int,
    val dailyLimitMinutes: Int?
) {
    /**
     * 计算使用百分比（相对于每日限额）
     */
    fun getUsagePercent(): Float {
        return if (dailyLimitMinutes != null && dailyLimitMinutes > 0) {
            (usageMinutes.toFloat() / dailyLimitMinutes).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
}

/**
 * 某一天的使用数据
 */
data class DayUsageData(
    val date: LocalDate,
    val dayOfWeek: String,
    val totalMinutes: Int
)
