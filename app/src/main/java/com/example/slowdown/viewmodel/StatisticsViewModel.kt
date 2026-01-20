package com.sharonZ.slowdown.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sharonZ.slowdown.data.local.entity.MonitoredApp
import com.sharonZ.slowdown.data.local.entity.UsageRecord
import com.sharonZ.slowdown.data.repository.SlowDownRepository
import com.sharonZ.slowdown.util.AppInfo
import com.sharonZ.slowdown.util.PackageUtils
import com.sharonZ.slowdown.util.formatDuration
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Statistics ViewModel - 统计页面的 ViewModel
 *
 * 提供今日、本周、本月的使用数据聚合
 */
class StatisticsViewModel(
    private val repository: SlowDownRepository,
    private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "StatisticsViewModel"
    }

    // 日期格式化
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINESE)

    // 应用信息缓存
    private val _installedApps = MutableStateFlow<Map<String, AppInfo>>(emptyMap())
    val installedApps: StateFlow<Map<String, AppInfo>> = _installedApps.asStateFlow()

    // 监控的应用列表
    val monitoredApps: StateFlow<List<MonitoredApp>> = repository.monitoredApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 当前选择的日期
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // 格式化的日期文字
    val formattedDate: StateFlow<String> = _selectedDate.map { date ->
        date.format(dateFormatter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // 今日使用总计（分钟）
    private val _todayTotalMinutes = MutableStateFlow(0)
    val todayTotalMinutes: StateFlow<Int> = _todayTotalMinutes.asStateFlow()

    // 昨日使用总计（分钟）- 用于对比
    private val _yesterdayTotalMinutes = MutableStateFlow(0)
    val yesterdayTotalMinutes: StateFlow<Int> = _yesterdayTotalMinutes.asStateFlow()

    // 今日各应用使用详情
    private val _todayUsageByApp = MutableStateFlow<List<AppUsageData>>(emptyList())
    val todayUsageByApp: StateFlow<List<AppUsageData>> = _todayUsageByApp.asStateFlow()

    // 本周每日使用数据
    private val _weeklyUsage = MutableStateFlow<List<DayUsageData>>(emptyList())
    val weeklyUsage: StateFlow<List<DayUsageData>> = _weeklyUsage.asStateFlow()

    // 本月总使用时间
    private val _monthTotalMinutes = MutableStateFlow(0)
    val monthTotalMinutes: StateFlow<Int> = _monthTotalMinutes.asStateFlow()

    // 月度对比数据
    private val _monthComparison = MutableStateFlow<MonthComparisonData?>(null)
    val monthComparison: StateFlow<MonthComparisonData?> = _monthComparison.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadInstalledApps()
        loadStatistics()
    }

    /**
     * 选择日期
     */
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        loadStatistics()
    }

    /**
     * 选择前一天
     */
    fun selectPreviousDay() {
        selectDate(_selectedDate.value.minusDays(1))
    }

    /**
     * 选择后一天
     */
    fun selectNextDay() {
        val nextDay = _selectedDate.value.plusDays(1)
        if (nextDay <= LocalDate.now()) {
            selectDate(nextDay)
        }
    }

    /**
     * 是否可以选择后一天（不能超过今天）
     */
    fun canSelectNextDay(): Boolean {
        return _selectedDate.value < LocalDate.now()
    }

    /**
     * 获取指定日期所在周的范围（周一到周日）
     */
    private fun getWeekRange(date: LocalDate): Pair<LocalDate, LocalDate> {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)
        return Pair(monday, sunday)
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = PackageUtils.getInstalledApps(context)
            _installedApps.value = apps.associateBy { it.packageName }
        }
    }

    @OptIn(FlowPreview::class)
    private fun loadStatistics() {
        viewModelScope.launch {
            _isLoading.value = true

            // 观察监控应用变化并更新统计，添加防抖动
            combine(
                repository.monitoredApps,
                _installedApps,
                _selectedDate
            ) { monitored, installed, date ->
                Triple(monitored, installed, date)
            }
            .debounce(100) // 防止快速切换日期时频繁加载
            .collectLatest { (monitoredApps, installedApps, selectedDate) ->
                if (monitoredApps.isEmpty()) {
                    _todayTotalMinutes.value = 0
                    _yesterdayTotalMinutes.value = 0
                    _todayUsageByApp.value = emptyList()
                    _weeklyUsage.value = emptyList()
                    _monthTotalMinutes.value = 0
                    _isLoading.value = false
                    return@collectLatest
                }

                // 在 IO 线程执行数据库查询，添加错误处理和超时保护
                try {
                    val result = withTimeoutOrNull(10_000L) { // 10 秒超时
                        withContext(Dispatchers.IO) {
                        // 使用选择的日期
                        val targetDate = selectedDate
                        val targetDateStr = targetDate.toString()
                        val yesterdayStr = targetDate.minusDays(1).toString()

                        // 获取所选日期所在周的范围（周一到周日）
                        val (weekStart, _) = getWeekRange(targetDate)

                        // 获取所选日期所在月的开始日期
                        val monthStart = targetDate.withDayOfMonth(1)

                        // 批量查询优化：收集所有需要查询的日期
                        val datesToQuery = mutableSetOf<String>()
                        datesToQuery.add(targetDateStr)
                        datesToQuery.add(yesterdayStr)

                        // 添加本周日期
                        val today = LocalDate.now()
                        for (dayOffset in 0L until 7L) {
                            val date = weekStart.plusDays(dayOffset)
                            if (date <= today) {
                                datesToQuery.add(date.toString())
                            }
                        }

                        // 添加本月日期
                        val monthEndDate = if (targetDate > today) today else targetDate
                        var monthDayOffset = 0L
                        while (monthStart.plusDays(monthDayOffset) <= monthEndDate) {
                            datesToQuery.add(monthStart.plusDays(monthDayOffset).toString())
                            monthDayOffset++
                        }

                        // 添加上月同期日期
                        val dayOfMonth = monthEndDate.dayOfMonth
                        val lastMonthStart = monthStart.minusMonths(1)
                        val lastMonthEnd = lastMonthStart.plusDays(dayOfMonth.toLong() - 1)
                        var lastMonthDayOffset = 0L
                        while (lastMonthStart.plusDays(lastMonthDayOffset) <= lastMonthEnd) {
                            datesToQuery.add(lastMonthStart.plusDays(lastMonthDayOffset).toString())
                            lastMonthDayOffset++
                        }

                        // 一次性批量查询所有日期的使用记录
                        val usageRecordsMap = repository.getUsageRecordsByDates(datesToQuery.toList())

                        // 收集各应用目标日期数据
                        val todayAppUsages = mutableListOf<AppUsageData>()
                        var totalToday = 0
                        var totalYesterday = 0

                        for (app in monitoredApps) {
                            // 目标日期数据
                            val record = usageRecordsMap["${app.packageName}_$targetDateStr"]
                            val minutes = record?.usageMinutes ?: 0
                            totalToday += minutes

                            // 前一天数据（用于对比）
                            val yesterdayRecord = usageRecordsMap["${app.packageName}_$yesterdayStr"]
                            totalYesterday += yesterdayRecord?.usageMinutes ?: 0

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
                        _yesterdayTotalMinutes.value = totalYesterday
                        _todayUsageByApp.value = todayAppUsages.sortedByDescending { it.usageMinutes }

                        // 获取本周拦截统计
                        val weekStartMillis = weekStart.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        // 修复：使用周日结束时间，而不是下周一开始时间，避免排除当前周数据
                        val weekEndDate = weekStart.plusDays(6)  // 周日
                        val actualEndDate = if (weekEndDate > today) today else weekEndDate
                        val weekEndMillis = actualEndDate.plusDays(1)  // 下一天 00:00:00，用于不包含边界查询
                            .atStartOfDay(java.time.ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                        val weeklyInterventions = repository.getDailyStatsBetween(weekStartMillis, weekEndMillis)
                        val interventionsByDate = weeklyInterventions.associate { it.day to it.count }

                        // 收集本周每日数据（周一到周日，显示完整一周）
                        val weekDays = mutableListOf<DayUsageData>()
                        for (dayOffset in 0L until 7L) {
                            val date = weekStart.plusDays(dayOffset)
                            val dateStr = date.toString()
                            var dayTotal = 0

                            // 只有不超过今天的日期才有数据，使用批量查询的结果
                            if (date <= today) {
                                for (app in monitoredApps) {
                                    val record = usageRecordsMap["${app.packageName}_$dateStr"]
                                    dayTotal += record?.usageMinutes ?: 0
                                }
                            }

                            weekDays.add(
                                DayUsageData(
                                    date = date,
                                    dayOfWeek = getDayOfWeekChinese(date.dayOfWeek),
                                    totalMinutes = dayTotal,
                                    interventionCount = interventionsByDate[dateStr] ?: 0,
                                    isSelected = date == targetDate
                                )
                            )
                        }
                        _weeklyUsage.value = weekDays

                        // 计算本月总计（从月初到选择的日期或今天，取较小者）- 使用批量查询结果
                        var monthTotal = 0
                        monthDayOffset = 0L
                        while (monthStart.plusDays(monthDayOffset) <= monthEndDate) {
                            val dateStr = monthStart.plusDays(monthDayOffset).toString()
                            for (app in monitoredApps) {
                                val record = usageRecordsMap["${app.packageName}_$dateStr"]
                                monthTotal += record?.usageMinutes ?: 0
                            }
                            monthDayOffset++
                        }
                        _monthTotalMinutes.value = monthTotal

                        // 计算上月同期使用时长 - 使用批量查询结果
                        var lastMonthTotal = 0
                        lastMonthDayOffset = 0L
                        while (lastMonthStart.plusDays(lastMonthDayOffset) <= lastMonthEnd) {
                            val dateStr = lastMonthStart.plusDays(lastMonthDayOffset).toString()
                            for (app in monitoredApps) {
                                val record = usageRecordsMap["${app.packageName}_$dateStr"]
                                lastMonthTotal += record?.usageMinutes ?: 0
                            }
                            lastMonthDayOffset++
                        }

                        // 计算本月和上月同期的拦截统计
                        val thisMonthStartMillis = monthStart.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val thisMonthEndMillis = monthEndDate.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val lastMonthStartMillis = lastMonthStart.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val lastMonthEndMillis = lastMonthEnd.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

                        val thisMonthInterventions = repository.getCountBetween(thisMonthStartMillis, thisMonthEndMillis)
                        val lastMonthInterventions = repository.getCountBetween(lastMonthStartMillis, lastMonthEndMillis)

                        val thisMonthSuccessRateStat = repository.getSuccessRateBetween(thisMonthStartMillis, thisMonthEndMillis)
                        val lastMonthSuccessRateStat = repository.getSuccessRateBetween(lastMonthStartMillis, lastMonthEndMillis)

                        val thisMonthSuccessRate = if (thisMonthSuccessRateStat.total > 0)
                            (thisMonthSuccessRateStat.successful * 100 / thisMonthSuccessRateStat.total) else 0
                        val lastMonthSuccessRate = if (lastMonthSuccessRateStat.total > 0)
                            (lastMonthSuccessRateStat.successful * 100 / lastMonthSuccessRateStat.total) else 0

                        _monthComparison.value = MonthComparisonData(
                            thisMonthUsage = monthTotal,
                            lastMonthUsage = lastMonthTotal,
                            thisMonthInterventions = thisMonthInterventions,
                            lastMonthInterventions = lastMonthInterventions,
                            thisMonthSuccessRate = thisMonthSuccessRate,
                            lastMonthSuccessRate = lastMonthSuccessRate
                        )
                        }
                        true // 返回成功标志
                    }
                    if (result == null) {
                        Log.w(TAG, "Statistics loading timed out after 10 seconds")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load statistics: ${e.message}", e)
                    // 出错时保持当前数据，不清空
                }

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
     * 格式化分钟数为 "X小时 Y分钟" 格式（本地化）
     */
    fun formatDuration(minutes: Int): String {
        return com.sharonZ.slowdown.util.formatDuration(context, minutes)
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
    val totalMinutes: Int,
    val interventionCount: Int = 0,  // 拦截次数
    val isSelected: Boolean = false
)

/**
 * 月度对比数据
 */
data class MonthComparisonData(
    val thisMonthUsage: Int,      // 本月使用分钟数
    val lastMonthUsage: Int,      // 上月同期使用分钟数
    val thisMonthInterventions: Int,  // 本月拦截次数
    val lastMonthInterventions: Int,  // 上月同期拦截次数
    val thisMonthSuccessRate: Int,    // 本月成功率 (%)
    val lastMonthSuccessRate: Int     // 上月同期成功率 (%)
) {
    val usageChange: Int get() = if (lastMonthUsage > 0) ((thisMonthUsage - lastMonthUsage) * 100 / lastMonthUsage) else 0
    val interventionChange: Int get() = if (lastMonthInterventions > 0) ((thisMonthInterventions - lastMonthInterventions) * 100 / lastMonthInterventions) else 0
    val successRateChange: Int get() = thisMonthSuccessRate - lastMonthSuccessRate
}
