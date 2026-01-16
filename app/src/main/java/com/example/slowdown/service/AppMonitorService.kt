package com.example.slowdown.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.slowdown.SlowDownApp
import com.example.slowdown.ui.overlay.OverlayActivity
import com.example.slowdown.util.PackageUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AppMonitorService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val cooldownMap = mutableMapOf<String, Long>()

    private val repository by lazy {
        (application as SlowDownApp).repository
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Skip our own package
        if (packageName == this.packageName) return

        // Skip system critical apps
        if (PackageUtils.isSystemCriticalApp(packageName)) return

        serviceScope.launch {
            handleAppLaunch(packageName)
        }
    }

    private suspend fun handleAppLaunch(packageName: String) {
        // Check if service is enabled
        val serviceEnabled = repository.serviceEnabled.first()
        if (!serviceEnabled) return

        // Check if app is monitored
        val monitoredApp = repository.getMonitoredApp(packageName) ?: return
        if (!monitoredApp.isEnabled) return

        // Check cooldown
        val cooldownMinutes = repository.cooldownMinutes.first()
        val lastIntervention = cooldownMap[packageName] ?: 0
        val cooldownMs = cooldownMinutes * 60 * 1000L
        if (System.currentTimeMillis() - lastIntervention < cooldownMs) return

        // Update cooldown
        cooldownMap[packageName] = System.currentTimeMillis()

        // Launch overlay
        val intent = Intent(this, OverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(OverlayActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(OverlayActivity.EXTRA_APP_NAME, monitoredApp.appName)
            putExtra(OverlayActivity.EXTRA_COUNTDOWN_SECONDS, monitoredApp.countdownSeconds)
            putExtra(OverlayActivity.EXTRA_REDIRECT_PACKAGE, monitoredApp.redirectPackage)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
