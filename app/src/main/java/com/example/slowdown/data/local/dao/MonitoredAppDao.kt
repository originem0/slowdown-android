package com.sharonZ.slowdown.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sharonZ.slowdown.data.local.entity.MonitoredApp
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoredAppDao {

    @Query("SELECT * FROM monitored_apps")
    fun getAll(): Flow<List<MonitoredApp>>

    @Query("SELECT * FROM monitored_apps WHERE isEnabled = 1")
    fun getEnabled(): Flow<List<MonitoredApp>>

    @Query("SELECT * FROM monitored_apps WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): MonitoredApp?

    @Query("SELECT EXISTS(SELECT 1 FROM monitored_apps WHERE packageName = :packageName AND isEnabled = 1)")
    suspend fun isMonitored(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: MonitoredApp)

    @Update
    suspend fun update(app: MonitoredApp)

    @Delete
    suspend fun delete(app: MonitoredApp)

    @Query("DELETE FROM monitored_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("UPDATE monitored_apps SET countdownSeconds = :seconds")
    suspend fun updateAllCountdownSeconds(seconds: Int)
}
