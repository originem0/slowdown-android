package com.example.slowdown.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.slowdown.data.local.entity.MonitoredApp
import com.example.slowdown.data.repository.SlowDownRepository
import com.example.slowdown.util.AppInfo
import com.example.slowdown.util.PackageUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppListViewModel(
    private val repository: SlowDownRepository,
    private val context: Context
) : ViewModel() {

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
            _installedApps.value = PackageUtils.getInstalledApps(context)
            _isLoading.value = false
        }
    }

    private fun observeUsage() {
        viewModelScope.launch {
            monitoredApps.collect { apps ->
                val usageFlows = apps.map { app ->
                    repository.getTodayUsage(app.packageName).map { usage ->
                        app.packageName to usage
                    }
                }

                if (usageFlows.isNotEmpty()) {
                    combine(usageFlows) { usageArray ->
                        usageArray.toMap()
                    }.collect { usageData ->
                        _usageMap.value = usageData
                    }
                }
            }
        }
    }

    fun toggleApp(appInfo: AppInfo, isMonitored: Boolean) {
        viewModelScope.launch {
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
            }
        }
    }

    fun updateAppConfig(app: MonitoredApp) {
        viewModelScope.launch {
            repository.updateMonitoredApp(app)
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
