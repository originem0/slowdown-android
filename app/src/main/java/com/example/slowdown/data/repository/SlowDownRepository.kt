package com.example.slowdown.data.repository

import com.example.slowdown.data.local.dao.AppStat
import com.example.slowdown.data.local.dao.DailyStat
import com.example.slowdown.data.local.dao.InterventionDao
import com.example.slowdown.data.local.dao.MonitoredAppDao
import com.example.slowdown.data.local.dao.SuccessRateStat
import com.example.slowdown.data.local.dao.UsageRecordDao
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import com.example.slowdown.data.local.entity.InterventionRecord
import com.example.slowdown.data.local.entity.MonitoredApp
import com.example.slowdown.data.local.entity.UsageRecord
import com.example.slowdown.data.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Calendar

class SlowDownRepository(
    private val interventionDao: InterventionDao,
    private val monitoredAppDao: MonitoredAppDao,
    private val usageRecordDao: UsageRecordDao,
    private val userPreferences: UserPreferences,
    private val context: Context
) {
    companion object {
        private const val TAG = "SlowDownRepository"
    }
    // Preferences
    val serviceEnabled: Flow<Boolean> = userPreferences.serviceEnabled
    val defaultCountdown: Flow<Int> = userPreferences.defaultCountdown
    val cooldownMinutes: Flow<Int> = userPreferences.cooldownMinutes
    val miuiAutoStartConfirmed: Flow<Boolean> = userPreferences.miuiAutoStartConfirmed
    val miuiBackgroundPopupConfirmed: Flow<Boolean> = userPreferences.miuiBackgroundPopupConfirmed
    val miuiBatterySaverConfirmed: Flow<Boolean> = userPreferences.miuiBatterySaverConfirmed
    val miuiLockAppConfirmed: Flow<Boolean> = userPreferences.miuiLockAppConfirmed
    val appLanguage: Flow<String> = userPreferences.appLanguage

    suspend fun setServiceEnabled(enabled: Boolean) = userPreferences.setServiceEnabled(enabled)
    suspend fun setDefaultCountdown(seconds: Int) = userPreferences.setDefaultCountdown(seconds)
    suspend fun setCooldownMinutes(minutes: Int) = userPreferences.setCooldownMinutes(minutes)
    suspend fun setMiuiAutoStartConfirmed(confirmed: Boolean) = userPreferences.setMiuiAutoStartConfirmed(confirmed)
    suspend fun setMiuiBackgroundPopupConfirmed(confirmed: Boolean) = userPreferences.setMiuiBackgroundPopupConfirmed(confirmed)
    suspend fun setMiuiBatterySaverConfirmed(confirmed: Boolean) = userPreferences.setMiuiBatterySaverConfirmed(confirmed)
    suspend fun setMiuiLockAppConfirmed(confirmed: Boolean) = userPreferences.setMiuiLockAppConfirmed(confirmed)
    suspend fun setAppLanguage(language: String) = userPreferences.setAppLanguage(language)

    // Monitored Apps
    val monitoredApps: Flow<List<MonitoredApp>> = monitoredAppDao.getAll()
    val enabledApps: Flow<List<MonitoredApp>> = monitoredAppDao.getEnabled()

    suspend fun isMonitored(packageName: String): Boolean = monitoredAppDao.isMonitored(packageName)
    suspend fun getMonitoredApp(packageName: String): MonitoredApp? = monitoredAppDao.getByPackage(packageName)
    suspend fun addMonitoredApp(app: MonitoredApp) = monitoredAppDao.insert(app)
    suspend fun updateMonitoredApp(app: MonitoredApp) = monitoredAppDao.update(app)
    suspend fun removeMonitoredApp(packageName: String) = monitoredAppDao.deleteByPackage(packageName)
    suspend fun updateAllCountdownSeconds(seconds: Int) = monitoredAppDao.updateAllCountdownSeconds(seconds)

    // Interventions
    suspend fun recordIntervention(record: InterventionRecord) = interventionDao.insert(record)

    fun getTodayCount(): Flow<Int> = interventionDao.getCountSince(getTodayStart())
    fun getTodaySavedMinutes(): Flow<Int> = interventionDao.getSavedMinutesSince(getTodayStart())
    fun getTodaySuccessRate(): Flow<SuccessRateStat> = interventionDao.getSuccessRateSince(getTodayStart())
    fun getWeeklyStats(): Flow<List<DailyStat>> = interventionDao.getDailyStats(getWeekStart())
    fun getTopApps(): Flow<List<AppStat>> = interventionDao.getTopApps(getWeekStart())
    fun getRecentInterventions(limit: Int = 20): Flow<List<InterventionRecord>> = interventionDao.getRecent(limit)
    fun getTodayInterventions(): Flow<List<InterventionRecord>> = interventionDao.getTodayRecords(getTodayStart())

    private fun getTodayStart(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getWeekStart(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Usage Records
    private fun getTodayDateString(): String = LocalDate.now().toString()

    /**
     * 获取指定应用在指定日期的使用记录
     */
    suspend fun getUsageRecord(packageName: String, date: String): UsageRecord? {
        return usageRecordDao.getRecord(packageName, date)
    }

    /**
     * 更新指定应用的使用分钟数（今日）
     */
    suspend fun updateUsageMinutes(packageName: String, minutes: Int) {
        val today = getTodayDateString()
        val record = UsageRecord(
            packageName = packageName,
            date = today,
            usageMinutes = minutes,
            lastUpdated = System.currentTimeMillis()
        )
        usageRecordDao.upsert(record)
    }

    /**
     * 获取指定应用今日使用时间（Flow）
     */
    fun getTodayUsage(packageName: String): Flow<Int> {
        val today = getTodayDateString()
        return usageRecordDao.getTodayRecords(today).map { records ->
            records.find { it.packageName == packageName }?.usageMinutes ?: 0
        }
    }

    /**
     * 获取指定应用最近N天的使用记录
     */
    fun getRecentUsage(packageName: String, days: Int): Flow<List<UsageRecord>> {
        return usageRecordDao.getRecentRecords(packageName, days)
    }

    /**
     * 设置应用每日使用限额
     */
    suspend fun setDailyLimit(packageName: String, minutes: Int?, mode: String = "soft") {
        val app = monitoredAppDao.getByPackage(packageName) ?: return
        val updatedApp = app.copy(
            dailyLimitMinutes = minutes,
            limitMode = mode
        )
        monitoredAppDao.update(updatedApp)
    }

    /**
     * 同步所有已监控应用的今日使用时长
     * 从 UsageStatsManager 获取并更新到数据库
     */
    suspend fun syncAllMonitoredAppsUsage() = withContext(Dispatchers.IO) {
        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usageStatsManager == null) {
                Log.w(TAG, "UsageStatsManager not available")
                return@withContext
            }

            val apps = monitoredApps.first()
            if (apps.isEmpty()) {
                Log.d(TAG, "No monitored apps to sync")
                return@withContext
            }

            val endTime = System.currentTimeMillis()
            val startTime = getTodayStart()

            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            if (usageEvents == null) {
                Log.d(TAG, "No usage events available")
                return@withContext
            }

            // 收集所有应用的前台时间
            val packageNames = apps.map { it.packageName }.toSet()
            val foregroundTimes = mutableMapOf<String, Long>()
            val lastForegroundTimes = mutableMapOf<String, Long>()

            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.packageName !in packageNames) continue

                when (event.eventType) {
                    @Suppress("DEPRECATION")
                    UsageEvents.Event.MOVE_TO_FOREGROUND,
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        lastForegroundTimes[event.packageName] = event.timeStamp
                    }
                    @Suppress("DEPRECATION")
                    UsageEvents.Event.MOVE_TO_BACKGROUND,
                    UsageEvents.Event.ACTIVITY_PAUSED -> {
                        val lastTime = lastForegroundTimes[event.packageName] ?: 0
                        if (lastTime > 0) {
                            val duration = event.timeStamp - lastTime
                            if (duration > 0) {
                                foregroundTimes[event.packageName] =
                                    (foregroundTimes[event.packageName] ?: 0) + duration
                            }
                            lastForegroundTimes[event.packageName] = 0
                        }
                    }
                }
            }

            // 处理仍在前台的应用
            val currentTime = System.currentTimeMillis()
            for ((pkg, lastTime) in lastForegroundTimes) {
                if (lastTime > 0) {
                    foregroundTimes[pkg] = (foregroundTimes[pkg] ?: 0) + (currentTime - lastTime)
                }
            }

            // 更新数据库
            var syncedCount = 0
            for ((pkg, timeMs) in foregroundTimes) {
                val minutes = (timeMs / 1000 / 60).toInt()
                if (minutes > 0) {
                    updateUsageMinutes(pkg, minutes)
                    syncedCount++
                }
            }

            Log.d(TAG, "Synced usage for $syncedCount apps out of ${apps.size} monitored apps")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync all apps usage: ${e.message}", e)
        }
    }

    /**
     * 同步单个应用的今日使用时长
     */
    suspend fun syncAppUsage(packageName: String) = withContext(Dispatchers.IO) {
        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usageStatsManager == null) {
                Log.w(TAG, "UsageStatsManager not available")
                return@withContext
            }

            val endTime = System.currentTimeMillis()
            val startTime = getTodayStart()

            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            if (usageEvents == null) {
                Log.d(TAG, "No usage events available")
                return@withContext
            }

            var foregroundTimeMs = 0L
            var lastForegroundTime = 0L

            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.packageName != packageName) continue

                when (event.eventType) {
                    @Suppress("DEPRECATION")
                    UsageEvents.Event.MOVE_TO_FOREGROUND,
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        lastForegroundTime = event.timeStamp
                    }
                    @Suppress("DEPRECATION")
                    UsageEvents.Event.MOVE_TO_BACKGROUND,
                    UsageEvents.Event.ACTIVITY_PAUSED -> {
                        if (lastForegroundTime > 0) {
                            val duration = event.timeStamp - lastForegroundTime
                            if (duration > 0) {
                                foregroundTimeMs += duration
                            }
                            lastForegroundTime = 0
                        }
                    }
                }
            }

            if (lastForegroundTime > 0) {
                foregroundTimeMs += System.currentTimeMillis() - lastForegroundTime
            }

            val minutes = (foregroundTimeMs / 1000 / 60).toInt()
            if (minutes > 0) {
                Log.d(TAG, "Synced usage for $packageName: $minutes minutes")
                updateUsageMinutes(packageName, minutes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync usage for $packageName: ${e.message}", e)
        }
    }
}
