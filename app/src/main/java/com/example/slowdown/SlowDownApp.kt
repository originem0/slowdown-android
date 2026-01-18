package com.example.slowdown

import android.app.Application
import android.util.Log
import com.example.slowdown.data.local.AppDatabase
import com.example.slowdown.data.preferences.UserPreferences
import com.example.slowdown.data.repository.SlowDownRepository
import com.example.slowdown.util.MiuiHelper
import com.example.slowdown.util.NotificationHelper

class SlowDownApp : Application() {

    companion object {
        private const val TAG = "SlowDownApp"
    }

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val userPreferences: UserPreferences by lazy { UserPreferences(this) }
    val repository: SlowDownRepository by lazy {
        SlowDownRepository(
            interventionDao = database.interventionDao(),
            monitoredAppDao = database.monitoredAppDao(),
            usageRecordDao = database.usageRecordDao(),
            userPreferences = userPreferences,
            context = this
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

        // Setup global exception handler for crash logging
        setupExceptionHandler()

        // 创建通知频道（Full-Screen Intent 需要）
        NotificationHelper.createNotificationChannel(this)
    }

    /**
     * Setup global uncaught exception handler.
     * This logs crashes for debugging and allows graceful degradation.
     */
    private fun setupExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "===== UNCAUGHT EXCEPTION =====")
            Log.e(TAG, "Thread: ${thread.name}")
            Log.e(TAG, "Exception: ${throwable.javaClass.simpleName}")
            Log.e(TAG, "Message: ${throwable.message}")
            Log.e(TAG, "Stack trace:", throwable)

            // Log cause chain
            var cause = throwable.cause
            var depth = 0
            while (cause != null && depth < 5) {
                Log.e(TAG, "Caused by: ${cause.javaClass.simpleName}: ${cause.message}")
                cause = cause.cause
                depth++
            }

            Log.e(TAG, "==============================")

            // Pass to default handler (which will crash the app, but we've logged it)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
