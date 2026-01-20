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

    /**
     * 批量查询多个日期的使用记录（优化N+1查询）
     * @param dates 日期列表（格式：yyyy-MM-dd）
     * @return 所有匹配的记录
     */
    @Query("SELECT * FROM usage_records WHERE date IN (:dates)")
    suspend fun getRecordsByDates(dates: List<String>): List<UsageRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: UsageRecord)

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

    // 数据清理：删除指定日期之前的记录
    @Query("DELETE FROM usage_records WHERE date < :beforeDate")
    suspend fun deleteRecordsBefore(beforeDate: String): Int
}
