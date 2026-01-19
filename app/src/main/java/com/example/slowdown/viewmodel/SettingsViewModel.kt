package com.example.slowdown.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.slowdown.data.repository.SlowDownRepository
import com.example.slowdown.util.MiuiHelper
import com.example.slowdown.util.PermissionHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SlowDownRepository,
    private val context: Context
) : ViewModel() {

    val defaultCountdown: StateFlow<Int> = repository.defaultCountdown
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val cooldownMinutes: StateFlow<Int> = repository.cooldownMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val customReminderTexts: StateFlow<String> = repository.customReminderTexts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val miuiAutoStartConfirmed: StateFlow<Boolean> = repository.miuiAutoStartConfirmed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val miuiBackgroundPopupConfirmed: StateFlow<Boolean> = repository.miuiBackgroundPopupConfirmed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val miuiBatterySaverConfirmed: StateFlow<Boolean> = repository.miuiBatterySaverConfirmed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val miuiLockAppConfirmed: StateFlow<Boolean> = repository.miuiLockAppConfirmed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentLanguage: StateFlow<String> = repository.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    // 记录上一次使用统计权限状态，用于检测变化
    private var previousUsageStatsEnabled: Boolean = false

    init {
        // 初始化时获取当前使用统计权限状态
        previousUsageStatsEnabled = PermissionHelper.hasUsageStatsPermission(context)
        refreshPermissions()
        // 监听 MIUI 确认状态变化
        viewModelScope.launch {
            combine(
                miuiAutoStartConfirmed,
                miuiBackgroundPopupConfirmed,
                miuiBatterySaverConfirmed,
                miuiLockAppConfirmed
            ) { autoStart, popup, battery, lock ->
                arrayOf(autoStart, popup, battery, lock)
            }.collect { states ->
                _permissionState.value = _permissionState.value.copy(
                    miuiAutoStartConfirmed = states[0],
                    miuiBackgroundPopupConfirmed = states[1],
                    miuiBatterySaverConfirmed = states[2],
                    miuiLockAppConfirmed = states[3]
                )
            }
        }
    }

    fun refreshPermissions() {
        val isMiui = PermissionHelper.isMiui()
        val currentUsageStatsEnabled = PermissionHelper.hasUsageStatsPermission(context)

        _permissionState.value = PermissionState(
            accessibilityEnabled = PermissionHelper.isAccessibilityEnabled(context),
            overlayEnabled = PermissionHelper.canDrawOverlays(context),
            batteryOptimizationDisabled = PermissionHelper.isIgnoringBatteryOptimizations(context),
            usageStatsEnabled = currentUsageStatsEnabled,
            isMiui = isMiui,
            // 使用 MiuiHelper 检测实际权限状态
            miuiBackgroundPopupGranted = if (isMiui) MiuiHelper.canBackgroundStart(context) else true,
            miuiAutoStartConfirmed = miuiAutoStartConfirmed.value,
            miuiBackgroundPopupConfirmed = miuiBackgroundPopupConfirmed.value,
            miuiBatterySaverConfirmed = miuiBatterySaverConfirmed.value,
            miuiLockAppConfirmed = miuiLockAppConfirmed.value
        )

        // 检测使用统计权限从关闭变为开启，触发使用时长同步
        if (currentUsageStatsEnabled && !previousUsageStatsEnabled) {
            viewModelScope.launch {
                repository.syncAllMonitoredAppsUsage()
            }
        }
        previousUsageStatsEnabled = currentUsageStatsEnabled
    }

    fun setDefaultCountdown(seconds: Int) {
        viewModelScope.launch {
            repository.setDefaultCountdown(seconds)
            repository.updateAllCountdownSeconds(seconds)  // 同步更新所有已监控应用
        }
    }

    fun setCooldownMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.setCooldownMinutes(minutes)
        }
    }

    fun setCustomReminderTexts(texts: String) {
        viewModelScope.launch {
            repository.setCustomReminderTexts(texts)
        }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val current = currentLanguage.value
            val newLang = if (current == "en") "zh" else "en"
            repository.setAppLanguage(newLang)
        }
    }

    fun openAccessibilitySettings() = PermissionHelper.openAccessibilitySettings(context)
    fun openOverlaySettings() = PermissionHelper.openOverlaySettings(context)
    fun openBatterySettings() = PermissionHelper.openBatteryOptimizationSettings(context)
    fun openUsageStatsSettings() = PermissionHelper.openUsageStatsSettings(context)
    fun openMiuiAutoStartSettings() = PermissionHelper.openMiuiAutoStartSettings(context)
    fun openMiuiBackgroundPopupSettings() = PermissionHelper.openMiuiBackgroundPopupSettings(context)
    fun openMiuiBatterySettings() = PermissionHelper.openMiuiBatterySettings(context)

    fun confirmMiuiAutoStart() {
        viewModelScope.launch {
            repository.setMiuiAutoStartConfirmed(true)
        }
    }

    fun resetMiuiAutoStart() {
        viewModelScope.launch {
            repository.setMiuiAutoStartConfirmed(false)
        }
    }

    fun confirmMiuiBackgroundPopup() {
        viewModelScope.launch {
            repository.setMiuiBackgroundPopupConfirmed(true)
        }
    }

    fun confirmMiuiBatterySaver() {
        viewModelScope.launch {
            repository.setMiuiBatterySaverConfirmed(true)
        }
    }

    fun resetMiuiBatterySaver() {
        viewModelScope.launch {
            repository.setMiuiBatterySaverConfirmed(false)
        }
    }

    fun confirmMiuiLockApp() {
        viewModelScope.launch {
            repository.setMiuiLockAppConfirmed(true)
        }
    }

    fun resetMiuiLockApp() {
        viewModelScope.launch {
            repository.setMiuiLockAppConfirmed(false)
        }
    }

    class Factory(
        private val repository: SlowDownRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository, context) as T
        }
    }
}
