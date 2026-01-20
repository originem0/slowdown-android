package com.sharonZ.slowdown.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.sharonZ.slowdown.data.repository.SlowDownRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 使用时间追踪管理器
 * 负责：
 * 1. 定期从 UsageStatsManager 同步使用数据到本地数据库
 * 2. 当使用时间接近限额时启动实时追踪
 * 3. 检查并触发使用时间警告
 */
class UsageTrackingManager(
    private val context: Context,
    private val repository: SlowDownRepository
) {
    companion object {
        private const val TAG = "UsageTrackingManager"
        private const val DEFAULT_SYNC_INTERVAL_MS = 5 * 60 * 1000L  // 默认 5 分钟
        private const val SHORT_SYNC_INTERVAL_MS = 1 * 60 * 1000L   // 短限额时 1 分钟
        private const val SHORT_LIMIT_THRESHOLD = 10  // 限额小于 10 分钟时使用短同步间隔
        private const val REALTIME_THRESHOLD = 0.70  // 70% 时启动实时追踪
        private const val WARNING_THRESHOLD = 0.80   // 80% 警告阈值
    }

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentSyncInterval = DEFAULT_SYNC_INTERVAL_MS

    // 同步完成后的回调，用于通知 AppMonitorService 检查当前前台应用
    private var onSyncCompleteListener: ((Set<String>) -> Unit)? = null

    /**
     * 设置同步完成回调
     * @param listener 回调函数，参数是本次同步中更新的包名集合
     */
    fun setOnSyncCompleteListener(listener: ((Set<String>) -> Unit)?) {
        onSyncCompleteListener = listener
    }

    // 定期同步任务
    private val syncRunnable = object : Runnable {
        override fun run() {
            Log.d(TAG, "Running periodic UsageStats sync (interval: ${currentSyncInterval / 1000}s)")
            scope.launch {
                try {
                    syncUsageStats()
                    updateSyncInterval()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync usage stats: ${e.message}")
                }
            }
            handler.postDelayed(this, currentSyncInterval)
        }
    }

    // 实时追踪状态
    private var isRealtimeTrackingEnabled = false
    private var currentTrackingPackage: String? = null
    private var trackingStartTime: Long = 0
    @Volatile  // 保证线程安全
    private var accumulatedRealtimeMs: Long = 0

    /**
     * 启动定期同步
     */
    fun startPeriodicSync() {
        scope.launch {
            updateSyncInterval()
            Log.d(TAG, "Starting periodic sync (interval: ${currentSyncInterval / 1000}s)")
            handler.post(syncRunnable)
        }
    }

    /**
     * 停止定期同步
     */
    fun stopPeriodicSync() {
        Log.d(TAG, "Stopping periodic sync")
        handler.removeCallbacks(syncRunnable)
    }

    /**
     * 立即同步使用统计（用于打开应用时即时获取最新数据）
     */
    fun syncNow() {
        Log.d(TAG, "Triggering immediate UsageStats sync")
        scope.launch {
            try {
                syncUsageStats()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync usage stats: ${e.message}")
            }
        }
    }

    /**
     * 从 UsageStatsManager 同步数据到本地数据库
     * 使用 queryEvents() 获取准确的实时数据，而非 queryUsageStats(INTERVAL_DAILY) 的延迟聚合数据
     */
    suspend fun syncUsageStats() {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usageStatsManager == null) {
            Log.e(TAG, "UsageStatsManager not available")
            return
        }

        val endTime = System.currentTimeMillis()
        val startTime = getTodayStartTime()

        Log.d(TAG, "Querying usage events from ${formatTime(startTime)} to ${formatTime(endTime)}")

        // 获取所有被监控的应用包名
        val monitoredApps = repository.monitoredApps.first()
        val monitoredPackages = monitoredApps.map { it.packageName }.toSet()

        if (monitoredPackages.isEmpty()) {
            Log.d(TAG, "No monitored apps, skipping sync")
            return
        }

        // 使用 queryEvents 获取精确的事件数据
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        if (usageEvents == null) {
            Log.d(TAG, "No usage events available (permission might not be granted)")
            return
        }

        // 计算每个应用的前台时间
        val foregroundTimeMap = mutableMapOf<String, Long>()  // packageName -> foreground time in ms
        val lastForegroundTime = mutableMapOf<String, Long>()  // packageName -> last MOVE_TO_FOREGROUND timestamp

        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val packageName = event.packageName

            // 只处理被监控的应用
            if (packageName !in monitoredPackages) continue

            when (event.eventType) {
                // MOVE_TO_FOREGROUND/BACKGROUND 在 API 29 后废弃，但为兼容旧版本仍需处理
                @Suppress("DEPRECATION")
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    // 记录进入前台的时间
                    lastForegroundTime[packageName] = event.timeStamp
                }
                @Suppress("DEPRECATION")
                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    // 计算前台停留时间
                    val foregroundStart = lastForegroundTime[packageName]
                    if (foregroundStart != null && foregroundStart > 0) {
                        val duration = event.timeStamp - foregroundStart
                        if (duration > 0) {
                            foregroundTimeMap[packageName] = (foregroundTimeMap[packageName] ?: 0L) + duration
                        }
                        lastForegroundTime[packageName] = 0  // 重置
                    }
                }
            }
        }

        // 处理仍在前台的应用（没有 MOVE_TO_BACKGROUND 事件）
        val now = System.currentTimeMillis()
        for ((packageName, foregroundStart) in lastForegroundTime) {
            if (foregroundStart > 0) {
                val duration = now - foregroundStart
                if (duration > 0) {
                    foregroundTimeMap[packageName] = (foregroundTimeMap[packageName] ?: 0L) + duration
                }
            }
        }

        Log.d(TAG, "Calculated foreground time for ${foregroundTimeMap.size} apps")

        // 更新数据库
        val updatedPackages = mutableSetOf<String>()
        for ((packageName, foregroundMs) in foregroundTimeMap) {
            val minutes = (foregroundMs / 1000 / 60).toInt()
            if (minutes > 0) {
                Log.d(TAG, "Updating usage for $packageName: $minutes minutes (${foregroundMs}ms)")
                repository.updateUsageMinutes(packageName, minutes)
                updatedPackages.add(packageName)
            }
        }

        // 同步完成后通知监听器（用于检查当前前台应用是否需要显示警告）
        if (updatedPackages.isNotEmpty()) {
            onSyncCompleteListener?.invoke(updatedPackages)
        }
    }

    /**
     * 根据监控应用的状态动态调整同步间隔
     * 使用 1 分钟间隔的条件（满足任一）：
     * - 有应用限额 < 10 分钟
     * - 有应用使用时间 ≥ 80% 限额（且该应用启用了提醒）
     * 否则使用默认 5 分钟间隔
     */
    private suspend fun updateSyncInterval() {
        val monitoredApps = repository.monitoredApps.first()
        val todayDate = java.time.LocalDate.now().toString()

        var needShortInterval = false

        for (app in monitoredApps) {
            if (!app.isEnabled) continue

            val dailyLimit = app.dailyLimitMinutes
            if (dailyLimit == null) continue  // 无限制的应用不影响同步间隔

            // 条件1：限额 < 10 分钟
            if (dailyLimit < SHORT_LIMIT_THRESHOLD) {
                needShortInterval = true
                Log.d(TAG, "Short interval needed: ${app.packageName} has limit $dailyLimit < $SHORT_LIMIT_THRESHOLD min")
                break
            }

            // 条件2：使用时间 ≥ 80% 限额
            val usageRecord = repository.getUsageRecord(app.packageName, todayDate)
            val currentMinutes = usageRecord?.usageMinutes ?: 0
            val usageRatio = currentMinutes.toDouble() / dailyLimit

            if (usageRatio >= WARNING_THRESHOLD) {
                needShortInterval = true
                Log.d(TAG, "Short interval needed: ${app.packageName} at ${(usageRatio * 100).toInt()}% (>= ${(WARNING_THRESHOLD * 100).toInt()}%)")
                break
            }
        }

        val newInterval = if (needShortInterval) SHORT_SYNC_INTERVAL_MS else DEFAULT_SYNC_INTERVAL_MS

        if (newInterval != currentSyncInterval) {
            Log.d(TAG, "Sync interval changed: ${currentSyncInterval / 1000}s -> ${newInterval / 1000}s")
            currentSyncInterval = newInterval
        }
    }

    /**
     * 检查是否需要启动实时追踪（使用时间 >= 70% 限额）
     */
    suspend fun checkRealtimeTrackingNeeded(packageName: String): Boolean {
        val app = repository.getMonitoredApp(packageName) ?: return false
        val dailyLimit = app.dailyLimitMinutes ?: return false  // 无限制则不需要实时追踪

        val todayDate = java.time.LocalDate.now().toString()
        val usageRecord = repository.getUsageRecord(packageName, todayDate)
        val currentMinutes = usageRecord?.usageMinutes ?: 0

        val usageRatio = currentMinutes.toDouble() / dailyLimit
        val needsRealtime = usageRatio >= REALTIME_THRESHOLD

        Log.d(TAG, "checkRealtimeTrackingNeeded($packageName): $currentMinutes/$dailyLimit min (${(usageRatio * 100).toInt()}%), needs realtime: $needsRealtime")

        return needsRealtime
    }

    /**
     * 启动实时追踪
     */
    fun startRealtimeTracking(packageName: String) {
        Log.d(TAG, "Starting realtime tracking for $packageName")
        isRealtimeTrackingEnabled = true
        currentTrackingPackage = packageName
        trackingStartTime = System.currentTimeMillis()
        accumulatedRealtimeMs = 0
    }

    /**
     * 停止实时追踪
     */
    fun stopRealtimeTracking() {
        val packageToRecord = currentTrackingPackage
        if (isRealtimeTrackingEnabled && packageToRecord != null) {
            // 记录最后一段时间
            val duration = System.currentTimeMillis() - trackingStartTime
            if (duration > 0) {
                scope.launch {
                    recordForegroundTimeInternal(packageToRecord, duration)
                }
            }
        }

        Log.d(TAG, "Stopping realtime tracking")
        isRealtimeTrackingEnabled = false
        currentTrackingPackage = null
        trackingStartTime = 0
        accumulatedRealtimeMs = 0
    }

    /**
     * 记录前台使用时间（被 AppMonitorService 调用）
     */
    fun recordForegroundTime(packageName: String, durationMs: Long) {
        if (durationMs <= 0) return

        scope.launch {
            recordForegroundTimeInternal(packageName, durationMs)
        }
    }

    private suspend fun recordForegroundTimeInternal(packageName: String, durationMs: Long) {
        // 检查是否是被监控的应用
        val monitoredApp = repository.getMonitoredApp(packageName) ?: return

        val todayDate = java.time.LocalDate.now().toString()
        val existingRecord = repository.getUsageRecord(packageName, todayDate)
        val currentMinutes = existingRecord?.usageMinutes ?: 0

        // 将毫秒累加到分钟（只在累积超过60秒时更新）
        accumulatedRealtimeMs += durationMs

        if (accumulatedRealtimeMs >= 60000) {  // 累积超过1分钟
            val additionalMinutes = (accumulatedRealtimeMs / 60000).toInt()
            val newTotalMinutes = currentMinutes + additionalMinutes
            accumulatedRealtimeMs %= 60000  // 保留不足1分钟的部分

            Log.d(TAG, "Recording $additionalMinutes additional minutes for $packageName (total: $newTotalMinutes)")
            repository.updateUsageMinutes(packageName, newTotalMinutes)
        }
    }

    /**
     * 获取当前使用分钟数（包含未写入数据库的缓冲部分）
     *
     * 这是"虚拟分钟"方案的核心：
     * - 数据库记录只有在累积超过60秒时才更新
     * - 但检查警告时需要包含缓冲区中尚未写入的部分
     * - 这样可以将时间精度从 ±60秒 提升到 ~0秒
     *
     * @param packageName 应用包名
     * @return 虚拟使用分钟数 = 数据库记录 + 实时追踪缓冲
     */
    suspend fun getCurrentUsageMinutesWithBuffer(packageName: String): Int {
        val todayDate = java.time.LocalDate.now().toString()
        val dbMinutes = repository.getUsageRecord(packageName, todayDate)?.usageMinutes ?: 0

        // 如果当前正在追踪这个应用，加上缓冲区的秒数
        if (currentTrackingPackage == packageName && isRealtimeTrackingEnabled) {
            val bufferedSeconds = accumulatedRealtimeMs / 1000
            val bufferedMinutes = bufferedSeconds / 60
            Log.d(TAG, "getCurrentUsageMinutesWithBuffer($packageName): db=$dbMinutes + buffer=$bufferedMinutes (${accumulatedRealtimeMs}ms)")
            return dbMinutes + bufferedMinutes.toInt()
        }

        return dbMinutes
    }

    /**
     * 检查使用时间警告类型
     */
    suspend fun checkUsageWarning(packageName: String): UsageWarningType? {
        val app = repository.getMonitoredApp(packageName) ?: return null
        val dailyLimit = app.dailyLimitMinutes ?: return null  // 无限制

        // 使用虚拟分钟（包含缓冲区），提高精度
        val currentMinutes = getCurrentUsageMinutesWithBuffer(packageName)

        val usageRatio = currentMinutes.toDouble() / dailyLimit

        Log.d(TAG, "checkUsageWarning($packageName): $currentMinutes/$dailyLimit min (${(usageRatio * 100).toInt()}%), mode: ${app.limitMode}")

        return when {
            usageRatio >= 1.0 -> {
                // 达到或超过限额
                if (app.limitMode == "strict") {
                    UsageWarningType.LIMIT_REACHED_STRICT
                } else {
                    UsageWarningType.LIMIT_REACHED_SOFT
                }
            }
            usageRatio >= WARNING_THRESHOLD -> {
                // ≥80% 时返回软提醒（深呼吸弹窗）
                UsageWarningType.SOFT_REMINDER
            }
            else -> null
        }
    }

    /**
     * 检查是否启用了实时追踪
     */
    fun isRealtimeTrackingEnabled(): Boolean = isRealtimeTrackingEnabled

    /**
     * 获取当前追踪的包名
     */
    fun getCurrentTrackingPackage(): String? = currentTrackingPackage

    /**
     * 处理应用切换（由 AppMonitorService 调用）
     * @return 如果之前的应用有使用时间需要记录，返回记录的时长（毫秒）
     */
    fun onAppSwitch(newPackageName: String?): Long {
        if (!isRealtimeTrackingEnabled) return 0

        var recordedDuration = 0L

        // 记录上一个应用的使用时间
        if (currentTrackingPackage != null && trackingStartTime > 0) {
            recordedDuration = System.currentTimeMillis() - trackingStartTime
            if (recordedDuration > 0) {
                recordForegroundTime(currentTrackingPackage!!, recordedDuration)
                Log.d(TAG, "App switched from $currentTrackingPackage to $newPackageName, recorded ${recordedDuration}ms")
            }
        }

        // 如果新应用也需要追踪，继续追踪
        if (newPackageName != null) {
            scope.launch {
                val needsTracking = checkRealtimeTrackingNeeded(newPackageName)
                if (needsTracking) {
                    currentTrackingPackage = newPackageName
                    trackingStartTime = System.currentTimeMillis()
                } else {
                    // 新应用不需要追踪，但保持实时追踪模式开启
                    currentTrackingPackage = null
                    trackingStartTime = 0
                }
            }
        }

        return recordedDuration
    }

    /**
     * 获取今天 00:00:00 的时间戳
     */
    private fun getTodayStartTime(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun formatTime(timestamp: Long): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
    }
}

/**
 * 使用时间警告类型
 */
enum class UsageWarningType {
    SOFT_REMINDER,          // ≥80% 软提醒（深呼吸弹窗）
    LIMIT_REACHED_SOFT,     // 100% 软提醒（温和警告，可继续）
    LIMIT_REACHED_STRICT    // 100% 强制关闭
}
