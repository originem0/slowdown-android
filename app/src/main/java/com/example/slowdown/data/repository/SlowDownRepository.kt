package com.example.slowdown.data.repository

import com.example.slowdown.data.local.dao.AppStat
import com.example.slowdown.data.local.dao.DailyStat
import com.example.slowdown.data.local.dao.InterventionDao
import com.example.slowdown.data.local.dao.MonitoredAppDao
import com.example.slowdown.data.local.dao.UsageRecordDao
import com.example.slowdown.data.local.entity.InterventionRecord
import com.example.slowdown.data.local.entity.MonitoredApp
import com.example.slowdown.data.local.entity.UsageRecord
import com.example.slowdown.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.Calendar

class SlowDownRepository(
    private val interventionDao: InterventionDao,
    private val monitoredAppDao: MonitoredAppDao,
    private val usageRecordDao: UsageRecordDao,
    private val userPreferences: UserPreferences
) {
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
    fun getWeeklyStats(): Flow<List<DailyStat>> = interventionDao.getDailyStats(getWeekStart())
    fun getTopApps(): Flow<List<AppStat>> = interventionDao.getTopApps(getWeekStart())
    fun getRecentInterventions(limit: Int = 20): Flow<List<InterventionRecord>> = interventionDao.getRecent(limit)

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
}
