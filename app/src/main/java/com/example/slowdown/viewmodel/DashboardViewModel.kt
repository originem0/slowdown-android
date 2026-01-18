package com.example.slowdown.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.slowdown.data.local.dao.AppStat
import com.example.slowdown.data.local.dao.DailyStat
import com.example.slowdown.data.local.dao.SuccessRateStat
import com.example.slowdown.data.repository.SlowDownRepository
import com.example.slowdown.util.MiuiHelper
import com.example.slowdown.util.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    fun refreshPermissions() {
        val isMiui = PermissionHelper.isMiui()
        _permissionState.value = PermissionState(
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
