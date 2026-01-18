package com.example.slowdown.viewmodel

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
import com.example.slowdown.data.local.entity.MonitoredApp
import com.example.slowdown.data.preferences.UserPreferences
import com.example.slowdown.data.repository.SlowDownRepository
import com.example.slowdown.ui.onboarding.AppInfo
import com.example.slowdown.util.PermissionHelper
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
        _permissionItems.value = listOf(
            PermissionItem(
                id = "accessibility",
                title = "Accessibility Service",
                description = "Monitor app launches for interventions",
                icon = Icons.Outlined.Accessibility,
                isGranted = PermissionHelper.isAccessibilityEnabled(context),
                intentAction = Settings.ACTION_ACCESSIBILITY_SETTINGS,
                priority = PermissionPriority.CRITICAL
            ),
            PermissionItem(
                id = "overlay",
                title = "Display Over Other Apps",
                description = "Show breathing pause overlay",
                icon = Icons.Outlined.Layers,
                isGranted = Settings.canDrawOverlays(context),
                intentAction = Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                intentData = "package:${context.packageName}",
                priority = PermissionPriority.CRITICAL
            ),
            PermissionItem(
                id = "usage_stats",
                title = "Usage Access",
                description = "Track app usage time",
                icon = Icons.Outlined.QueryStats,
                isGranted = PermissionHelper.hasUsageStatsPermission(context),
                intentAction = Settings.ACTION_USAGE_ACCESS_SETTINGS,
                priority = PermissionPriority.IMPORTANT
            ),
            PermissionItem(
                id = "battery",
                title = "Ignore Battery Optimization",
                description = "Keep service running in background",
                icon = Icons.Outlined.BatteryChargingFull,
                isGranted = PermissionHelper.isIgnoringBatteryOptimizations(context),
                intentAction = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                intentData = "package:${context.packageName}",
                priority = PermissionPriority.OPTIONAL
            )
        )
    }

    fun requestPermission(item: PermissionItem) {
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
 * - CRITICAL: Core functionality depends on this (Accessibility, Overlay)
 * - IMPORTANT: Significant features depend on this (Usage Stats)
 * - OPTIONAL: Nice to have, but app works without it (Battery Optimization)
 */
enum class PermissionPriority {
    CRITICAL,
    IMPORTANT,
    OPTIONAL
}

/**
 * Data class representing a permission item in the onboarding flow
 */
data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isGranted: Boolean,
    val intentAction: String,
    val intentData: String? = null,
    val priority: PermissionPriority = PermissionPriority.IMPORTANT
)
