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

    private val repository by lazy {
        (application as SlowDownApp).repository
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "[Service] ===== AccessibilityService CONNECTED =====")
        Log.d(TAG, "[Service] Service info: ${serviceInfo?.let { "eventTypes=${it.eventTypes}, feedbackType=${it.feedbackType}, flags=${it.flags}" } ?: "null"}")

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

        serviceScope.launch {
            handleAppLaunch(packageName)
        }
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
        serviceScope.cancel()
    }
}
