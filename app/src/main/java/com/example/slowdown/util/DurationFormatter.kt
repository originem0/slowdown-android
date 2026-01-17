package com.example.slowdown.util

import android.content.Context
import com.example.slowdown.R

/**
 * 格式化分钟数为可读字符串（使用本地化资源）
 *
 * @param context Android Context
 * @param minutes 分钟数
 * @return 格式化后的字符串，如 "1h 30m" 或 "30m"
 */
fun formatDuration(context: Context, minutes: Int): String {
    return when {
        minutes >= 60 -> {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins > 0) {
                context.getString(R.string.duration_hours_minutes, hours, mins)
            } else {
                context.getString(R.string.duration_hours_only, hours)
            }
        }
        else -> context.getString(R.string.duration_minutes_only, minutes)
    }
}

/**
 * 格式化分钟数为可读字符串（旧版兼容，硬编码中文）
 * @deprecated 请使用带 Context 参数的版本
 */
@Deprecated("Use formatDuration(context, minutes) for localized strings")
fun formatDuration(minutes: Int): String {
    return when {
        minutes >= 60 -> {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins > 0) "${hours}小时 ${mins}分钟" else "${hours}小时"
        }
        else -> "${minutes}分钟"
    }
}
