package com.example.slowdown.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.slowdown.data.local.entity.InterventionRecord
import kotlinx.coroutines.flow.Flow

data class DailyStat(
    val day: String,
    val count: Int
)

data class AppStat(
    val appName: String,
    val count: Int
)

@Dao
interface InterventionDao {

    @Insert
    suspend fun insert(record: InterventionRecord)

    @Query("SELECT COUNT(*) FROM intervention_records WHERE timestamp >= :startTime")
    fun getCountSince(startTime: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) * 5 FROM intervention_records
        WHERE timestamp >= :startTime
        AND userChoice != 'continued'
    """)
    fun getSavedMinutesSince(startTime: Long): Flow<Int>

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

    @Query("SELECT * FROM intervention_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<InterventionRecord>>
}
