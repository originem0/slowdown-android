package com.example.slowdown.service

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.slowdown.data.repository.SlowDownRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
        private const val SYNC_INTERVAL_MS = 5 * 60 * 1000L  // 5 分钟
        private const val REALTIME_THRESHOLD = 0.70  // 70% 时启动实时追踪
        private const val WARNING_THRESHOLD = 0.80   // 80% 警告阈值
    }

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 定期同步任务
    private val syncRunnable = object : Runnable {
        override fun run() {
            Log.d(TAG, "Running periodic UsageStats sync")
            scope.launch {
                try {
                    syncUsageStats()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync usage stats: ${e.message}")
                }
            }
            handler.postDelayed(this, SYNC_INTERVAL_MS)
        }
    }

    // 实时追踪状态
    private var isRealtimeTrackingEnabled = false
    private var currentTrackingPackage: String? = null
    private var trackingStartTime: Long = 0
    private var accumulatedRealtimeMs: Long = 0

    /**
     * 启动定期同步（每 5 分钟）
     */
    fun startPeriodicSync() {
        Log.d(TAG, "Starting periodic sync (interval: ${SYNC_INTERVAL_MS / 1000}s)")
        // 先立即执行一次同步
        handler.post(syncRunnable)
    }

    /**
     * 停止定期同步
     */
    fun stopPeriodicSync() {
        Log.d(TAG, "Stopping periodic sync")
        handler.removeCallbacks(syncRunnable)
    }

    /**
     * 从 UsageStatsManager 同步数据到本地数据库
     */
    suspend fun syncUsageStats() {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usageStatsManager == null) {
            Log.e(TAG, "UsageStatsManager not available")
            return
        }

        val endTime = System.currentTimeMillis()
        val startTime = getTodayStartTime()

        Log.d(TAG, "Querying usage stats from ${formatTime(startTime)} to ${formatTime(endTime)}")

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        if (stats.isNullOrEmpty()) {
            Log.d(TAG, "No usage stats available (permission might not be granted)")
            return
        }

        Log.d(TAG, "Got ${stats.size} usage stats entries")

        for (stat in stats) {
            // 只记录被监控的应用
            val monitoredApp = repository.getMonitoredApp(stat.packageName)
            if (monitoredApp != null) {
                val minutes = (stat.totalTimeInForeground / 1000 / 60).toInt()
                if (minutes > 0) {
                    Log.d(TAG, "Updating usage for ${stat.packageName}: $minutes minutes")
                    repository.updateUsageMinutes(stat.packageName, minutes)
                }
            }
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
        if (isRealtimeTrackingEnabled && currentTrackingPackage != null) {
            // 记录最后一段时间
            val duration = System.currentTimeMillis() - trackingStartTime
            if (duration > 0) {
                scope.launch {
                    recordForegroundTimeInternal(currentTrackingPackage!!, duration)
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
     * 检查使用时间警告类型
     */
    suspend fun checkUsageWarning(packageName: String): UsageWarningType? {
        val app = repository.getMonitoredApp(packageName) ?: return null
        val dailyLimit = app.dailyLimitMinutes ?: return null  // 无限制

        val todayDate = java.time.LocalDate.now().toString()
        val usageRecord = repository.getUsageRecord(packageName, todayDate)
        val currentMinutes = usageRecord?.usageMinutes ?: 0

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
                UsageWarningType.WARNING_80_PERCENT
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
    WARNING_80_PERCENT,     // 80% 提醒
    LIMIT_REACHED_SOFT,     // 100% 软提醒
    LIMIT_REACHED_STRICT    // 100% 强制关闭
}
