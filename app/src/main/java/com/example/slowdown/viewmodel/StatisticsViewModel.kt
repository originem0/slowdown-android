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
import com.example.slowdown.util.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

                // 在 IO 线程执行数据库查询
                withContext(Dispatchers.IO) {
                    // 使用选择的日期
                    val targetDate = selectedDate
                    val targetDateStr = targetDate.toString()
                    val yesterdayStr = targetDate.minusDays(1).toString()

                    // 获取所选日期所在周的范围（周一到周日）
                    val (weekStart, _) = getWeekRange(targetDate)

                    // 获取所选日期所在月的开始日期
                    val monthStart = targetDate.withDayOfMonth(1)

                    // 收集各应用目标日期数据
                    val todayAppUsages = mutableListOf<AppUsageData>()
                    var totalToday = 0
                    var totalYesterday = 0

                    for (app in monitoredApps) {
                        // 目标日期数据
                        val record = repository.getUsageRecord(app.packageName, targetDateStr)
                        val minutes = record?.usageMinutes ?: 0
                        totalToday += minutes

                        // 前一天数据（用于对比）
                        val yesterdayRecord = repository.getUsageRecord(app.packageName, yesterdayStr)
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

                    // 收集本周每日数据（周一到周日，显示完整一周）
                    val weekDays = mutableListOf<DayUsageData>()
                    val today = LocalDate.now()
                    for (dayOffset in 0L until 7L) {
                        val date = weekStart.plusDays(dayOffset)
                        val dateStr = date.toString()
                        var dayTotal = 0

                        // 只有不超过今天的日期才有数据
                        if (date <= today) {
                            for (app in monitoredApps) {
                                val record = repository.getUsageRecord(app.packageName, dateStr)
                                dayTotal += record?.usageMinutes ?: 0
                            }
                        }

                        weekDays.add(
                            DayUsageData(
                                date = date,
                                dayOfWeek = getDayOfWeekChinese(date.dayOfWeek),
                                totalMinutes = dayTotal,
                                isSelected = date == targetDate
                            )
                        )
                    }
                    _weeklyUsage.value = weekDays

                    // 计算本月总计（从月初到选择的日期或今天，取较小者）
                    val monthEndDate = if (targetDate > today) today else targetDate
                    var monthTotal = 0
                    var monthDayOffset = 0L
                    while (monthStart.plusDays(monthDayOffset) <= monthEndDate) {
                        val dateStr = monthStart.plusDays(monthDayOffset).toString()
                        for (app in monitoredApps) {
                            val record = repository.getUsageRecord(app.packageName, dateStr)
                            monthTotal += record?.usageMinutes ?: 0
                        }
                        monthDayOffset++
                    }
                    _monthTotalMinutes.value = monthTotal
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
        return com.example.slowdown.util.formatDuration(context, minutes)
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
    val isSelected: Boolean = false
)
