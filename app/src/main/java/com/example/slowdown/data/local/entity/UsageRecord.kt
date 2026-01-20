package com.sharonZ.slowdown.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "usage_records",
    primaryKeys = ["packageName", "date"]
)
data class UsageRecord(
    val packageName: String,
    val date: String,              // "yyyy-MM-dd" 格式
    val usageMinutes: Int = 0,     // 当日累计使用分钟
    val lastUpdated: Long = 0      // 最后更新时间戳
)
