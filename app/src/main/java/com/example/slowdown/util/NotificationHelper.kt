package com.example.slowdown.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.slowdown.MainActivity
import com.example.slowdown.R
import com.example.slowdown.service.UsageWarningType
import com.example.slowdown.ui.overlay.OverlayActivity

/**
 * 通知帮助类 - 使用 Full-Screen Intent 实现类似闹钟的全屏弹窗
 *
 * 注意：此方法主要作为备用方案使用。
 * 主要启动方式是直接启动 Activity（launchOverlayDirectly），
 * 只有在直接启动失败时才会回退到 Full-Screen Intent。
 *
 * 工作原理：
 * 1. 如果设备锁屏或屏幕关闭 -> 直接启动全屏 Activity
 * 2. 如果设备正在使用 -> 显示高优先级 heads-up 通知
 */
object NotificationHelper {

    private const val TAG = "SlowDown"
    private const val CHANNEL_ID = "slowdown_intervention"
    private const val CHANNEL_NAME = "应用干预"
    private const val NOTIFICATION_ID = 1001

    // 前台服务通知 - 防止 MIUI 冻结 AccessibilityService
    private const val FOREGROUND_CHANNEL_ID = "slowdown_foreground"
    private const val FOREGROUND_CHANNEL_NAME = "后台运行"
    const val FOREGROUND_NOTIFICATION_ID = 1002

    // 使用时间警告通知
    private const val USAGE_WARNING_CHANNEL_ID = "slowdown_usage_warning"
    private const val USAGE_WARNING_CHANNEL_NAME = "使用时间提醒"
    private const val USAGE_WARNING_NOTIFICATION_ID = 1003

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "用于在打开监控应用时显示干预界面"
            setShowBadge(false)
            enableVibration(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }

        // 前台服务通知渠道 - 低优先级，静音
        val foregroundChannel = NotificationChannel(
            FOREGROUND_CHANNEL_ID,
            FOREGROUND_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "保持应用在后台运行，监控应用启动"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        // 使用时间警告通知渠道
        val usageWarningChannel = NotificationChannel(
            USAGE_WARNING_CHANNEL_ID,
            USAGE_WARNING_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "提醒应用使用时间达到限额"
            setShowBadge(true)
            enableVibration(true)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        notificationManager.createNotificationChannel(foregroundChannel)
        notificationManager.createNotificationChannel(usageWarningChannel)
        Log.d(TAG, "[NotificationHelper] Notification channels created")
    }

    /**
     * 发送带有 Full-Screen Intent 的通知（备用方案）
     *
     * 此方法主要在直接启动 Activity 失败时使用。
     *
     * 行为：
     * - 锁屏时：直接弹出全屏界面
     * - 亮屏时：显示 heads-up 通知，点击后打开界面
     */
    fun showInterventionNotification(
        context: Context,
        packageName: String,
        appName: String,
        countdownSeconds: Int,
        redirectPackage: String?,
        isLimitReached: Boolean = false,
        usedMinutes: Int = 0,
        limitMinutes: Int = 0
    ) {
        Log.d(TAG, "[NotificationHelper] showInterventionNotification for $appName, isLimitReached=$isLimitReached")

        // 创建打开 OverlayActivity 的 Intent
        val fullScreenIntent = Intent(context, OverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(OverlayActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(OverlayActivity.EXTRA_APP_NAME, appName)
            putExtra(OverlayActivity.EXTRA_COUNTDOWN_SECONDS, countdownSeconds)
            putExtra(OverlayActivity.EXTRA_REDIRECT_PACKAGE, redirectPackage)
            putExtra(OverlayActivity.EXTRA_IS_LIMIT_REACHED, isLimitReached)
            putExtra(OverlayActivity.EXTRA_USED_MINUTES, usedMinutes)
            putExtra(OverlayActivity.EXTRA_LIMIT_MINUTES, limitMinutes)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(), // 唯一的 requestCode
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建通知 - 根据是否达到限额显示不同文案
        val contentText = if (isLimitReached) {
            "$appName 已达到今日限额"
        } else {
            "你正在打开 $appName"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("慢下来")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM) // 关键：使用闹钟类别
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(true) // 持续通知，防止被滑掉
            // 关键：设置 Full-Screen Intent
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "[NotificationHelper] Notification with full-screen intent sent")
    }

    /**
     * 创建前台服务通知 - 用于保持 AccessibilityService 活跃
     * 这是防止 MIUI 冻结服务的关键
     */
    fun buildForegroundNotification(context: Context): Notification {
        // 确保通知渠道已创建
        createNotificationChannel(context)

        // 点击通知打开主界面
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, FOREGROUND_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("SlowDown 正在运行")
            .setContentText("监控应用启动中...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun cancelNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    /**
     * 检查是否有全屏通知权限（Android 11+）
     */
    fun canUseFullScreenIntent(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ 需要检查权限
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.canUseFullScreenIntent()
        } else {
            // Android 11-13 默认允许
            true
        }
    }

    /**
     * 打开全屏通知权限设置（Android 14+）
     */
    fun openFullScreenIntentSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "[NotificationHelper] Failed to open full-screen intent settings: ${e.message}")
            }
        }
    }

    /**
     * 显示使用时间警告通知
     */
    fun showUsageWarningNotification(
        context: Context,
        packageName: String,
        appName: String,
        warningType: UsageWarningType
    ) {
        Log.d(TAG, "[NotificationHelper] showUsageWarningNotification for $appName, type: $warningType")

        val (title, content) = when (warningType) {
            UsageWarningType.SOFT_REMINDER -> {
                "深呼吸提醒" to "$appName 已使用 80% 的每日限额，请注意休息"
            }
            UsageWarningType.LIMIT_REACHED_SOFT -> {
                "已达每日限额" to "$appName 今日使用时间已达限额"
            }
            UsageWarningType.LIMIT_REACHED_STRICT -> {
                "已达每日限额（强制）" to "$appName 今日使用时间已达限额，应用已关闭"
            }
        }

        // 点击通知打开主界面
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, USAGE_WARNING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 使用包名的哈希值作为通知 ID，这样同一个应用的警告会更新而不是累加
        val notificationId = USAGE_WARNING_NOTIFICATION_ID + packageName.hashCode() % 1000
        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "[NotificationHelper] Usage warning notification sent with id: $notificationId")
    }
}
