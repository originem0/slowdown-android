package com.sharonZ.slowdown.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sharonZ.slowdown.data.local.entity.MonitoredApp
import com.sharonZ.slowdown.data.repository.SlowDownRepository
import com.sharonZ.slowdown.util.AppInfo
import com.sharonZ.slowdown.util.PackageUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class AppListViewModel(
    private val repository: SlowDownRepository,
    private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "AppListViewModel"
    }

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    val monitoredApps: StateFlow<List<MonitoredApp>> = repository.monitoredApps
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val defaultCountdown: StateFlow<Int> = repository.defaultCountdown
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Map of package name to today's usage in minutes
    private val _usageMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val usageMap: StateFlow<Map<String, Int>> = _usageMap.asStateFlow()

    init {
        loadInstalledApps()
        observeUsage()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val apps = withTimeoutOrNull(15_000L) { // 15 秒超时
                    PackageUtils.getInstalledApps(context)
                }
                if (apps != null) {
                    _installedApps.value = apps
                } else {
                    Log.w(TAG, "Loading installed apps timed out after 15 seconds")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load installed apps: ${e.message}", e)
            }
            _isLoading.value = false
        }
    }

    private fun observeUsage() {
        viewModelScope.launch {
            // 使用 collectLatest 而非 collect：当 monitoredApps 变化时，
            // 会自动取消之前的内部 collect 协程，防止内存泄漏
            monitoredApps.collectLatest { apps ->
                if (apps.isEmpty()) {
                    _usageMap.value = emptyMap()
                    return@collectLatest
                }

                val usageFlows = apps.map { app ->
                    repository.getTodayUsage(app.packageName).map { usage ->
                        app.packageName to usage
                    }
                }

                combine(usageFlows) { usageArray ->
                    usageArray.toMap()
                }.collect { usageData ->
                    _usageMap.value = usageData
                }
            }
        }
    }

    fun toggleApp(appInfo: AppInfo, isMonitored: Boolean) {
        viewModelScope.launch {
            try {
                if (isMonitored) {
                    repository.removeMonitoredApp(appInfo.packageName)
                } else {
                    val countdown = defaultCountdown.value
                    repository.addMonitoredApp(
                        MonitoredApp(
                            packageName = appInfo.packageName,
                            appName = appInfo.appName,
                            countdownSeconds = countdown
                        )
                    )
                    // 添加后立即同步该应用的使用时间
                    repository.syncAppUsage(appInfo.packageName)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle app ${appInfo.packageName}: ${e.message}", e)
            }
        }
    }

    fun updateAppConfig(app: MonitoredApp) {
        viewModelScope.launch {
            try {
                repository.updateMonitoredApp(app)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update app config ${app.packageName}: ${e.message}", e)
            }
        }
    }

    class Factory(
        private val repository: SlowDownRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppListViewModel(repository, context) as T
        }
    }
}
