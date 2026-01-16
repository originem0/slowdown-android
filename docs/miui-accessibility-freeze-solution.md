# MIUI AccessibilityService 冻结问题解决方案

## 问题概述

**现象**：SlowDown 应用在三星 One UI 上正常工作，但在 MIUI（小米）设备上，当用户打开被监控的应用（如 bilibili）时，干预弹窗无法显示。

**最终确认的根本原因**：MIUI 系统会"冻结"后台应用的 AccessibilityService，导致事件无法被接收。

---

## 排查过程

### 第一阶段：初步分析

**症状描述**：
- 在三星设备上，打开被监控应用后立即弹出干预界面
- 在 MIUI 设备上，打开被监控应用后没有任何反应

**初步假设**：
1. MIUI 的"后台弹出界面"权限未开启
2. `startActivity()` 被 MIUI 拦截
3. 需要设置 MIUI 特定的 Intent 标志位（如 `mMiuiFlags`）

### 第二阶段：尝试各种绕过方案

#### 尝试 1：mMiuiFlags 反射设置

试图通过反射设置 Intent 的 `mMiuiFlags = 2`（MIUI 内部标志，允许后台启动 Activity）。

```kotlin
val flagsField = Intent::class.java.getDeclaredField("mMiuiFlags")
flagsField.isAccessible = true
flagsField.setInt(intent, 2)
```

**结果**：失败。`mMiuiFlags` 是隐藏 API，被 Android 系统阻止访问。

#### 尝试 2：元反射绕过隐藏 API 限制

尝试使用 `VMRuntime.setHiddenApiExemptions()` 解除隐藏 API 限制。

**结果**：失败。`setHiddenApiExemptions` 本身也是 `core-platform-api`，只有系统应用才能调用。

#### 尝试 3：moveTaskToFront

使用 `ActivityManager.moveTaskToFront()` 强制将 SlowDown 任务推到前台。

```kotlin
activityManager.moveTaskToFront(task.id, ActivityManager.MOVE_TASK_WITH_HOME)
```

**结果**：失败。虽然代码执行了，但弹窗仍然不显示。

### 第三阶段：添加诊断日志

在 `AppMonitorService` 中添加详细日志：

```kotlin
override fun onServiceConnected() {
    Log.d(TAG, "[Service] ===== AccessibilityService CONNECTED =====")
}

override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    Log.d(TAG, "[Service] onAccessibilityEvent: type=$eventType, pkg=$pkg")
    // ...
}
```

### 第四阶段：关键发现

分析 logcat 后发现一个关键现象：

```
05:32:16.530  SlowDown  [Service] onAccessibilityEvent: type=32, pkg=com.miui.home

            ↑↑↑ 最后一次事件 ↑↑↑

        ~~~~~~ 用户在 bilibili 中浏览了 1 分钟 ~~~~~~

            ↓↓↓ 下一次事件 ↓↓↓

05:33:25.399  SlowDown  [Service] onAccessibilityEvent: type=32, pkg=com.bilibili.app.in
```

**关键洞察**：
- 在用户浏览 bilibili 的这 1 分钟内，**没有任何事件被接收**
- bilibili 事件的时间戳（05:33:25.399）与用户点击 SlowDown 图标的时间（05:33:25.304）几乎完全一致

**用户的关键反馈**：
> "不是延迟发送吧，那为什么我在被限制应用里浏览了至少一分钟，还没有等到弹窗"

这句话让我意识到：事件不是"延迟"了，而是**根本没有被发送**。只有当用户点击 SlowDown 图标"激活"应用时，事件才开始送达。

### 第五阶段：确认根本原因

**根本原因**：MIUI 会"冻结"后台应用的进程，包括 AccessibilityService。当应用处于后台时：
1. AccessibilityService 的进程被冻结
2. 系统不会向冻结的进程发送无障碍事件
3. 只有当用户"激活"应用（点击图标、打开通知等）时，进程才会解冻
4. 解冻后，之前积压的事件才会被送达

这解释了为什么：
- 三星设备正常（没有这种激进的进程冻结机制）
- bilibili 事件在用户点击 SlowDown 图标后立即到达
- 之前所有的绕过方案都无效（因为问题不在启动 Activity，而在于事件根本没有被接收）

---

## 解决方案

### 实施：前台服务通知

让 AccessibilityService 显示一个持久的前台服务通知，告诉系统"这个服务正在执行重要任务"，从而阻止系统冻结进程。

#### 修改 1：NotificationHelper.kt

添加前台服务通知渠道和构建方法：

```kotlin
// 前台服务通知 - 防止 MIUI 冻结 AccessibilityService
private const val FOREGROUND_CHANNEL_ID = "slowdown_foreground"
private const val FOREGROUND_CHANNEL_NAME = "后台运行"
const val FOREGROUND_NOTIFICATION_ID = 1002

fun buildForegroundNotification(context: Context): Notification {
    createNotificationChannel(context)

    val intent = Intent(context, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    return NotificationCompat.Builder(context, FOREGROUND_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("SlowDown 正在运行")
        .setContentText("监控应用启动中...")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setContentIntent(pendingIntent)
        .build()
}
```

#### 修改 2：AppMonitorService.kt

在 `onServiceConnected()` 中启动前台服务：

```kotlin
override fun onServiceConnected() {
    super.onServiceConnected()
    startForegroundNotification()
}

private fun startForegroundNotification() {
    val notification = NotificationHelper.buildForegroundNotification(this)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ServiceCompat.startForeground(
            this,
            NotificationHelper.FOREGROUND_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
    } else {
        startForeground(NotificationHelper.FOREGROUND_NOTIFICATION_ID, notification)
    }
}
```

#### 修改 3：AndroidManifest.xml

为 Service 添加前台服务类型声明（Android 14+ 要求）：

```xml
<service
    android:name=".service.AppMonitorService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:foregroundServiceType="specialUse"
    android:exported="false">
    <!-- ... -->
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="accessibility_monitoring" />
</service>
```

---

## 验证结果

修复后的 logcat 显示：

```
05:47:27.516  SlowDown  [Service] Foreground notification started - service should not be frozen now
05:47:43.520  SlowDown  [Service] onAccessibilityEvent: type=32, pkg=com.bilibili.app.in
05:47:43.529  SlowDown  [Service] Triggering intervention for com.bilibili.app.in, countdown=5s
```

- 前台服务通知成功启动
- bilibili 事件被**立即捕获**
- 干预界面成功触发

---

## 关键教训

1. **日志是最重要的诊断工具**：没有日志就是盲人摸象

2. **要质疑自己的假设**：我最初以为是"事件延迟"，用户纠正后才发现是"事件没有被发送"

3. **用户反馈是宝贵的信息**：用户说"我浏览了至少一分钟"是解开谜题的关键

4. **理解平台特性**：MIUI 的进程冻结机制是独特的，不能用通用 Android 思维来理解

5. **前台服务是保活的标准方案**：这是 Android 官方推荐的方式，比各种 Hack 更可靠

---

## 相关文件

| 文件 | 作用 |
|------|------|
| `AppMonitorService.kt` | AccessibilityService，监控应用启动，现在会启动前台服务 |
| `NotificationHelper.kt` | 通知帮助类，新增前台服务通知构建方法 |
| `AndroidManifest.xml` | 添加了 `foregroundServiceType="specialUse"` |

---

## 后续优化建议

1. **用户可选**：可以添加设置项，让用户选择是否显示前台服务通知（牺牲 MIUI 兼容性换取通知栏清洁）

2. **智能检测**：只在 MIUI 设备上显示前台服务通知，其他设备不需要

3. **通知美化**：可以让通知显示更多有用信息，如"已监控 X 个应用"

---

*文档创建时间：2026-01-17*
*问题解决耗时：约 4 小时*
*核心洞察来源：用户反馈 "不是延迟发送吧，那为什么我在被限制应用里浏览了至少一分钟"*
