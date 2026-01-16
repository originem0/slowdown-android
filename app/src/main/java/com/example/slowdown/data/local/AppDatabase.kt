package com.example.slowdown.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.slowdown.data.local.dao.InterventionDao
import com.example.slowdown.data.local.dao.MonitoredAppDao
import com.example.slowdown.data.local.entity.InterventionRecord
import com.example.slowdown.data.local.entity.MonitoredApp

@Database(
    entities = [InterventionRecord::class, MonitoredApp::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun interventionDao(): InterventionDao
    abstract fun monitoredAppDao(): MonitoredAppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "slowdown_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
