package com.sharonZ.slowdown.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sharonZ.slowdown.data.local.entity.InterventionRecord
import kotlinx.coroutines.flow.Flow

data class DailyStat(
    val day: String,
    val count: Int
)

data class AppStat(
    val appName: String,
    val count: Int
)

data class SuccessRateStat(
    val total: Int,
    val successful: Int
)

data class HourlyStat(
    val hour: Int,
    val count: Int
)

@Dao
interface InterventionDao {

    @Insert
    suspend fun insert(record: InterventionRecord)

    // 平均决策时间（秒）- 过滤异常值（1-60秒范围）
    @Query("""
        SELECT AVG(actualWaitTime)
        FROM intervention_records
        WHERE timestamp >= :startTime
          AND actualWaitTime >= 1
          AND actualWaitTime <= 60
    """)
    fun getAverageDecisionTime(startTime: Long): Flow<Float?>

    // 时段分布（按小时分组）
    @Query("""
        SELECT
            CAST(strftime('%H', datetime(timestamp/1000, 'unixepoch', 'localtime')) AS INTEGER) as hour,
            COUNT(*) as count
        FROM intervention_records
        WHERE timestamp >= :startTime
        GROUP BY hour
    """)
    fun getHourlyDistribution(startTime: Long): Flow<List<HourlyStat>>

    @Query("SELECT COUNT(*) FROM intervention_records WHERE timestamp >= :startTime")
    fun getCountSince(startTime: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) * 5 FROM intervention_records
        WHERE timestamp >= :startTime
        AND userChoice != 'continued'
    """)
    fun getSavedMinutesSince(startTime: Long): Flow<Int>

    @Query("""
        SELECT
            COUNT(*) as total,
            SUM(CASE WHEN userChoice != 'continued' THEN 1 ELSE 0 END) as successful
        FROM intervention_records
        WHERE timestamp >= :startTime
    """)
    fun getSuccessRateSince(startTime: Long): Flow<SuccessRateStat>

    @Query("""
        SELECT date(timestamp/1000, 'unixepoch', 'localtime') as day, COUNT(*) as count
        FROM intervention_records
        WHERE timestamp >= :startTime
        GROUP BY day
        ORDER BY day
    """)
    fun getDailyStats(startTime: Long): Flow<List<DailyStat>>

    @Query("""
        SELECT appName, COUNT(*) as count
        FROM intervention_records
        WHERE timestamp >= :startTime
        GROUP BY packageName
        ORDER BY count DESC
        LIMIT 5
    """)
    fun getTopApps(startTime: Long): Flow<List<AppStat>>

    @Query("""
        SELECT date(timestamp/1000, 'unixepoch', 'localtime') as day, COUNT(*) as count
        FROM intervention_records
        WHERE timestamp >= :startTime AND timestamp < :endTime
        GROUP BY day
        ORDER BY day
    """)
    suspend fun getDailyStatsBetween(startTime: Long, endTime: Long): List<DailyStat>

    @Query("SELECT * FROM intervention_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<InterventionRecord>>

    @Query("SELECT * FROM intervention_records WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getTodayRecords(startTime: Long): Flow<List<InterventionRecord>>

    // 月度统计：指定日期范围的拦截次数
    @Query("SELECT COUNT(*) FROM intervention_records WHERE timestamp >= :startTime AND timestamp < :endTime")
    suspend fun getCountBetween(startTime: Long, endTime: Long): Int

    // 月度统计：指定日期范围的成功率
    @Query("""
        SELECT
            COUNT(*) as total,
            SUM(CASE WHEN userChoice != 'continued' THEN 1 ELSE 0 END) as successful
        FROM intervention_records
        WHERE timestamp >= :startTime AND timestamp < :endTime
    """)
    suspend fun getSuccessRateBetween(startTime: Long, endTime: Long): SuccessRateStat

    // 数据清理：删除指定时间之前的记录
    @Query("DELETE FROM intervention_records WHERE timestamp < :beforeTimestamp")
    suspend fun deleteRecordsBefore(beforeTimestamp: Long): Int
}
