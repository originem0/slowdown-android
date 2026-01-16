package com.example.slowdown.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.slowdown.data.local.entity.UsageRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageRecordDao {

    @Query("SELECT * FROM usage_records WHERE packageName = :packageName AND date = :date")
    suspend fun getRecord(packageName: String, date: String): UsageRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: UsageRecord)

    @Query("""
        SELECT * FROM usage_records
        WHERE packageName = :packageName
        ORDER BY date DESC
        LIMIT :days
    """)
    fun getRecentRecords(packageName: String, days: Int): Flow<List<UsageRecord>>

    @Query("SELECT * FROM usage_records WHERE date = :date")
    fun getTodayRecords(date: String): Flow<List<UsageRecord>>

    @Query("SELECT COALESCE(SUM(usageMinutes), 0) FROM usage_records WHERE date = :date")
    suspend fun getTotalUsageForDate(date: String): Int
}
