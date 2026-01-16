package com.example.slowdown

import android.app.Application
import android.util.Log
import com.example.slowdown.data.local.AppDatabase
import com.example.slowdown.data.preferences.UserPreferences
import com.example.slowdown.data.repository.SlowDownRepository
import com.example.slowdown.util.MiuiHelper
import com.example.slowdown.util.NotificationHelper

class SlowDownApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val userPreferences: UserPreferences by lazy { UserPreferences(this) }
    val repository: SlowDownRepository by lazy {
        SlowDownRepository(
            interventionDao = database.interventionDao(),
            monitoredAppDao = database.monitoredAppDao(),
            usageRecordDao = database.usageRecordDao(),
            userPreferences = userPreferences
        )
    }

    override fun attachBaseContext(base: android.content.Context?) {
        super.attachBaseContext(base)
        // 在 MIUI 设备上尝试解除反射限制
        if (MiuiHelper.isMiui()) {
            MiuiHelper.tryUnsealReflection()
        }
    }

    override fun onCreate() {
        super.onCreate()
        // 创建通知频道（Full-Screen Intent 需要）
        NotificationHelper.createNotificationChannel(this)
    }
}
