package com.example.slowdown.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.slowdown.R
import com.example.slowdown.data.local.dao.AppStat
import com.example.slowdown.data.local.dao.DailyStat
import com.example.slowdown.data.local.dao.SuccessRateStat
import com.example.slowdown.data.local.entity.InterventionRecord
import com.example.slowdown.data.repository.SlowDownRepository
import com.example.slowdown.util.MiuiHelper
import com.example.slowdown.util.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 用户心理状态枚举
 * 根据今日拦截次数判定：0-2 平静，3-5 警觉，6+ 超限
 */
enum class MindfulState {
    CALM,     // 平静：青蓝色，8秒慢呼吸，正圆
    ALERT,    // 警觉：暖橙色，5秒快呼吸，微椭圆
    EXCEEDED  // 超限：暗紫色，12秒极慢，略收缩
}

/**
 * 今日觉知时刻数据
 * 改为存储资源 ID 而非固定字符串，支持语言切换
 */
data class AwarenessMoment(
    val id: Long,
    val timeOfDayResId: Int,      // 时间段资源 ID (R.string.time_morning 等)
    val messageResId: Int,         // 觉知文案资源 ID
    val messageArgs: Array<Any>,   // 格式化参数（如 appName）
    val appName: String,           // 涉及的应用名
    val userChoice: String,        // cancelled/continued/redirected
    val timestamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AwarenessMoment
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

class DashboardViewModel(
    private val repository: SlowDownRepository,
    private val context: Context
) : ViewModel() {

    val todayCount: StateFlow<Int> = repository.getTodayCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todaySuccessRate: StateFlow<SuccessRateStat> = repository.getTodaySuccessRate()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SuccessRateStat(0, 0))

    val weeklyStats: StateFlow<List<DailyStat>> = repository.getWeeklyStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topApps: StateFlow<List<AppStat>> = repository.getTopApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val serviceEnabled: StateFlow<Boolean> = repository.serviceEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // 今日干预记录
    private val todayInterventions: StateFlow<List<InterventionRecord>> = repository.getTodayInterventions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 用户心理状态（基于拦截次数判定）
    val mindfulState: StateFlow<MindfulState> = todayCount.map { count ->
        when {
            count <= 2 -> MindfulState.CALM
            count <= 5 -> MindfulState.ALERT
            else -> MindfulState.EXCEEDED
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MindfulState.CALM)

    // 今日觉知时刻（最多显示3条最近记录）
    val awarenessMoments: StateFlow<List<AwarenessMoment>> = todayInterventions.map { records ->
        records.take(3).map { record ->
            AwarenessMoment(
                id = record.id,
                timeOfDayResId = getTimeOfDayResId(record.timestamp),
                messageResId = getAwarenessMessageResId(record),
                messageArgs = arrayOf(record.appName),
                appName = record.appName,
                userChoice = record.userChoice,
                timestamp = record.timestamp
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // MIUI 手动确认状态
    private val miuiAutoStartConfirmed: StateFlow<Boolean> = repository.miuiAutoStartConfirmed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val miuiBatterySaverConfirmed: StateFlow<Boolean> = repository.miuiBatterySaverConfirmed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val miuiLockAppConfirmed: StateFlow<Boolean> = repository.miuiLockAppConfirmed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    init {
        refreshPermissions()
        // 监听 MIUI 确认状态变化
        viewModelScope.launch {
            combine(
                miuiAutoStartConfirmed,
                miuiBatterySaverConfirmed,
                miuiLockAppConfirmed
            ) { autoStart, battery, lock ->
                Triple(autoStart, battery, lock)
            }.collect { (autoStart, battery, lock) ->
                _permissionState.value = _permissionState.value.copy(
                    miuiAutoStartConfirmed = autoStart,
                    miuiBatterySaverConfirmed = battery,
                    miuiLockAppConfirmed = lock
                )
            }
        }
    }

    /**
     * 根据时间戳返回时间段资源 ID
     */
    private fun getTimeOfDayResId(timestamp: Long): Int {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> R.string.time_morning
            hour < 18 -> R.string.time_afternoon
            else -> R.string.time_evening
        }
    }

    /**
     * 生成觉知文案资源 ID
     */
    private fun getAwarenessMessageResId(record: InterventionRecord): Int {
        return when (record.userChoice) {
            "cancelled" -> R.string.awareness_chose_leave
            "continued" -> R.string.awareness_continued_after_breath
            "redirected" -> R.string.awareness_chose_redirect
            else -> R.string.awareness_paused_before
        }
    }

    fun refreshPermissions() {
        val isMiui = PermissionHelper.isMiui()
        val newState = PermissionState(
            accessibilityEnabled = PermissionHelper.isAccessibilityEnabled(context),
            overlayEnabled = PermissionHelper.canDrawOverlays(context),
            batteryOptimizationDisabled = PermissionHelper.isIgnoringBatteryOptimizations(context),
            usageStatsEnabled = PermissionHelper.hasUsageStatsPermission(context),
            isMiui = isMiui,
            // 使用 MiuiHelper 检测实际权限状态
            miuiBackgroundPopupGranted = if (isMiui) MiuiHelper.canBackgroundStart(context) else true,
            miuiAutoStartConfirmed = miuiAutoStartConfirmed.value,
            miuiBatterySaverConfirmed = miuiBatterySaverConfirmed.value,
            miuiLockAppConfirmed = miuiLockAppConfirmed.value
        )
        _permissionState.value = newState

        // 如果必要权限不足，自动关闭 Protection
        if (!newState.allRequiredPermissionsGranted && serviceEnabled.value) {
            viewModelScope.launch {
                repository.setServiceEnabled(false)
            }
        }
    }

    fun setServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setServiceEnabled(enabled)
        }
    }

    fun openAccessibilitySettings() = PermissionHelper.openAccessibilitySettings(context)
    fun openOverlaySettings() = PermissionHelper.openOverlaySettings(context)
    fun openBatterySettings() = PermissionHelper.openBatteryOptimizationSettings(context)

    class Factory(
        private val repository: SlowDownRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository, context) as T
        }
    }
}
