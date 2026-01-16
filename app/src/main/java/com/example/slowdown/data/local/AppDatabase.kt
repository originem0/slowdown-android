package com.example.slowdown.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.slowdown.data.local.dao.InterventionDao
import com.example.slowdown.data.local.dao.MonitoredAppDao
import com.example.slowdown.data.local.dao.UsageRecordDao
import com.example.slowdown.data.local.entity.InterventionRecord
import com.example.slowdown.data.local.entity.MonitoredApp
import com.example.slowdown.data.local.entity.UsageRecord

@Database(
    entities = [InterventionRecord::class, MonitoredApp::class, UsageRecord::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun interventionDao(): InterventionDao
    abstract fun monitoredAppDao(): MonitoredAppDao
    abstract fun usageRecordDao(): UsageRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 从版本1迁移到版本2：添加 usage_records 表和 monitored_apps 新字段
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建 usage_records 表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS usage_records (
                        packageName TEXT NOT NULL,
                        date TEXT NOT NULL,
                        usageMinutes INTEGER NOT NULL DEFAULT 0,
                        lastUpdated INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(packageName, date)
                    )
                """.trimIndent())

                // 添加 dailyLimitMinutes 字段到 monitored_apps
                database.execSQL("""
                    ALTER TABLE monitored_apps ADD COLUMN dailyLimitMinutes INTEGER DEFAULT NULL
                """.trimIndent())

                // 添加 limitMode 字段到 monitored_apps
                database.execSQL("""
                    ALTER TABLE monitored_apps ADD COLUMN limitMode TEXT NOT NULL DEFAULT 'soft'
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "slowdown_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
