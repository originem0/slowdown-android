package com.example.slowdown.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.slowdown.data.repository.SlowDownRepository
import com.example.slowdown.util.PermissionHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PermissionState(
    val accessibilityEnabled: Boolean = false,
    val overlayEnabled: Boolean = false,
    val batteryOptimizationDisabled: Boolean = false,
    val isMiui: Boolean = false
)

class SettingsViewModel(
    private val repository: SlowDownRepository,
    private val context: Context
) : ViewModel() {

    val defaultCountdown: StateFlow<Int> = repository.defaultCountdown
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val cooldownMinutes: StateFlow<Int> = repository.cooldownMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        _permissionState.value = PermissionState(
            accessibilityEnabled = PermissionHelper.isAccessibilityEnabled(context),
            overlayEnabled = PermissionHelper.canDrawOverlays(context),
            batteryOptimizationDisabled = PermissionHelper.isIgnoringBatteryOptimizations(context),
            isMiui = PermissionHelper.isMiui()
        )
    }

    fun setDefaultCountdown(seconds: Int) {
        viewModelScope.launch {
            repository.setDefaultCountdown(seconds)
        }
    }

    fun setCooldownMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.setCooldownMinutes(minutes)
        }
    }

    fun openAccessibilitySettings() = PermissionHelper.openAccessibilitySettings(context)
    fun openOverlaySettings() = PermissionHelper.openOverlaySettings(context)
    fun openBatterySettings() = PermissionHelper.openBatteryOptimizationSettings(context)
    fun openMiuiAutoStartSettings() = PermissionHelper.openMiuiAutoStartSettings(context)
    fun openMiuiBackgroundPopupSettings() = PermissionHelper.openMiuiBackgroundPopupSettings(context)

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
