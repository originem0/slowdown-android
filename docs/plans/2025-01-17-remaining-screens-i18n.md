# 剩余界面国际化实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将 DashboardScreen、AppListScreen、StatisticsScreen、AppDetailScreen 中的所有硬编码字符串替换为 stringResource，实现完整的中英文切换

**Architecture:** 逐屏幕修改，每个屏幕完成后编译验证

**Tech Stack:** Android Jetpack Compose, stringResource(), values/strings.xml, values-zh/strings.xml

---

## 问题诊断

### 当前状态
- ✅ SettingsScreen.kt - 已完成国际化
- ❌ DashboardScreen.kt - 英文硬编码字符串
- ❌ AppListScreen.kt - 英文硬编码字符串
- ❌ StatisticsScreen.kt - 中文硬编码字符串
- ❌ AppDetailScreen.kt - 大量中文硬编码字符串

---

## Task 1: 添加所有需要的新字符串资源

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh/strings.xml`

**Step 1:** 在 `values/strings.xml` 文件末尾 `</resources>` 之前添加以下内容：

```xml
    <!-- Dashboard Screen -->
    <string name="setup_required">Setup required to enable protection</string>
    <string name="miui_permissions_needed">MIUI permissions needed for background</string>

    <!-- Statistics Screen -->
    <string name="weekly_trend">Weekly Trend</string>
    <string name="app_details">App Details</string>
    <string name="no_usage_today">No usage records today</string>
    <string name="compared_to_yesterday_more">%s more than yesterday</string>
    <string name="compared_to_yesterday_less">%s less than yesterday</string>
    <string name="this_month">This Month</string>
    <string name="month_total_usage">Monthly Total Usage</string>

    <!-- App List Screen -->
    <string name="protected_apps_count">Protected Apps (%d)</string>
    <string name="available_apps_count">Available Apps (%d)</string>

    <!-- App Detail Screen -->
    <string name="app_settings">App Settings</string>
    <string name="back">Back</string>
    <string name="today_usage">Today\'s Usage</string>
    <string name="restriction_mode">Restriction Mode</string>
    <string name="other_settings">Other Settings</string>
    <string name="tracking_only_title">Tracking Only</string>
    <string name="tracking_only_desc">Only record usage time, no intervention</string>
    <string name="gentle_reminder_title">Gentle Reminder</string>
    <string name="gentle_reminder_desc">Remind when limit reached, can continue using</string>
    <string name="gentle_reminder_with_limit">Remind after %d minutes daily, can continue</string>
    <string name="gentle_reminder_every_open">Deep breathing reminder on each open</string>
    <string name="strict_limit_title">Strict Limit</string>
    <string name="strict_limit_desc">Block usage after limit reached today</string>
    <string name="strict_limit_with_limit">Block after %d minutes daily</string>
    <string name="completely_blocked_title">Completely Blocked</string>
    <string name="completely_blocked_desc">Blocked immediately when opened</string>
    <string name="video_mode">Short Video Mode</string>
    <string name="video_mode_enabled">Enabled: Check every 30 seconds</string>
    <string name="video_mode_desc">For TikTok, YouTube Shorts, etc.</string>
    <string name="redirect_app">Redirect App</string>
    <string name="redirect_app_desc">Open specified app during intervention</string>
    <string name="none">None</string>
    <string name="set_daily_limit">Set Daily Limit</string>
    <string name="no_limit">No Limit</string>
    <string name="no_limit_remind_every_open">No limit (remind on each open)</string>
    <string name="minutes_format">%d minutes</string>
    <string name="hour_format">%d hour</string>
    <string name="hours_format">%d hours</string>
    <string name="custom">Custom</string>
    <string name="enter_minutes">Enter minutes</string>
    <string name="minutes_label">minutes</string>
    <string name="confirm">Confirm</string>
    <string name="minute_unit">min</string>
    <string name="minutes_per_day">min/day</string>
    <string name="weekly_average">Weekly Average</string>
    <string name="select_redirect_app">Select Redirect App</string>
    <string name="search_apps_hint">Search apps...</string>
    <string name="no_redirect">None (no redirect)</string>
    <string name="no_apps_found">No matching apps found</string>
    <string name="input_range_error">Please enter a number between 1-1440</string>
    <string name="already_blocked_mode">Completely blocked mode selected</string>
    <string name="soft_reminder">Soft Reminder</string>
    <string name="soft_reminder_desc">Remind after timeout, but allow continue</string>
    <string name="force_close">Force Close</string>
    <string name="force_close_desc">Cannot use this app today after timeout</string>
    <string name="completely_blocked_simple">Completely Blocked</string>
    <string name="blocked_on_open">Blocked when opening</string>
```

**Step 2:** 在 `values-zh/strings.xml` 文件末尾 `</resources>` 之前添加对应的中文内容：

```xml
    <!-- Dashboard Screen -->
    <string name="setup_required">需要完成设置才能启用保护</string>
    <string name="miui_permissions_needed">MIUI 需要配置后台权限</string>

    <!-- Statistics Screen -->
    <string name="weekly_trend">本周趋势</string>
    <string name="app_details">各应用详情</string>
    <string name="no_usage_today">当日暂无使用记录</string>
    <string name="compared_to_yesterday_more">比昨天多%s</string>
    <string name="compared_to_yesterday_less">比昨天少%s</string>
    <string name="this_month">本月</string>
    <string name="month_total_usage">本月总计使用时间</string>

    <!-- App List Screen -->
    <string name="protected_apps_count">已监控 (%d)</string>
    <string name="available_apps_count">可添加 (%d)</string>

    <!-- App Detail Screen -->
    <string name="app_settings">应用设置</string>
    <string name="back">返回</string>
    <string name="today_usage">今日使用</string>
    <string name="restriction_mode">限制模式</string>
    <string name="other_settings">其他设置</string>
    <string name="tracking_only_title">仅统计</string>
    <string name="tracking_only_desc">只记录使用时间，不做任何干预</string>
    <string name="gentle_reminder_title">温和提醒</string>
    <string name="gentle_reminder_desc">达到限额后提醒，但可继续使用</string>
    <string name="gentle_reminder_with_limit">每日 %d 分钟后提醒，可继续使用</string>
    <string name="gentle_reminder_every_open">每次打开应用时深呼吸提醒</string>
    <string name="strict_limit_title">严格限制</string>
    <string name="strict_limit_desc">达到限额后今日禁止继续使用</string>
    <string name="strict_limit_with_limit">每日 %d 分钟后禁止使用</string>
    <string name="completely_blocked_title">完全禁止</string>
    <string name="completely_blocked_desc">打开应用即被阻止，无法使用</string>
    <string name="video_mode">短视频模式</string>
    <string name="video_mode_enabled">已启用：每 30 秒检查一次</string>
    <string name="video_mode_desc">适用于抖音、B站等刷视频应用</string>
    <string name="redirect_app">跳转应用</string>
    <string name="redirect_app_desc">干预时跳转到指定应用</string>
    <string name="none">无</string>
    <string name="set_daily_limit">设置每日限额</string>
    <string name="no_limit">无限制</string>
    <string name="no_limit_remind_every_open">无限制（每次打开都提醒）</string>
    <string name="minutes_format">%d 分钟</string>
    <string name="hour_format">%d 小时</string>
    <string name="hours_format">%d 小时</string>
    <string name="custom">自定义</string>
    <string name="enter_minutes">分钟数</string>
    <string name="minutes_label">分钟</string>
    <string name="confirm">确定</string>
    <string name="minute_unit">分钟</string>
    <string name="minutes_per_day">分钟/天</string>
    <string name="weekly_average">本周平均</string>
    <string name="select_redirect_app">选择跳转应用</string>
    <string name="search_apps_hint">搜索应用...</string>
    <string name="no_redirect">无（不跳转）</string>
    <string name="no_apps_found">未找到匹配的应用</string>
    <string name="input_range_error">请输入 1-1440 之间的数字</string>
    <string name="already_blocked_mode">已选择完全禁止模式</string>
    <string name="soft_reminder">软提醒</string>
    <string name="soft_reminder_desc">超时后提醒，但允许继续使用</string>
    <string name="force_close">强制关闭</string>
    <string name="force_close_desc">超时后今日无法再使用该应用</string>
    <string name="completely_blocked_simple">完全禁止</string>
    <string name="blocked_on_open">打开应用即被阻止</string>
```

**Verification:** 运行 `./gradlew assembleDebug` 确保无编译错误

---

## Task 2: DashboardScreen 国际化

**Files:**
- Modify: `app/src/main/java/com/example/slowdown/ui/screen/DashboardScreen.kt`

**Step 1:** 在文件顶部添加 import

```kotlin
import androidx.compose.ui.res.stringResource
import com.sharonZ.slowdown.R
```

**Step 2:** 替换 AlertBanner 中的硬编码字符串

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 87 | `"Setup required to enable protection"` | `stringResource(R.string.setup_required)` |
| 97 | `"MIUI permissions needed for background"` | `stringResource(R.string.miui_permissions_needed)` |

**Step 3:** 替换 SectionTitle 中的硬编码字符串

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 113 | `"Weekly Mindful Moments"` | `stringResource(R.string.weekly_mindful_moments)` |
| 133 | `"Most Intercepted"` | `stringResource(R.string.most_intercepted)` |

**Step 4:** 替换 "View All Apps"

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 164 | `"View All Apps"` | `stringResource(R.string.view_all_apps)` |

**Step 5:** 替换 StatusHeader 中的字符串

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 198 | `"Protection Active"` | `stringResource(R.string.protection_active)` |
| 198 | `"Protection Paused"` | `stringResource(R.string.protection_paused)` |

完整代码：
```kotlin
Text(
    text = if (serviceEnabled) stringResource(R.string.protection_active) else stringResource(R.string.protection_paused),
    ...
)
```

**Step 6:** 替换 StatsOverview 中的标签

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 240 | `"Intercepts"` | `stringResource(R.string.intercepts)` |
| 254 | `"Min Saved"` | `stringResource(R.string.min_saved)` |

**Step 7:** 替换 BreathingCircle 中的 "Today"

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 340 | `"Today"` | `stringResource(R.string.today)` |

**Step 8:** 替换 WeeklyTrendSection 中的字符串

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 359 | `"No data yet"` | `stringResource(R.string.no_data_yet)` |
| 399 | `"Weekly Total"` | `stringResource(R.string.weekly_total)` |
| 404 | `"$weekTotal interrupts"` | `stringResource(R.string.interrupts).let { "$weekTotal $it" }` |

**Verification:** 运行 `./gradlew assembleDebug` 确保无编译错误

---

## Task 3: AppListScreen 国际化

**Files:**
- Modify: `app/src/main/java/com/example/slowdown/ui/screen/AppListScreen.kt`

**Step 1:** 在文件顶部添加 import（如果没有）

```kotlin
import androidx.compose.ui.res.stringResource
import com.sharonZ.slowdown.R
```

**Step 2:** 替换 SectionTitle 字符串

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 119 | `"Protected Apps (${monitoredList.size})"` | `stringResource(R.string.protected_apps_count, monitoredList.size)` |
| 140 | `"Available Apps (${unmonitoredList.size})"` | `stringResource(R.string.available_apps_count, unmonitoredList.size)` |

**Step 3:** 替换 AlertDialog 字符串

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 160 | `"Remove Protection?"` | `stringResource(R.string.remove_protection)` |
| 161 | `"Are you sure you want to stop monitoring \"${app.appName}\"?"` | `stringResource(R.string.remove_protection_confirm, app.appName)` |
| 172 | `"Remove"` | `stringResource(R.string.remove)` |
| 177 | `"Cancel"` | `stringResource(R.string.cancel)` |

**Step 4:** 替换 ModernSearchBar placeholder

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 199 | `"Search apps..."` | `stringResource(R.string.search_apps)` |

**Step 5:** 替换 StatsCard 标签

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 232 | `"Protected"` | `stringResource(R.string.label_protected)` |
| 239 | `"Available"` | `stringResource(R.string.label_available)` |

**Step 6:** 替换 MonitoredAppItem 模式文字

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 320 | `"Tracking Only"` | `stringResource(R.string.tracking_only)` |
| 321 | `"Strict Mode"` | `stringResource(R.string.strict_mode)` |
| 322 | `"Gentle Mode"` | `stringResource(R.string.gentle_mode)` |

完整代码：
```kotlin
val modeText = when {
    monitoredApp?.isEnabled != true -> stringResource(R.string.tracking_only)
    monitoredApp.limitMode == "strict" -> stringResource(R.string.strict_mode)
    else -> stringResource(R.string.gentle_mode)
}
```

**Verification:** 运行 `./gradlew assembleDebug` 确保无编译错误

---

## Task 4: StatisticsScreen 国际化

**Files:**
- Modify: `app/src/main/java/com/example/slowdown/ui/screen/StatisticsScreen.kt`

**Step 1:** 在文件顶部添加 import

```kotlin
import androidx.compose.ui.res.stringResource
import com.sharonZ.slowdown.R
```

**Step 2:** 替换 SectionTitle 字符串

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 82 | `"本周趋势"` | `stringResource(R.string.weekly_trend)` |
| 94 | `"各应用详情"` | `stringResource(R.string.app_details)` |
| 118 | `"本月"` | `stringResource(R.string.this_month)` |

**Step 3:** 替换 EmptyState 消息

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 100 | `"当日暂无使用记录"` | `stringResource(R.string.no_usage_today)` |
| 264 | `"暂无数据"` | `stringResource(R.string.no_data_yet)` |

**Step 4:** 修改 TodayCircularSection 中的比较文字

需要修改 `comparisonText` 的生成逻辑，改用 stringResource：

```kotlin
// 在 TodayCircularSection 函数签名添加参数，或者直接内联使用
val comparisonText = if (diff != 0) {
    if (isIncrease) {
        stringResource(R.string.compared_to_yesterday_more, formatDuration(abs(diff)))
    } else {
        stringResource(R.string.compared_to_yesterday_less, formatDuration(abs(diff)))
    }
} else null
```

**注意：** 由于 Composable 函数中使用 stringResource 需要在 @Composable 上下文中，需要将此逻辑移到正确的位置。

**Step 5:** 替换 WeeklyBarSection 中的 "本周总计"

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 295 | `"本周总计"` | `stringResource(R.string.weekly_total)` |

**Step 6:** 替换 MonthSection 中的 "本月总计使用时间"

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 427 | `"本月总计使用时间"` | `stringResource(R.string.month_total_usage)` |

**Verification:** 运行 `./gradlew assembleDebug` 确保无编译错误

---

## Task 5: AppDetailScreen 国际化 - Part 1 (TopAppBar 和基础组件)

**Files:**
- Modify: `app/src/main/java/com/example/slowdown/ui/screen/AppDetailScreen.kt`

**Step 1:** 在文件顶部添加 import

```kotlin
import androidx.compose.ui.res.stringResource
import com.sharonZ.slowdown.R
```

**Step 2:** 替换 TopAppBar 中的字符串

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 74 | `"应用设置"` | `stringResource(R.string.app_settings)` |
| 80 | `contentDescription = "返回"` | `contentDescription = stringResource(R.string.back)` |

完整代码：
```kotlin
TopAppBar(
    title = {
        Text(
            text = app?.appName ?: stringResource(R.string.app_settings),
            fontWeight = FontWeight.SemiBold
        )
    },
    navigationIcon = {
        IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }
    },
    ...
)
```

**Step 3:** 替换 SectionTitle 字符串

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 113 | `"今日使用"` | `stringResource(R.string.today_usage)` |
| 127 | `"限制模式"` | `stringResource(R.string.restriction_mode)` |
| 177 | `"其他设置"` | `stringResource(R.string.other_settings)` |

**Verification:** 运行 `./gradlew assembleDebug` 确保无编译错误

---

## Task 6: AppDetailScreen 国际化 - Part 2 (UsageStatsSection)

**Files:**
- Modify: `app/src/main/java/com/example/slowdown/ui/screen/AppDetailScreen.kt`

**Step 1:** 修改 UsageStatsSection 中的标签

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 272 | `"$todayUsage 分钟"` | 见下方代码 |
| 273 | `"今日使用"` | `stringResource(R.string.today_usage)` |
| 277 | `"$weeklyAverage 分钟/天"` | 见下方代码 |
| 278 | `"本周平均"` | `stringResource(R.string.weekly_average)` |

完整代码（需要在 Composable 中）：
```kotlin
StatValue(
    value = stringResource(R.string.minutes_format, todayUsage),
    label = stringResource(R.string.today_usage),
    valueColor = MaterialTheme.colorScheme.primary
)
StatValue(
    value = "$weeklyAverage ${stringResource(R.string.minutes_per_day)}",
    label = stringResource(R.string.weekly_average),
    valueColor = MaterialTheme.colorScheme.secondary
)
```

**Verification:** 运行 `./gradlew assembleDebug` 确保无编译错误

---

## Task 7: AppDetailScreen 国际化 - Part 3 (RestrictionModeSection)

**Files:**
- Modify: `app/src/main/java/com/example/slowdown/ui/screen/AppDetailScreen.kt`

**Step 1:** 修改 RestrictionModeSection 中各选项的标题和描述

选项1 - 仅统计：
| 原文 | 替换为 |
|------|--------|
| `"仅统计"` | `stringResource(R.string.tracking_only_title)` |
| `"只记录使用时间，不做任何干预"` | `stringResource(R.string.tracking_only_desc)` |

选项2 - 温和提醒（description 是动态的）：
| 原文 | 替换为 |
|------|--------|
| `"温和提醒"` | `stringResource(R.string.gentle_reminder_title)` |
| `"每日 $currentLimit 分钟后提醒，可继续使用"` | `stringResource(R.string.gentle_reminder_with_limit, currentLimit)` |
| `"每次打开应用时深呼吸提醒"` | `stringResource(R.string.gentle_reminder_every_open)` |
| `"达到限额后提醒，但可继续使用"` | `stringResource(R.string.gentle_reminder_desc)` |

选项3 - 严格限制：
| 原文 | 替换为 |
|------|--------|
| `"严格限制"` | `stringResource(R.string.strict_limit_title)` |
| `"每日 $currentLimit 分钟后禁止使用"` | `stringResource(R.string.strict_limit_with_limit, currentLimit)` |
| `"达到限额后今日禁止继续使用"` | `stringResource(R.string.strict_limit_desc)` |

选项4 - 完全禁止：
| 原文 | 替换为 |
|------|--------|
| `"完全禁止"` | `stringResource(R.string.completely_blocked_title)` |
| `"打开应用即被阻止，无法使用"` | `stringResource(R.string.completely_blocked_desc)` |

**Step 2:** 修改 RestrictionModeItem 中的时间按钮文字

| 原文 | 替换为 |
|------|--------|
| `"$currentLimit 分钟"` | `stringResource(R.string.minutes_format, currentLimit)` |
| `"无限制"` | `stringResource(R.string.no_limit)` |

**Verification:** 运行 `./gradlew assembleDebug` 确保无编译错误

---

## Task 8: AppDetailScreen 国际化 - Part 4 (TimeLimitPickerDialog)

**Files:**
- Modify: `app/src/main/java/com/example/slowdown/ui/screen/AppDetailScreen.kt`

**Step 1:** 修改对话框标题

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 486 | `"设置每日限额"` | `stringResource(R.string.set_daily_limit)` |

**Step 2:** 修改预设选项列表

原代码：
```kotlin
val presets = listOf(
    15 to "15 分钟",
    30 to "30 分钟",
    60 to "1 小时",
    120 to "2 小时"
)
```

由于 stringResource 必须在 @Composable 中调用，需要改为在 Column 内部生成：

```kotlin
// 在 Column 内
val presetsData = listOf(15, 30, 60, 120)
presetsData.forEach { value ->
    val label = when (value) {
        60 -> stringResource(R.string.hour_format, 1)
        120 -> stringResource(R.string.hours_format, 2)
        else -> stringResource(R.string.minutes_format, value)
    }
    // ... RadioButton row
}
```

**Step 3:** 修改无限制选项文字

| 原文 | 替换为 |
|------|--------|
| `"无限制（每次打开都提醒）"` | `stringResource(R.string.no_limit_remind_every_open)` |

**Step 4:** 修改自定义选项

| 原文 | 替换为 |
|------|--------|
| `"自定义"` | `stringResource(R.string.custom)` |
| `label = { Text("分钟数") }` | `label = { Text(stringResource(R.string.enter_minutes)) }` |
| `suffix = { Text("分钟") }` | `suffix = { Text(stringResource(R.string.minutes_label)) }` |

**Step 5:** 修改按钮文字

| 原文 | 替换为 |
|------|--------|
| `"确定"` | `stringResource(R.string.confirm)` |
| `"取消"` | `stringResource(R.string.cancel)` |

**Verification:** 运行 `./gradlew assembleDebug` 确保无编译错误

---

## Task 9: AppDetailScreen 国际化 - Part 5 (VideoAppModeSection 和 RedirectSection)

**Files:**
- Modify: `app/src/main/java/com/example/slowdown/ui/screen/AppDetailScreen.kt`

**Step 1:** 修改 VideoAppModeSection

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 940 | `"短视频模式"` | `stringResource(R.string.video_mode)` |
| 944 | `"已启用：每 30 秒检查一次"` | `stringResource(R.string.video_mode_enabled)` |
| 944 | `"适用于抖音、B站等刷视频应用"` | `stringResource(R.string.video_mode_desc)` |

完整代码：
```kotlin
Text(
    text = if (isVideoApp) stringResource(R.string.video_mode_enabled) else stringResource(R.string.video_mode_desc),
    ...
)
```

**Step 2:** 修改 RedirectSection

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 983 | `"跳转应用"` | `stringResource(R.string.redirect_app)` |
| 988 | `"干预时跳转到指定应用"` | `stringResource(R.string.redirect_app_desc)` |
| 995 | `"无"` | `stringResource(R.string.none)` |

**Verification:** 运行 `./gradlew assembleDebug` 确保无编译错误

---

## Task 10: AppDetailScreen 国际化 - Part 6 (AppPickerDialog)

**Files:**
- Modify: `app/src/main/java/com/example/slowdown/ui/screen/AppDetailScreen.kt`

**Step 1:** 修改 AppPickerDialog

| 行号 | 原文 | 替换为 |
|-----|------|--------|
| 1053 | `"选择跳转应用"` | `stringResource(R.string.select_redirect_app)` |
| 1064 | `"搜索应用..."` | `stringResource(R.string.search_apps_hint)` |
| 1084 | `"无（不跳转）"` | `stringResource(R.string.no_redirect)` |
| 1133 | `"未找到匹配的应用"` | `stringResource(R.string.no_apps_found)` |
| 1143 | `"取消"` | `stringResource(R.string.cancel)` |

**Verification:** 运行 `./gradlew assembleDebug` 确保无编译错误

---

## Task 11: AppDetailScreen 国际化 - Part 7 (旧组件清理)

**Files:**
- Modify: `app/src/main/java/com/example/slowdown/ui/screen/AppDetailScreen.kt`

**说明:** 文件中有部分标记为"已弃用"的旧组件（DailyLimitSection, CustomLimitDialog, LimitModeSection），这些组件中也有硬编码字符串。虽然它们可能不再使用，但为完整性也应该国际化。

**Step 1:** 修改 DailyLimitSection（行 613-761）

| 原文 | 替换为 |
|------|--------|
| `"完全禁止"` | `stringResource(R.string.completely_blocked_simple)` |
| `"打开应用即被阻止"` | `stringResource(R.string.blocked_on_open)` |
| `"无限制"` | `stringResource(R.string.no_limit)` |
| `"15 分钟"` | `stringResource(R.string.minutes_format, 15)` |
| `"30 分钟"` | `stringResource(R.string.minutes_format, 30)` |
| `"1 小时"` | `stringResource(R.string.hour_format, 1)` |
| `"2 小时"` | `stringResource(R.string.hours_format, 2)` |
| `"自定义"` | `stringResource(R.string.custom)` |
| `"$currentLimit 分钟"` | `stringResource(R.string.minutes_format, currentLimit)` |

**Step 2:** 修改 CustomLimitDialog（行 763-815）

| 原文 | 替换为 |
|------|--------|
| `"设置每日限额"` | `stringResource(R.string.set_daily_limit)` |
| `label = { Text("分钟数") }` | `label = { Text(stringResource(R.string.enter_minutes)) }` |
| `suffix = { Text("分钟") }` | `suffix = { Text(stringResource(R.string.minutes_label)) }` |
| `"请输入 1-1440 之间的数字"` | `stringResource(R.string.input_range_error)` |
| `"确定"` | `stringResource(R.string.confirm)` |
| `"取消"` | `stringResource(R.string.cancel)` |

**Step 3:** 修改 LimitModeSection（行 821-914）

| 原文 | 替换为 |
|------|--------|
| `"已选择完全禁止模式"` | `stringResource(R.string.already_blocked_mode)` |
| `"软提醒"` | `stringResource(R.string.soft_reminder)` |
| `"超时后提醒，但允许继续使用"` | `stringResource(R.string.soft_reminder_desc)` |
| `"强制关闭"` | `stringResource(R.string.force_close)` |
| `"超时后今日无法再使用该应用"` | `stringResource(R.string.force_close_desc)` |

**Verification:** 运行 `./gradlew assembleDebug` 确保无编译错误

---

## Task 12: 最终验证

**Step 1:** 完整构建验证

```bash
./gradlew clean assembleDebug
```

**Step 2:** 功能验证清单

1. 打开应用，默认语言正确显示
2. 进入设置页面，切换语言
3. 确认以下页面文字正确切换：
   - [ ] Dashboard（仪表盘）
   - [ ] App List（应用列表）
   - [ ] Statistics（统计）
   - [ ] App Detail（应用详情）
   - [ ] Settings（设置）
4. 确认所有对话框文字正确切换
5. 确认动态内容（如 "15 分钟", "比昨天多 30 分钟"）正确显示

**Step 3:** 回归测试

- 重启应用确认语言设置被保存
- 切换多次确认无崩溃
- 确认数字格式正确（中英文分钟/minutes）

---

## 文件清单

| 文件 | 操作 | 变更数量 |
|-----|------|---------|
| `app/src/main/res/values/strings.xml` | 新增 | ~50 条 |
| `app/src/main/res/values-zh/strings.xml` | 新增 | ~50 条 |
| `app/src/main/java/com/example/slowdown/ui/screen/DashboardScreen.kt` | 修改 | ~12 处 |
| `app/src/main/java/com/example/slowdown/ui/screen/AppListScreen.kt` | 修改 | ~12 处 |
| `app/src/main/java/com/example/slowdown/ui/screen/StatisticsScreen.kt` | 修改 | ~8 处 |
| `app/src/main/java/com/example/slowdown/ui/screen/AppDetailScreen.kt` | 修改 | ~60 处 |

---

## 预估工作量

| Task | 说明 |
|------|-----|
| Task 1 | 添加所有字符串资源 |
| Task 2 | DashboardScreen 国际化 |
| Task 3 | AppListScreen 国际化 |
| Task 4 | StatisticsScreen 国际化 |
| Task 5-11 | AppDetailScreen 国际化（分7个小任务） |
| Task 12 | 最终验证 |
