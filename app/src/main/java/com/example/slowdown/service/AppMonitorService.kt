package com.example.slowdown.service

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.ServiceCompat
import com.example.slowdown.SlowDownApp
import com.example.slowdown.ui.overlay.OverlayActivity
import com.example.slowdown.util.MiuiHelper
import com.example.slowdown.util.NotificationHelper
import com.example.slowdown.util.PackageUtils
import com.example.slowdown.util.PermissionHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AppMonitorService : AccessibilityService() {

    companion object {
        private const val TAG = "SlowDown"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val cooldownMap = mutableMapOf<String, Long>()

    // 使用时间追踪
    private lateinit var usageTrackingManager: UsageTrackingManager
    private var currentForegroundApp: String? = null
    private var foregroundStartTime: Long = 0
    private val usageWarningCooldownMap = mutableMapOf<String, Long>()
    private val usageWarningCooldownMs = 10 * 60 * 1000L  // 10 分钟内不重复提醒同一警告

    private val repository by lazy {
        (application as SlowDownApp).repository
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "[Service] ===== AccessibilityService CONNECTED =====")
        Log.d(TAG, "[Service] Service info: ${serviceInfo?.let { "eventTypes=${it.eventTypes}, feedbackType=${it.feedbackType}, flags=${it.flags}" } ?: "null"}")

        // 初始化使用时间追踪管理器
        usageTrackingManager = UsageTrackingManager(this, repository)
        usageTrackingManager.startPeriodicSync()
        Log.d(TAG, "[Service] UsageTrackingManager initialized and periodic sync started")

        // 启动前台服务通知 - 防止 MIUI 冻结 AccessibilityService
        startForegroundNotification()
    }

    /**
     * 启动前台服务通知
     * 这是解决 MIUI 冻结 AccessibilityService 的关键：
     * 当应用有前台服务通知时，系统不会冻结其进程
     */
    private fun startForegroundNotification() {
        try {
            val notification = NotificationHelper.buildForegroundNotification(this)

            // Android 14+ 需要指定前台服务类型
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceCompat.startForeground(
                    this,
                    NotificationHelper.FOREGROUND_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NotificationHelper.FOREGROUND_NOTIFICATION_ID, notification)
            }

            Log.d(TAG, "[Service] Foreground notification started - service should not be frozen now")
        } catch (e: Exception) {
            Log.e(TAG, "[Service] Failed to start foreground notification: ${e.message}")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 最早的日志点 - 确认事件是否到达
        val eventType = event?.eventType
        val pkg = event?.packageName?.toString()
        Log.d(TAG, "[Service] onAccessibilityEvent: type=$eventType, pkg=$pkg")

        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Skip our own package
        if (packageName == this.packageName) return

        // Skip system critical apps
        if (PackageUtils.isSystemCriticalApp(packageName)) return

        // 验证：只有当目标应用确实在前台时才处理
        // 使用 rootInActiveWindow 获取当前活动窗口的包名
        val activeWindowPackage = try {
            rootInActiveWindow?.packageName?.toString()
        } catch (e: Exception) {
            null
        }

        if (activeWindowPackage != null && activeWindowPackage != packageName) {
            Log.d(TAG, "[Service] $packageName event received but active window is $activeWindowPackage, skip")
            return
        }

        // 实时追踪：处理应用切换
        handleRealtimeTracking(packageName)

        serviceScope.launch {
            handleAppLaunch(packageName)
        }
    }

    /**
     * 处理实时使用时间追踪
     * 当应用切换时，记录上一个应用的使用时间
     */
    private fun handleRealtimeTracking(newPackageName: String) {
        if (!::usageTrackingManager.isInitialized) return

        // 如果是同一个应用，不需要处理
        if (newPackageName == currentForegroundApp) return

        // 记录上一个应用的使用时间
        if (usageTrackingManager.isRealtimeTrackingEnabled() && currentForegroundApp != null && foregroundStartTime > 0) {
            val duration = System.currentTimeMillis() - foregroundStartTime
            if (duration > 0) {
                usageTrackingManager.recordForegroundTime(currentForegroundApp!!, duration)
                Log.d(TAG, "[UsageTracking] Recorded ${duration}ms for $currentForegroundApp")
            }
        }

        // 更新当前前台应用
        currentForegroundApp = newPackageName
        foregroundStartTime = System.currentTimeMillis()

        // 检查新应用是否需要启动实时追踪
        serviceScope.launch {
            try {
                if (usageTrackingManager.checkRealtimeTrackingNeeded(newPackageName)) {
                    if (!usageTrackingManager.isRealtimeTrackingEnabled()) {
                        usageTrackingManager.startRealtimeTracking(newPackageName)
                        Log.d(TAG, "[UsageTracking] Started realtime tracking for $newPackageName")
                    }
                }

                // 检查使用时间警告
                checkAndShowUsageWarning(newPackageName)
            } catch (e: Exception) {
                Log.e(TAG, "[UsageTracking] Error in realtime tracking: ${e.message}")
            }
        }
    }

    /**
     * 检查并显示使用时间警告
     */
    private suspend fun checkAndShowUsageWarning(packageName: String) {
        if (!::usageTrackingManager.isInitialized) return

        val warningType = usageTrackingManager.checkUsageWarning(packageName) ?: return

        // 检查冷却时间，避免频繁提醒
        val warningKey = "${packageName}_${warningType.name}"
        val lastWarningTime = usageWarningCooldownMap[warningKey] ?: 0
        val elapsed = System.currentTimeMillis() - lastWarningTime
        if (elapsed < usageWarningCooldownMs) {
            Log.d(TAG, "[UsageWarning] $warningKey in cooldown, skip")
            return
        }

        // 更新冷却时间
        usageWarningCooldownMap[warningKey] = System.currentTimeMillis()

        val monitoredApp = repository.getMonitoredApp(packageName) ?: return

        Log.d(TAG, "[UsageWarning] Showing warning for $packageName: $warningType")

        when (warningType) {
            UsageWarningType.WARNING_80_PERCENT -> {
                // 80% 警告：显示通知提醒
                NotificationHelper.showUsageWarningNotification(
                    context = this,
                    packageName = packageName,
                    appName = monitoredApp.appName,
                    warningType = warningType
                )
            }
            UsageWarningType.LIMIT_REACHED_SOFT -> {
                // 100% 软提醒：显示干预界面
                showLimitReachedIntervention(packageName, monitoredApp.appName, monitoredApp.redirectPackage, false)
            }
            UsageWarningType.LIMIT_REACHED_STRICT -> {
                // 100% 强制关闭：显示干预界面并返回桌面
                showLimitReachedIntervention(packageName, monitoredApp.appName, monitoredApp.redirectPackage, true)
            }
        }
    }

    /**
     * 显示达到限额的干预界面
     */
    private fun showLimitReachedIntervention(
        packageName: String,
        appName: String,
        redirectPackage: String?,
        forceClose: Boolean
    ) {
        if (forceClose) {
            // 强制模式：直接返回桌面
            performGlobalAction(GLOBAL_ACTION_HOME)
            Log.d(TAG, "[UsageWarning] Force closed app, returned to home")
        }

        // 显示提醒通知
        NotificationHelper.showUsageWarningNotification(
            context = this,
            packageName = packageName,
            appName = appName,
            warningType = if (forceClose) UsageWarningType.LIMIT_REACHED_STRICT else UsageWarningType.LIMIT_REACHED_SOFT
        )
    }

    private suspend fun handleAppLaunch(packageName: String) {
        Log.d(TAG, "[Service] handleAppLaunch: $packageName")

        // Check if service is enabled
        val serviceEnabled = repository.serviceEnabled.first()
        if (!serviceEnabled) {
            Log.d(TAG, "[Service] serviceEnabled=false, skip")
            return
        }

        // Check if app is monitored
        val monitoredApp = repository.getMonitoredApp(packageName)
        if (monitoredApp == null) {
            Log.d(TAG, "[Service] $packageName not in monitored list, skip")
            return
        }
        if (!monitoredApp.isEnabled) {
            Log.d(TAG, "[Service] $packageName isEnabled=false, skip")
            return
        }

        // 只检查：如果当前前台是 SlowDown 自己，则跳过（避免在自己应用上显示悬浮窗）
        val currentForegroundPackage = try {
            rootInActiveWindow?.packageName?.toString()
        } catch (e: Exception) {
            null
        }
        Log.d(TAG, "[Service] Current foreground: $currentForegroundPackage, target: $packageName")

        if (currentForegroundPackage == this.packageName) {
            Log.d(TAG, "[Service] SlowDown is in foreground, skip intervention to avoid overlay on self")
            return
        }

        // Check cooldown
        val cooldownMinutes = repository.cooldownMinutes.first()
        val lastIntervention = cooldownMap[packageName] ?: 0
        val cooldownMs = cooldownMinutes * 60 * 1000L
        val elapsed = System.currentTimeMillis() - lastIntervention
        if (elapsed < cooldownMs) {
            Log.d(TAG, "[Service] $packageName in cooldown (${elapsed/1000}s < ${cooldownMs/1000}s), skip")
            return
        }

        // Update cooldown
        cooldownMap[packageName] = System.currentTimeMillis()

        // 使用全局默认倒计时
        val defaultCountdown = repository.defaultCountdown.first()

        Log.d(TAG, "[Service] Triggering intervention for $packageName, countdown=${defaultCountdown}s")

        // MIUI 特定策略：先尝试直接启动 Activity（配合 moveTaskToFront）
        // 这比 Full-Screen Intent 更可靠，因为它绕过了"后台弹出界面"限制
        if (PermissionHelper.isMiui()) {
            Log.d(TAG, "[Service] MIUI detected, using direct launch strategy")
            launchOverlayDirectly(packageName, monitoredApp.appName, defaultCountdown, monitoredApp.redirectPackage)
        } else {
            // 非 MIUI 设备：使用标准的 Full-Screen Intent
            Log.d(TAG, "[Service] Non-MIUI device, using Full-Screen Intent")
            NotificationHelper.showInterventionNotification(
                context = this,
                packageName = packageName,
                appName = monitoredApp.appName,
                countdownSeconds = defaultCountdown,
                redirectPackage = monitoredApp.redirectPackage
            )
        }

        // WindowManager 悬浮窗作为备用（所有设备）
        OverlayService.start(
            context = this,
            packageName = packageName,
            appName = monitoredApp.appName,
            countdownSeconds = defaultCountdown,
            redirectPackage = monitoredApp.redirectPackage
        )
        Log.d(TAG, "[Service] OverlayService.start() called as backup")
    }

    /**
     * MIUI 特定：直接启动 OverlayActivity 并强制移到前台
     *
     * 策略：
     * 1. 先启动 Activity
     * 2. 使用 ActivityManager.moveTaskToFront() 强制将任务移到前台
     * 3. 延迟一点时间后再次 moveTaskToFront，确保显示
     */
    private fun launchOverlayDirectly(
        packageName: String,
        appName: String,
        countdownSeconds: Int,
        redirectPackage: String?
    ) {
        try {
            // 构建 Intent - 使用更激进的 flags
            val intent = Intent(this, OverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                putExtra(OverlayActivity.EXTRA_PACKAGE_NAME, packageName)
                putExtra(OverlayActivity.EXTRA_APP_NAME, appName)
                putExtra(OverlayActivity.EXTRA_COUNTDOWN_SECONDS, countdownSeconds)
                putExtra(OverlayActivity.EXTRA_REDIRECT_PACKAGE, redirectPackage)
            }

            // 尝试设置 MIUI 特定标志位
            val miuiFlagsSet = MiuiHelper.addMiuiFlags(intent)
            Log.d(TAG, "[Service] MiuiFlags set result: $miuiFlagsSet")

            // 启动 Activity
            startActivity(intent)
            Log.d(TAG, "[Service] OverlayActivity launched")

            // 关键：使用 moveTaskToFront 强制将任务移到前台
            val handler = Handler(Looper.getMainLooper())

            // 立即尝试移到前台
            handler.post { moveSlowDownToFront() }

            // 100ms 后再次尝试（确保 Activity 已创建）
            handler.postDelayed({ moveSlowDownToFront() }, 100)

            // 300ms 后再次尝试（最后保障）
            handler.postDelayed({ moveSlowDownToFront() }, 300)

        } catch (e: Exception) {
            Log.e(TAG, "[Service] Direct launch failed: ${e.message}, falling back to notification")
            // 失败时回退到 Full-Screen Intent
            NotificationHelper.showInterventionNotification(
                context = this,
                packageName = packageName,
                appName = appName,
                countdownSeconds = countdownSeconds,
                redirectPackage = redirectPackage
            )
        }
    }

    /**
     * 使用 ActivityManager.moveTaskToFront() 将 SlowDown 任务移到前台
     */
    private fun moveSlowDownToFront() {
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

            // 获取所有任务，找到 SlowDown 的任务
            @Suppress("DEPRECATION")
            val tasks = activityManager.getRunningTasks(10)

            for (task in tasks) {
                if (task.baseActivity?.packageName == this.packageName ||
                    task.topActivity?.packageName == this.packageName) {

                    Log.d(TAG, "[Service] Found SlowDown task: ${task.id}, moving to front")

                    @Suppress("DEPRECATION")
                    activityManager.moveTaskToFront(task.id, ActivityManager.MOVE_TASK_WITH_HOME)
                    return
                }
            }

            Log.d(TAG, "[Service] SlowDown task not found in running tasks")
        } catch (e: Exception) {
            Log.e(TAG, "[Service] moveTaskToFront failed: ${e.message}")
        }
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止使用时间追踪
        if (::usageTrackingManager.isInitialized) {
            usageTrackingManager.stopRealtimeTracking()
            usageTrackingManager.stopPeriodicSync()
            Log.d(TAG, "[Service] UsageTrackingManager stopped")
        }
        serviceScope.cancel()
    }
}
