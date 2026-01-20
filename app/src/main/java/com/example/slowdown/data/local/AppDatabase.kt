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
    version = 6,
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

        // 从版本2迁移到版本3：添加 isVideoApp 字段（短视频模式）
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加 isVideoApp 字段到 monitored_apps，默认值为 false (0)
                database.execSQL("""
                    ALTER TABLE monitored_apps ADD COLUMN isVideoApp INTEGER NOT NULL DEFAULT 0
                """.trimIndent())
            }
        }

        // 从版本3迁移到版本4：添加 cooldownMinutes 字段（单独冷却时间）
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加 cooldownMinutes 字段到 monitored_apps，null = 使用全局设置
                database.execSQL("""
                    ALTER TABLE monitored_apps ADD COLUMN cooldownMinutes INTEGER DEFAULT NULL
                """.trimIndent())
            }
        }

        // 从版本4迁移到版本5：添加性能优化索引
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 为 intervention_records 表添加索引，优化时间范围查询性能
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_intervention_timestamp
                    ON intervention_records(timestamp)
                """.trimIndent())

                // 为 intervention_records 表添加复合索引，优化按应用查询性能
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_intervention_package_time
                    ON intervention_records(packageName, timestamp)
                """.trimIndent())
            }
        }

        // 从版本5迁移到版本6：修复索引缺失问题（幂等操作）
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 确保索引存在（IF NOT EXISTS 保证幂等性）
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_intervention_timestamp
                    ON intervention_records(timestamp)
                """.trimIndent())

                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_intervention_package_time
                    ON intervention_records(packageName, timestamp)
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)  // 降级时重建数据库，避免崩溃
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
