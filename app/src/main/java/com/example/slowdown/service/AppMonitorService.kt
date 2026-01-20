package com.sharonZ.slowdown.service

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
import com.sharonZ.slowdown.SlowDownApp
import com.sharonZ.slowdown.ui.overlay.OverlayActivity
import com.sharonZ.slowdown.ui.warning.UsageWarningActivity
import com.sharonZ.slowdown.util.MiuiHelper
import com.sharonZ.slowdown.util.NotificationHelper
import com.sharonZ.slowdown.util.PackageUtils
import com.sharonZ.slowdown.util.PermissionHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AppMonitorService : AccessibilityService() {

    companion object {
        private const val TAG = "SlowDown"
        private const val VIDEO_APP_CHECK_INTERVAL_MS = 30_000L  // 视频应用检查间隔：30秒
        private const val MAP_CLEANUP_INTERVAL_MS = 60 * 60 * 1000L  // Map 清理间隔：1小时
        private const val MAP_ENTRY_MAX_AGE_MS = 24 * 60 * 60 * 1000L  // Map 条目最大存活时间：24小时
        private const val DEBOUNCE_INTERVAL_MS = 500L  // 防抖动间隔：500ms
        private const val MOVE_TO_FRONT_RETRY_DELAY_MS = 100L  // moveTaskToFront 重试延迟
        private const val MOVE_TO_FRONT_MAX_ATTEMPTS = 3  // moveTaskToFront 最大尝试次数
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val cooldownMap = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val lastCheckTime = java.util.concurrent.ConcurrentHashMap<String, Long>()  // 防抖动：记录最后检查时间

    // 使用时间追踪
    private lateinit var usageTrackingManager: UsageTrackingManager
    private var currentForegroundApp: String? = null
    private var foregroundStartTime: Long = 0

    // 视频应用定时检查
    private val videoAppCheckHandler = Handler(Looper.getMainLooper())
    private var isVideoAppCheckRunning = false
    private var currentVideoApp: String? = null  // 当前正在检查的视频应用

    // 视频应用定时检查 Runnable
    private val videoAppCheckRunnable = object : Runnable {
        override fun run() {
            val targetApp = currentVideoApp
            if (targetApp == null || !isVideoAppCheckRunning) {
                Log.d(TAG, "[VideoAppCheck] No target app or not running, stopping")
                stopVideoAppCheck()
                return
            }

            // 验证当前前台是否仍然是目标视频应用
            val actualForeground = try {
                rootInActiveWindow?.packageName?.toString()
            } catch (e: Exception) {
                null
            }

            // 短视频应用特殊处理：
            // 1. actualForeground == null：可能是全屏视频播放（SurfaceView/TextureView），继续检查
            // 2. actualForeground == targetApp：正常情况，继续检查
            // 3. actualForeground 是其他应用：用户已离开，停止定时器
            if (actualForeground != null && actualForeground != targetApp) {
                Log.d(TAG, "[VideoAppCheck] Foreground changed to $actualForeground, stopping")
                stopVideoAppCheck()
                return
            }

            // null 情况：全屏视频播放时常见，记录日志但继续检查
            if (actualForeground == null) {
                Log.d(TAG, "[VideoAppCheck] Foreground is null (fullscreen video?), proceeding with check anyway")
            }

            Log.d(TAG, "[VideoAppCheck] Timer fired for $targetApp, checking cooldown and warnings")

            // 检查是否需要触发弹窗
            serviceScope.launch {
                try {
                    val monitoredApp = repository.getMonitoredApp(targetApp)
                    if (monitoredApp == null || !monitoredApp.isEnabled) {
                        Log.d(TAG, "[VideoAppCheck] App not monitored or disabled, stopping")
                        stopVideoAppCheck()
                        return@launch
                    }

                    val hasTimeLimit = monitoredApp.dailyLimitMinutes != null && monitoredApp.dailyLimitMinutes > 0

                    if (hasTimeLimit) {
                        // 有时间限制：通过 checkAndShowUsageWarning 处理
                        checkAndShowUsageWarning(targetApp)
                    } else {
                        // 无时间限制：检查 cooldown 并触发深呼吸
                        val cooldownMinutes = getEffectiveCooldownMinutes(targetApp)
                        val lastIntervention = cooldownMap[targetApp] ?: 0
                        val cooldownMs = cooldownMinutes * 60 * 1000L
                        val elapsed = System.currentTimeMillis() - lastIntervention

                        if (elapsed >= cooldownMs) {
                            cooldownMap[targetApp] = System.currentTimeMillis()
                            val defaultCountdown = repository.defaultCountdown.first()
                            Log.d(TAG, "[VideoAppCheck] Cooldown passed, triggering deep breath for $targetApp")
                            launchDeepBreathOverlay(targetApp, monitoredApp.appName, defaultCountdown, monitoredApp.redirectPackage)
                        } else {
                            Log.d(TAG, "[VideoAppCheck] $targetApp in cooldown (${elapsed/1000}s < ${cooldownMs/1000}s)")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "[VideoAppCheck] Error: ${e.message}")
                }
            }

            // 继续下一次检查
            if (isVideoAppCheckRunning) {
                videoAppCheckHandler.postDelayed(this, VIDEO_APP_CHECK_INTERVAL_MS)
            }
        }
    }

    // 今天已显示100%温和警告的应用（当天只显示一次）
    private val shownLimitWarningToday = mutableSetOf<String>()
    private var lastResetDate: String = ""

    private val repository by lazy {
        (application as SlowDownApp).repository
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "[Service] ===== AccessibilityService CONNECTED =====")
        Log.d(TAG, "[Service] Service info: ${serviceInfo?.let { "eventTypes=${it.eventTypes}, feedbackType=${it.feedbackType}, flags=${it.flags}" } ?: "null"}")

        // 初始化使用时间追踪管理器
        usageTrackingManager = UsageTrackingManager(this, repository)

        // 注册同步完成回调：同步后检查当前前台应用是否需要显示警告
        usageTrackingManager.setOnSyncCompleteListener { updatedPackages ->
            val currentFg = currentForegroundApp
            if (currentFg != null && currentFg in updatedPackages) {
                // 关键检查：确认该应用确实在前台，而不是残留的旧状态
                val actualForeground = try {
                    rootInActiveWindow?.packageName?.toString()
                } catch (e: Exception) {
                    null
                }

                // 只有当实际前台应用是被监控应用时才检查警告
                // 避免在 SlowDown 自己界面或其他应用时误触发
                //
                // 特殊处理 rootInActiveWindow == null 的情况：
                // 1. 全屏视频播放时（SurfaceView/TextureView）常见
                // 2. 浏览器渲染 WebView 时也可能发生
                // 3. 某些应用的特殊 UI 状态
                // 此时用户可能仍在被监控应用中，直接继续检查
                if (actualForeground == currentFg || actualForeground == null) {
                    if (actualForeground == null) {
                        Log.d(TAG, "[Service] Sync completed, foreground is null, proceeding with check for: $currentFg")
                    } else {
                        Log.d(TAG, "[Service] Sync completed, checking warnings for current foreground: $currentFg")
                    }
                    serviceScope.launch {
                        checkAndShowUsageWarning(currentFg)
                    }
                } else {
                    Log.d(TAG, "[Service] Sync completed but actual foreground ($actualForeground) != tracked ($currentFg), skip warning check")
                }
            }
        }

        usageTrackingManager.startPeriodicSync()
        Log.d(TAG, "[Service] UsageTrackingManager initialized and periodic sync started")

        // 启动前台服务通知 - 防止 MIUI 冻结 AccessibilityService
        startForegroundNotification()

        // 启动定期清理 Map 的任务，防止内存泄漏
        startMapCleanupTask()
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

    /**
     * 启动定期清理 Map 的任务
     * 防止 cooldownMap 和 lastCheckTime 无限增长导致内存泄漏
     */
    private fun startMapCleanupTask() {
        serviceScope.launch {
            while (true) {
                delay(MAP_CLEANUP_INTERVAL_MS)
                cleanupStaleMaps()
            }
        }
    }

    /**
     * 清理过期的 Map 条目
     * 删除超过 24 小时未更新的条目
     */
    private fun cleanupStaleMaps() {
        val now = System.currentTimeMillis()
        var cleanedCooldown = 0
        var cleanedLastCheck = 0

        cooldownMap.entries.removeIf { entry ->
            val shouldRemove = now - entry.value > MAP_ENTRY_MAX_AGE_MS
            if (shouldRemove) cleanedCooldown++
            shouldRemove
        }

        lastCheckTime.entries.removeIf { entry ->
            val shouldRemove = now - entry.value > MAP_ENTRY_MAX_AGE_MS
            if (shouldRemove) cleanedLastCheck++
            shouldRemove
        }

        if (cleanedCooldown > 0 || cleanedLastCheck > 0) {
            Log.d(TAG, "[MapCleanup] Cleaned $cleanedCooldown cooldown entries, $cleanedLastCheck lastCheck entries")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 最早的日志点 - 确认事件是否到达
        val eventType = event?.eventType
        val pkg = event?.packageName?.toString()
        Log.d(TAG, "[Service] onAccessibilityEvent: type=$eventType, pkg=$pkg")

        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Skip our own package - 但要先清除追踪状态
        if (packageName == this.packageName) {
            // 用户切换到 SlowDown 自己，清除当前前台应用追踪
            if (currentForegroundApp != null) {
                Log.d(TAG, "[Service] User switched to SlowDown, clearing foreground tracking (was: $currentForegroundApp)")
                stopVideoAppCheck()  // 停止视频应用检查
                currentForegroundApp = null
                foregroundStartTime = 0
            }
            return
        }

        // Skip system critical apps - 也要清除追踪状态
        if (PackageUtils.isSystemCriticalApp(packageName)) {
            // 用户切换到系统应用（如桌面），清除追踪
            if (currentForegroundApp != null) {
                Log.d(TAG, "[Service] User switched to system app $packageName, clearing foreground tracking (was: $currentForegroundApp)")
                stopVideoAppCheck()  // 停止视频应用检查
                currentForegroundApp = null
                foregroundStartTime = 0
            }
            return
        }

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
     *
     * 重要：即使是同一个应用内的事件，也需要检查 cooldown 是否到期
     * 因为用户可能在应用内持续使用，cooldown 到期后应该触发弹窗
     */
    private fun handleRealtimeTracking(newPackageName: String) {
        if (!::usageTrackingManager.isInitialized) return

        val isSameApp = newPackageName == currentForegroundApp

        // 只有切换到不同应用时才记录使用时间和重置追踪
        if (!isSameApp) {
            // 记录上一个应用的使用时间
            val previousApp = currentForegroundApp
            if (usageTrackingManager.isRealtimeTrackingEnabled() && previousApp != null && foregroundStartTime > 0) {
                val duration = System.currentTimeMillis() - foregroundStartTime
                if (duration > 0) {
                    usageTrackingManager.recordForegroundTime(previousApp, duration)
                    Log.d(TAG, "[UsageTracking] Recorded ${duration}ms for $previousApp")
                }
            }

            // 切换应用时停止之前的视频应用检查
            stopVideoAppCheck()

            // 更新当前前台应用
            currentForegroundApp = newPackageName
            foregroundStartTime = System.currentTimeMillis()
        }

        // 检查是否需要触发弹窗
        // 无论是否同一应用，都需要检查（因为 cooldown 可能已到期）
        serviceScope.launch {
            try {
                val monitoredApp = repository.getMonitoredApp(newPackageName)

                // 只有切换到新应用时才同步使用时间
                if (!isSameApp && monitoredApp != null) {
                    Log.d(TAG, "[UsageTracking] Syncing usage stats before checking warnings")
                    usageTrackingManager.syncNow()
                    // 给同步一点时间完成
                    kotlinx.coroutines.delay(200)
                }

                if (!isSameApp && usageTrackingManager.checkRealtimeTrackingNeeded(newPackageName)) {
                    if (!usageTrackingManager.isRealtimeTrackingEnabled()) {
                        usageTrackingManager.startRealtimeTracking(newPackageName)
                        Log.d(TAG, "[UsageTracking] Started realtime tracking for $newPackageName")
                    }
                }

                // 切换到新应用时，检查是否需要启动视频应用定时检查
                if (!isSameApp && monitoredApp != null && monitoredApp.isEnabled && monitoredApp.isVideoApp) {
                    Log.d(TAG, "[UsageTracking] $newPackageName is a video app, starting periodic check")
                    startVideoAppCheck(newPackageName)
                }

                // 检查使用时间警告（无论是否同一应用都检查，让 cooldown 机制来控制）
                checkAndShowUsageWarning(newPackageName)
            } catch (e: Exception) {
                Log.e(TAG, "[UsageTracking] Error in realtime tracking: ${e.message}")
            }
        }
    }

    /**
     * 检查并显示使用时间警告
     *
     * 逻辑说明：
     * - 深呼吸弹窗：可重复触发，受 cooldown 控制
     * - 100%温和警告：当天只触发一次，之后继续触发深呼吸
     * - 100%强制关闭：每次打开都触发
     */
    private suspend fun checkAndShowUsageWarning(packageName: String) {
        if (!::usageTrackingManager.isInitialized) return

        // 防抖动：DEBOUNCE_INTERVAL_MS 内不重复检查同一应用
        val now = System.currentTimeMillis()
        val lastCheck = lastCheckTime[packageName] ?: 0
        if (now - lastCheck < DEBOUNCE_INTERVAL_MS) {
            Log.d(TAG, "[Debounce] Skip duplicate check for $packageName (${now - lastCheck}ms ago)")
            return
        }
        lastCheckTime[packageName] = now

        // 每天重置已显示警告的记录
        val todayDate = java.time.LocalDate.now().toString()
        if (todayDate != lastResetDate) {
            shownLimitWarningToday.clear()
            lastResetDate = todayDate
            Log.d(TAG, "[UsageWarning] Reset daily warning records for $todayDate")
        }

        val warningType = usageTrackingManager.checkUsageWarning(packageName) ?: return

        val monitoredApp = repository.getMonitoredApp(packageName) ?: return
        val dailyLimit = monitoredApp.dailyLimitMinutes ?: 0
        val usageRecord = repository.getUsageRecord(packageName, todayDate)
        val usedMinutes = usageRecord?.usageMinutes ?: 0

        Log.d(TAG, "[UsageWarning] Checking warning for $packageName: $warningType (used=$usedMinutes, limit=$dailyLimit)")

        when (warningType) {
            UsageWarningType.SOFT_REMINDER -> {
                // ≥80% 但 <100%：显示深呼吸弹窗（受 cooldown 控制）
                if (checkCooldown(packageName)) {
                    val defaultCountdown = repository.defaultCountdown.first()
                    Log.d(TAG, "[UsageWarning] Showing deep breath popup for $packageName at ≥80%")
                    launchDeepBreathOverlay(packageName, monitoredApp.appName, defaultCountdown, monitoredApp.redirectPackage)
                    updateCooldown(packageName)
                }
            }

            UsageWarningType.LIMIT_REACHED_SOFT -> {
                // ≥100% + 软提醒模式：统一使用深呼吸弹窗，但显示"已达限额"风格的 UI
                // 首次达到100%时标记，之后都受 cooldown 控制
                if (packageName !in shownLimitWarningToday) {
                    shownLimitWarningToday.add(packageName)
                    Log.d(TAG, "[UsageWarning] First time reaching 100% today for $packageName")
                }

                if (checkCooldown(packageName)) {
                    val defaultCountdown = repository.defaultCountdown.first()
                    Log.d(TAG, "[UsageWarning] Showing deep breath (limit reached style) for $packageName at ≥100%")
                    launchDeepBreathOverlay(
                        packageName = packageName,
                        appName = monitoredApp.appName,
                        countdownSeconds = defaultCountdown,
                        redirectPackage = monitoredApp.redirectPackage,
                        isLimitReached = true,
                        usedMinutes = usedMinutes,
                        limitMinutes = dailyLimit
                    )
                    updateCooldown(packageName)
                }
            }

            UsageWarningType.LIMIT_REACHED_STRICT -> {
                // ≥100% + 强制模式：每次都显示强制关闭弹窗
                Log.d(TAG, "[UsageWarning] Showing strict mode warning for $packageName")

                launchUsageWarningActivity(
                    packageName = packageName,
                    appName = monitoredApp.appName,
                    warningType = warningType,
                    usedMinutes = usedMinutes,
                    limitMinutes = dailyLimit,
                    redirectPackage = monitoredApp.redirectPackage
                )

                NotificationHelper.showUsageWarningNotification(
                    context = this,
                    packageName = packageName,
                    appName = monitoredApp.appName,
                    warningType = warningType
                )
            }
        }
    }

    /**
     * 获取应用的有效冷却时间（分钟）
     * 优先使用应用单独设置，否则使用全局设置
     */
    private suspend fun getEffectiveCooldownMinutes(packageName: String): Int {
        val monitoredApp = repository.getMonitoredApp(packageName)
        val appCooldown = monitoredApp?.cooldownMinutes
        val globalCooldown = repository.cooldownMinutes.first()

        // 应用设置优先，否则用全局；最小值保护 1 分钟
        return maxOf(appCooldown ?: globalCooldown, 1)
    }

    /**
     * 检查深呼吸弹窗的冷却时间
     */
    private suspend fun checkCooldown(packageName: String): Boolean {
        val cooldownMinutes = getEffectiveCooldownMinutes(packageName)
        val lastTime = cooldownMap[packageName] ?: 0
        val cooldownMs = cooldownMinutes * 60 * 1000L
        val elapsed = System.currentTimeMillis() - lastTime
        val canShow = elapsed >= cooldownMs
        if (!canShow) {
            Log.d(TAG, "[UsageWarning] $packageName in cooldown (${elapsed/1000}s < ${cooldownMs/1000}s)")
        }
        return canShow
    }

    /**
     * 更新深呼吸弹窗的冷却时间
     */
    private fun updateCooldown(packageName: String) {
        cooldownMap[packageName] = System.currentTimeMillis()
    }

    /**
     * 启动使用时间警告 Activity
     */
    private fun launchUsageWarningActivity(
        packageName: String,
        appName: String,
        warningType: UsageWarningType,
        usedMinutes: Int,
        limitMinutes: Int,
        redirectPackage: String?
    ) {
        // 最后一道防线：在启动弹窗前再次验证当前前台应用
        val actualForeground = try {
            rootInActiveWindow?.packageName?.toString()
        } catch (e: Exception) {
            null
        }

        // 如果当前前台不是目标应用，跳过弹窗
        if (actualForeground != null && actualForeground != packageName) {
            Log.d(TAG, "[UsageWarning] launchUsageWarningActivity: actual foreground ($actualForeground) != target ($packageName), skip")
            return
        }

        try {
            val intent = Intent(this, UsageWarningActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                putExtra(UsageWarningActivity.EXTRA_WARNING_TYPE, warningType.name)
                putExtra(UsageWarningActivity.EXTRA_PACKAGE_NAME, packageName)
                putExtra(UsageWarningActivity.EXTRA_APP_NAME, appName)
                putExtra(UsageWarningActivity.EXTRA_USED_MINUTES, usedMinutes)
                putExtra(UsageWarningActivity.EXTRA_LIMIT_MINUTES, limitMinutes)
                putExtra(UsageWarningActivity.EXTRA_REDIRECT_PACKAGE, redirectPackage)
            }

            // 尝试设置 MIUI 特定标志位（自动检测 MIUI）
            MiuiHelper.addMiuiFlags(intent)

            startActivity(intent)
            Log.d(TAG, "[UsageWarning] UsageWarningActivity launched for $packageName")

            // 使用 moveTaskToFront 强制将任务移到前台（MIUI 兼容）
            val handler = Handler(Looper.getMainLooper())
            handler.post { moveSlowDownToFront() }
            handler.postDelayed({ moveSlowDownToFront() }, 100)
            handler.postDelayed({ moveSlowDownToFront() }, 300)

        } catch (e: Exception) {
            Log.e(TAG, "[UsageWarning] Failed to launch UsageWarningActivity: ${e.message}")
        }
    }

    /**
     * 显示达到限额的干预界面（已弃用，保留以备后用）
     */
    @Suppress("unused")
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

        // 判断是否有时间限制
        val hasTimeLimit = monitoredApp.dailyLimitMinutes != null && monitoredApp.dailyLimitMinutes > 0
        val isStrictMode = monitoredApp.limitMode == "strict"

        // 有时间限制的应用：只在 ≥80% 时触发深呼吸（由 checkAndShowUsageWarning 处理）
        // 无时间限制的应用：每次打开都触发深呼吸弹窗
        if (hasTimeLimit) {
            Log.d(TAG, "[Service] $packageName has time limit, skip launch intervention (handled by usage tracking)")
            return
        }

        // 无时间限制 + 强制关闭模式：直接显示强制关闭弹窗（不显示深呼吸）
        if (isStrictMode) {
            Log.d(TAG, "[Service] $packageName has no limit + strict mode, showing strict warning immediately")
            launchUsageWarningActivity(
                packageName = packageName,
                appName = monitoredApp.appName,
                warningType = UsageWarningType.LIMIT_REACHED_STRICT,
                usedMinutes = 0,
                limitMinutes = 0,
                redirectPackage = monitoredApp.redirectPackage
            )
            NotificationHelper.showUsageWarningNotification(
                context = this,
                packageName = packageName,
                appName = monitoredApp.appName,
                warningType = UsageWarningType.LIMIT_REACHED_STRICT
            )
            return
        }

        // 无时间限制 + 软提醒模式：使用 cooldown 机制，每次打开触发深呼吸
        Log.d(TAG, "[Service] $packageName has no time limit + soft mode, checking cooldown for deep breath popup")

        // Check cooldown（使用应用单独设置或全局设置）
        val cooldownMinutes = getEffectiveCooldownMinutes(packageName)
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

        Log.d(TAG, "[Service] Triggering deep breath for $packageName (no limit), countdown=${defaultCountdown}s")

        // 启动深呼吸弹窗
        launchDeepBreathOverlay(packageName, monitoredApp.appName, defaultCountdown, monitoredApp.redirectPackage)
    }

    /**
     * 启动深呼吸弹窗（OverlayActivity）
     * @param isLimitReached 是否已达到限额（true 时显示"休息一下"风格的 UI）
     * @param usedMinutes 已使用分钟数（仅当 isLimitReached=true 时有意义）
     * @param limitMinutes 限额分钟数（仅当 isLimitReached=true 时有意义）
     */
    private fun launchDeepBreathOverlay(
        packageName: String,
        appName: String,
        countdownSeconds: Int,
        redirectPackage: String?,
        isLimitReached: Boolean = false,
        usedMinutes: Int = 0,
        limitMinutes: Int = 0
    ) {
        // 最后一道防线：在启动弹窗前再次验证当前前台应用
        // 防止因为异步延迟导致在错误的时机显示弹窗
        val actualForeground = try {
            rootInActiveWindow?.packageName?.toString()
        } catch (e: Exception) {
            null
        }

        // 如果当前前台不是目标应用，跳过弹窗
        // 注意：actualForeground == null 时允许继续（与 launchUsageWarningActivity 保持一致）
        // 因为 rootInActiveWindow 在某些情况下会返回 null，但用户可能仍在目标应用中
        if (actualForeground != null && actualForeground != packageName) {
            Log.d(TAG, "[Service] launchDeepBreathOverlay: actual foreground ($actualForeground) != target ($packageName), skip")
            return
        }

        if (actualForeground == null) {
            Log.d(TAG, "[Service] launchDeepBreathOverlay: foreground is null, proceeding anyway (may be fullscreen mode)")
        }

        // 统一启动策略：直接启动 Activity（所有设备）
        // 不再依赖 Full-Screen Intent，避免不同 ROM 的兼容性问题
        Log.d(TAG, "[Service] Launching OverlayActivity directly (unified strategy)")
        launchOverlayDirectly(packageName, appName, countdownSeconds, redirectPackage, isLimitReached, usedMinutes, limitMinutes)

        // OverlayService 已弃用，统一使用 OverlayActivity（Compose UI）
        // 删除旧的 WindowManager 悬浮窗以避免两阶段渲染问题
    }

    /**
     * 直接启动 OverlayActivity 并强制移到前台
     *
     * 适用于所有设备，避免 Full-Screen Intent 的兼容性问题
     *
     * 策略：
     * 1. 先启动 Activity
     * 2. 使用 ActivityManager.moveTaskToFront() 强制将任务移到前台
     * 3. 延迟重试确保显示（100ms、300ms）
     * 4. 失败时回退到 Full-Screen Intent
     */
    private fun launchOverlayDirectly(
        packageName: String,
        appName: String,
        countdownSeconds: Int,
        redirectPackage: String?,
        isLimitReached: Boolean = false,
        usedMinutes: Int = 0,
        limitMinutes: Int = 0
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
                putExtra(OverlayActivity.EXTRA_IS_LIMIT_REACHED, isLimitReached)
                putExtra(OverlayActivity.EXTRA_USED_MINUTES, usedMinutes)
                putExtra(OverlayActivity.EXTRA_LIMIT_MINUTES, limitMinutes)
            }

            // 尝试设置 MIUI 特定标志位
            val miuiFlagsSet = MiuiHelper.addMiuiFlags(intent)
            Log.d(TAG, "[Service] MiuiFlags set result: $miuiFlagsSet")

            // 启动 Activity
            startActivity(intent)
            Log.d(TAG, "[Service] OverlayActivity launched")

            // 关键：使用 moveTaskToFront 强制将任务移到前台
            // 立即尝试一次，然后延迟重试确保 Activity 已创建
            moveSlowDownToFrontWithRetry(
                maxAttempts = MOVE_TO_FRONT_MAX_ATTEMPTS,
                delayMs = MOVE_TO_FRONT_RETRY_DELAY_MS
            )

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
     * 使用 ActivityManager.moveTaskToFront() 将 SlowDown 任务移到前台（带重试）
     * @param maxAttempts 最大尝试次数
     * @param delayMs 每次重试的延迟时间
     */
    private fun moveSlowDownToFrontWithRetry(maxAttempts: Int, delayMs: Long, attempt: Int = 1) {
        if (attempt > maxAttempts) {
            Log.d(TAG, "[Service] moveTaskToFront: max attempts ($maxAttempts) reached")
            return
        }

        val success = moveSlowDownToFront()
        if (success) {
            Log.d(TAG, "[Service] moveTaskToFront succeeded on attempt $attempt")
            return
        }

        // 失败则延迟重试
        Handler(Looper.getMainLooper()).postDelayed({
            moveSlowDownToFrontWithRetry(maxAttempts, delayMs, attempt + 1)
        }, delayMs)
    }

    /**
     * 使用 ActivityManager.moveTaskToFront() 将 SlowDown 任务移到前台
     * @return 是否成功找到并移动任务
     */
    private fun moveSlowDownToFront(): Boolean {
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
                    return true
                }
            }

            Log.d(TAG, "[Service] SlowDown task not found in running tasks")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "[Service] moveTaskToFront failed: ${e.message}")
            return false
        }
    }

    override fun onInterrupt() {
        // Required override
    }

    /**
     * 启动视频应用定时检查
     * 对于标记为视频应用的应用，启动定时器主动检查 cooldown 并触发弹窗
     */
    private fun startVideoAppCheck(packageName: String) {
        if (isVideoAppCheckRunning && currentVideoApp == packageName) {
            Log.d(TAG, "[VideoAppCheck] Already running for $packageName")
            return
        }

        // 停止之前的检查（如果有）
        stopVideoAppCheck()

        currentVideoApp = packageName
        isVideoAppCheckRunning = true

        // 延迟第一次检查，避免与普通触发重复
        videoAppCheckHandler.postDelayed(videoAppCheckRunnable, VIDEO_APP_CHECK_INTERVAL_MS)
        Log.d(TAG, "[VideoAppCheck] Started for $packageName, interval=${VIDEO_APP_CHECK_INTERVAL_MS}ms")
    }

    /**
     * 停止视频应用定时检查
     */
    private fun stopVideoAppCheck() {
        if (!isVideoAppCheckRunning) return

        videoAppCheckHandler.removeCallbacks(videoAppCheckRunnable)
        isVideoAppCheckRunning = false
        val stoppedApp = currentVideoApp
        currentVideoApp = null
        Log.d(TAG, "[VideoAppCheck] Stopped (was: $stoppedApp)")
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止视频应用定时检查
        stopVideoAppCheck()
        // 停止使用时间追踪
        if (::usageTrackingManager.isInitialized) {
            usageTrackingManager.stopRealtimeTracking()
            usageTrackingManager.stopPeriodicSync()
            Log.d(TAG, "[Service] UsageTrackingManager stopped")
        }
        serviceScope.cancel()
    }
}
