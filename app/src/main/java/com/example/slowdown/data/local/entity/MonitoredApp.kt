package com.example.slowdown.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitored_apps")
data class MonitoredApp(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val interventionType: String = "countdown",
    val countdownSeconds: Int = 10,
    val redirectPackage: String? = null,
    val isEnabled: Boolean = true,
    val dailyLimitMinutes: Int? = null,  // 每日限额（分钟），null = 无限制
    val limitMode: String = "soft"        // "soft" = 软提醒, "strict" = 强制关闭
)
