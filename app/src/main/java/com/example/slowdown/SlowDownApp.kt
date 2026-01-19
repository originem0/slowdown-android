package com.example.slowdown

import android.app.Application
import android.util.Log
import com.example.slowdown.data.local.AppDatabase
import com.example.slowdown.data.preferences.UserPreferences
import com.example.slowdown.data.repository.SlowDownRepository
import com.example.slowdown.util.MiuiHelper
import com.example.slowdown.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    // 缓存语言设置，避免 Activity 启动时阻塞 I/O
    // 使用 @Volatile 确保多线程可见性
    @Volatile
    var cachedLanguage: String = "en"
        private set

    // Application 级别的协程作用域
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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

        // 异步预加载语言设置，后续 Activity 可直接使用缓存
        applicationScope.launch {
            try {
                cachedLanguage = userPreferences.appLanguage.first()
                Log.d(TAG, "[Language Cache] Initial language loaded: $cachedLanguage")

                // 持续监听语言变化并更新缓存
                userPreferences.appLanguage.collect { newLanguage ->
                    cachedLanguage = newLanguage
                    Log.d(TAG, "[Language Cache] Language updated: $newLanguage")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[Language Cache] Failed to load language, using default", e)
                cachedLanguage = "en"
            }
        }
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
