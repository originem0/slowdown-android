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

class AppDetailViewModel(
    private val repository: SlowDownRepository,
    private val context: Context,
    private val packageName: String
) : ViewModel() {

    private val _monitoredApp = MutableStateFlow<MonitoredApp?>(null)
    val monitoredApp: StateFlow<MonitoredApp?> = _monitoredApp.asStateFlow()

    private val _appInfo = MutableStateFlow<AppInfo?>(null)
    val appInfo: StateFlow<AppInfo?> = _appInfo.asStateFlow()

    val todayUsage: StateFlow<Int> = repository.getTodayUsage(packageName)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentUsage: StateFlow<List<UsageRecord>> = repository.getRecentUsage(packageName, 7)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    init {
        loadAppData()
        loadInstalledApps()
    }

    private fun loadAppData() {
        viewModelScope.launch {
            // Observe changes to the monitored app
            repository.monitoredApps
                .map { apps -> apps.find { it.packageName == packageName } }
                .collect { app ->
                    _monitoredApp.value = app
                }
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = PackageUtils.getInstalledApps(context)
            _installedApps.value = apps
            _appInfo.value = apps.find { it.packageName == packageName }
        }
    }

    fun updateRedirectApp(redirectPackage: String?) {
        viewModelScope.launch {
            _monitoredApp.value?.let { app ->
                val updated = app.copy(redirectPackage = redirectPackage)
                repository.updateMonitoredApp(updated)
            }
        }
    }

    fun updateDailyLimit(minutes: Int?) {
        viewModelScope.launch {
            _monitoredApp.value?.let { app ->
                val updated = app.copy(dailyLimitMinutes = minutes)
                repository.updateMonitoredApp(updated)
            }
        }
    }

    fun updateLimitMode(mode: String) {
        viewModelScope.launch {
            _monitoredApp.value?.let { app ->
                val updated = app.copy(limitMode = mode)
                repository.updateMonitoredApp(updated)
            }
        }
    }

    /**
     * 设置完全禁止模式（无限制+强制关闭）
     * 选择此模式后，打开应用就会被阻止
     */
    fun setCompletelyBlocked() {
        viewModelScope.launch {
            _monitoredApp.value?.let { app ->
                val updated = app.copy(
                    dailyLimitMinutes = null,
                    limitMode = "strict"
                )
                repository.updateMonitoredApp(updated)
            }
        }
    }

    /**
     * 检查当前是否为完全禁止模式
     */
    fun isCompletelyBlocked(): Boolean {
        val app = _monitoredApp.value ?: return false
        return app.dailyLimitMinutes == null && app.limitMode == "strict"
    }

    fun updateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _monitoredApp.value?.let { app ->
                val updated = app.copy(isEnabled = enabled)
                repository.updateMonitoredApp(updated)
            }
        }
    }

    /**
     * 原子更新限制模式 - 一次性设置所有相关字段
     * 避免多次独立更新导致的状态竞争问题
     */
    fun updateRestrictionMode(isEnabled: Boolean, limitMode: String, dailyLimitMinutes: Int?) {
        viewModelScope.launch {
            _monitoredApp.value?.let { app ->
                val updated = app.copy(
                    isEnabled = isEnabled,
                    limitMode = limitMode,
                    dailyLimitMinutes = dailyLimitMinutes
                )
                repository.updateMonitoredApp(updated)
            }
        }
    }

    /**
     * 更新视频应用模式
     * 视频应用使用定时器主动触发弹窗检查，而非依赖 Activity 切换事件
     */
    fun updateVideoAppMode(isVideoApp: Boolean) {
        viewModelScope.launch {
            _monitoredApp.value?.let { app ->
                val updated = app.copy(isVideoApp = isVideoApp)
                repository.updateMonitoredApp(updated)
            }
        }
    }

    // Calculate weekly average usage
    fun getWeeklyAverage(): Int {
        val records = recentUsage.value
        if (records.isEmpty()) return 0
        return records.sumOf { it.usageMinutes } / records.size
    }

    class Factory(
        private val repository: SlowDownRepository,
        private val context: Context,
        private val packageName: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppDetailViewModel(repository, context, packageName) as T
        }
    }
}
