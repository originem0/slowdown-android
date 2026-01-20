package com.sharonZ.slowdown.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sharonZ.slowdown.R
import com.sharonZ.slowdown.data.local.entity.MonitoredApp
import com.sharonZ.slowdown.data.preferences.UserPreferences
import com.sharonZ.slowdown.data.repository.SlowDownRepository
import com.sharonZ.slowdown.ui.onboarding.AppInfo
import com.sharonZ.slowdown.util.MiuiHelper
import com.sharonZ.slowdown.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the Onboarding flow
 */
class OnboardingViewModel(
    private val repository: SlowDownRepository,
    private val context: Context
) : ViewModel() {

    // ========================================
    // State
    // ========================================
    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _permissionItems = MutableStateFlow<List<PermissionItem>>(emptyList())
    val permissionItems: StateFlow<List<PermissionItem>> = _permissionItems.asStateFlow()

    private val _availableApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val availableApps: StateFlow<List<AppInfo>> = _availableApps.asStateFlow()

    private val _selectedApps = MutableStateFlow<Set<String>>(emptySet())
    val selectedApps: StateFlow<Set<String>> = _selectedApps.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val userPreferences = UserPreferences(context)

    // ========================================
    // Initialization
    // ========================================
    init {
        refreshPermissions()
        loadAvailableApps()
        preSelectSuggestedApps()
    }

    // ========================================
    // Navigation
    // ========================================
    fun nextStep() {
        if (_currentStep.value < 3) {
            _currentStep.value++
        }
    }

    fun previousStep() {
        if (_currentStep.value > 0) {
            _currentStep.value--
        }
    }

    // ========================================
    // Permissions
    // ========================================
    fun refreshPermissions() {
        val isMiui = PermissionHelper.isMiui()
        val items = mutableListOf<PermissionItem>()

        // 必要权限 - CRITICAL
        items.add(PermissionItem(
            id = "accessibility",
            titleResId = R.string.accessibility_service,
            descriptionResId = R.string.accessibility_subtitle,
            icon = Icons.Outlined.Accessibility,
            isGranted = PermissionHelper.isAccessibilityEnabled(context),
            intentAction = Settings.ACTION_ACCESSIBILITY_SETTINGS,
            priority = PermissionPriority.CRITICAL
        ))

        items.add(PermissionItem(
            id = "overlay",
            titleResId = R.string.overlay_permission,
            descriptionResId = R.string.overlay_subtitle,
            icon = Icons.Outlined.Layers,
            isGranted = Settings.canDrawOverlays(context),
            intentAction = Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            intentData = "package:${context.packageName}",
            priority = PermissionPriority.CRITICAL
        ))

        items.add(PermissionItem(
            id = "usage_stats",
            titleResId = R.string.usage_stats,
            descriptionResId = R.string.usage_subtitle,
            icon = Icons.Outlined.QueryStats,
            isGranted = PermissionHelper.hasUsageStatsPermission(context),
            intentAction = Settings.ACTION_USAGE_ACCESS_SETTINGS,
            priority = PermissionPriority.CRITICAL
        ))

        // MIUI 后台弹窗权限 - 仅 MIUI 设备显示，属于必要权限
        if (isMiui) {
            items.add(PermissionItem(
                id = "miui_background_popup",
                titleResId = R.string.background_popup,
                descriptionResId = R.string.background_popup_subtitle,
                icon = Icons.Outlined.OpenInNew,
                isGranted = MiuiHelper.canBackgroundStart(context),
                intentAction = "miui_app_settings",  // 特殊标识，在 requestPermission 中处理
                priority = PermissionPriority.CRITICAL
            ))
        }

        // 建议权限 - OPTIONAL
        items.add(PermissionItem(
            id = "battery",
            titleResId = R.string.battery_optimization,
            descriptionResId = R.string.battery_subtitle,
            icon = Icons.Outlined.BatteryChargingFull,
            isGranted = PermissionHelper.isIgnoringBatteryOptimizations(context),
            intentAction = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            intentData = "package:${context.packageName}",
            priority = PermissionPriority.OPTIONAL
        ))

        _permissionItems.value = items
    }

    fun requestPermission(item: PermissionItem) {
        // 处理 MIUI 特殊权限
        if (item.intentAction == "miui_app_settings") {
            PermissionHelper.openMiuiAppSettings(context)
            return
        }

        val intent = Intent(item.intentAction).apply {
            if (item.intentData != null) {
                data = Uri.parse(item.intentData)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to generic settings
            val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        }
    }

    // ========================================
    // App Selection
    // ========================================
    private fun loadAvailableApps() {
        viewModelScope.launch {
            _isLoading.value = true

            val apps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

                // Filter to user apps (excluding system apps)
                installedApps
                    .filter { appInfo ->
                        // User-installed apps
                        (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                        // Not this app
                        appInfo.packageName != context.packageName &&
                        // Has a launcher activity
                        pm.getLaunchIntentForPackage(appInfo.packageName) != null
                    }
                    .map { appInfo ->
                        AppInfo(
                            packageName = appInfo.packageName,
                            appName = pm.getApplicationLabel(appInfo).toString()
                        )
                    }
                    .sortedBy { it.appName.lowercase() }
            }

            _availableApps.value = apps
            _isLoading.value = false
        }
    }

    private fun preSelectSuggestedApps() {
        // Pre-select common distraction apps if installed
        val suggestedPackages = setOf(
            "com.zhiliaoapp.musically", // TikTok (international)
            "com.ss.android.ugc.aweme", // Douyin (China)
            "com.instagram.android",
            "com.twitter.android",
            "com.facebook.katana",
            "com.google.android.youtube"
        )

        viewModelScope.launch {
            // Wait for apps to load
            while (_availableApps.value.isEmpty() && _isLoading.value) {
                kotlinx.coroutines.delay(100)
            }

            val installedSuggested = _availableApps.value
                .filter { it.packageName in suggestedPackages }
                .map { it.packageName }
                .toSet()

            _selectedApps.value = installedSuggested
        }
    }

    fun toggleAppSelection(packageName: String) {
        val current = _selectedApps.value.toMutableSet()
        if (packageName in current) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _selectedApps.value = current
    }

    fun saveSelectedApps() {
        viewModelScope.launch {
            val pm = context.packageManager

            _selectedApps.value.forEach { packageName ->
                try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()

                    val monitoredApp = MonitoredApp(
                        packageName = packageName,
                        appName = appName,
                        interventionType = "breathing", // Default intervention type
                        countdownSeconds = userPreferences.defaultCountdown.first(),
                        redirectPackage = null,
                        isEnabled = true,
                        dailyLimitMinutes = null, // No limit by default in gentle mode
                        limitMode = "soft" // Gentle mode
                    )

                    repository.addMonitoredApp(monitoredApp)
                } catch (e: Exception) {
                    // App might have been uninstalled, skip
                }
            }
        }
    }

    // ========================================
    // Completion
    // ========================================
    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferences.setOnboardingCompleted(true)
        }
    }

    // ========================================
    // Factory
    // ========================================
    class Factory(
        private val repository: SlowDownRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
                return OnboardingViewModel(repository, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

/**
 * Permission priority levels
 * - CRITICAL: Core functionality depends on this (Accessibility, Overlay, Usage Stats, MIUI Background Popup)
 * - OPTIONAL: Nice to have, but app works without it (Battery Optimization)
 */
enum class PermissionPriority {
    CRITICAL,
    OPTIONAL
}

/**
 * Data class representing a permission item in the onboarding flow
 * Uses resource IDs for localization support
 */
data class PermissionItem(
    val id: String,
    val titleResId: Int,
    val descriptionResId: Int,
    val icon: ImageVector,
    val isGranted: Boolean,
    val intentAction: String,
    val intentData: String? = null,
    val priority: PermissionPriority = PermissionPriority.CRITICAL
)
