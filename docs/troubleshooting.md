# SlowDown 问题排查与开发日志

## 目录
- [常见问题解答](#常见问题解答)
- [已修复的 Bug](#已修复的-bug)
- [MIUI 兼容性处理](#miui-兼容性处理)

---

## 常见问题解答

### Q1: 为什么冷却时间到了弹窗没有立即出现？

**A**: 这是设计预期行为。弹窗采用被动触发机制，依赖 AccessibilityEvent 事件。只有当用户在应用内产生操作（如滑动、点击、页面跳转）时才会触发检查。如果用户只是静静观看内容，不会触发新的弹窗。

### Q2: 为什么离开被监控应用后弹窗还会弹出？

**A**: 这是一个已修复的 bug（见 Bug 3）。现在系统有三层防护确保弹窗只在用户确实在被监控应用中时才会显示。

### Q3: cooldown 时间是精确的吗？

**A**: cooldown 是"最小间隔"而非"精确间隔"。例如设置 1 分钟 cooldown：
- 用户 0:00 触发弹窗
- 用户在应用内静止 3 分钟
- 用户 3:00 产生操作 → 此时才触发下一次弹窗
- 实际间隔是 3 分钟，而非 1 分钟

### Q4: 为什么在应用内持续操作时弹窗不触发？

**A**: 这是一个已修复的 bug（见 Bug 4）。之前的代码在同一应用内的事件时会直接跳过检查，导致即使 cooldown 到期也不会触发弹窗。修复后，每次事件都会检查 cooldown 是否到期。

### Q5: 为什么刷短视频时弹窗不触发？

**A**: 短视频应用（抖音、B站等）使用 ViewPager2/RecyclerView 实现视频切换，竖滑视频时不会产生 `TYPE_WINDOW_STATE_CHANGED` 事件。解决方案：开启"短视频模式"，该模式使用定时器（每 30 秒）主动检查并触发弹窗。

### Q6: 会影响电话和短信功能吗？

**A**: 不会。应用内置智能识别系统，自动检测电话、短信等关键系统功能，并立即允许访问。

### Q7: 应用会消耗大量电池吗？

**A**: 不会。应用采用高效的后台监控算法，正常使用情况下电池消耗不到1%。短视频模式会略增加消耗（每 30 秒一次检查）。

---

## 已修复的 Bug

### Bug 1：限制模式切换逻辑错误

**修复日期：** 2026-01-17

**问题表现：**
- 选择"仅统计"后无法切换到其他模式
- "完全禁止"和"严格限制"可以互换，但无法切换到"温和提醒"
- 添加应用默认是温和提醒，选择其他选项后无法切回

**根本原因：**
`onModeChange` 处理器进行多次独立的 ViewModel 调用（`updateEnabled`、`updateLimitMode`、`updateDailyLimit`），导致状态竞争条件。当快速连续调用时，后面的更新可能基于旧状态执行。

**解决方案：**
在 `AppDetailViewModel.kt` 中添加原子更新方法：
```kotlin
fun updateRestrictionMode(isEnabled: Boolean, limitMode: String, dailyLimitMinutes: Int?) {
    viewModelScope.launch {
        _monitoredApp.value?.let { app ->
            val updated = app.copy(
                isEnabled = isEnabled,
                limitMode = limitMode,
                dailyLimitMinutes = dailyLimitMinutes
            )
            repository.updateMonitoredApp(updated)
        }
    }
}
```

**关键学习：** 多个相关状态字段必须原子更新，避免中间状态导致的竞争条件。

---

### Bug 2：英文字母搜索不准确

**修复日期：** 2026-01-17

**问题表现：**
在应用列表页面搜索时，输入英文字母无法正确匹配应用名称。

**根本原因：**
1. 中日文输入法可能产生全角英文字母（如 `ａｂｃ` 而非 `abc`）
2. 大小写转换在不同 Locale 下行为不一致

**解决方案：**
在 `AppListScreen.kt` 中：
1. 使用 `Locale.ROOT` 进行一致的大小写转换
2. 添加 `toHalfWidth()` 函数处理全角转半角：
```kotlin
private fun String.toHalfWidth(): String {
    val sb = StringBuilder()
    for (char in this) {
        val code = char.code
        if (code == 0x3000) {
            sb.append(' ')  // 全角空格
        } else if (code in 0xFF01..0xFF5E) {
            sb.append((code - 0xFEE0).toChar())  // 全角转半角
        } else {
            sb.append(char)
        }
    }
    return sb.toString()
}
```

**关键学习：** CJK 输入法兼容性需要考虑全角/半角字符转换。

---

### Bug 3：弹窗在离开被监控应用后仍然弹出

**修复日期：** 2026-01-17

**问题表现：**
用户已退出被监控应用，在 SlowDown 主界面或其他应用时，深呼吸弹窗仍然弹出。

**根本原因：**
1. `currentForegroundApp` 在用户切换到 SlowDown 或系统应用时未被清除
2. 定期同步回调检查的是旧的 `currentForegroundApp` 值
3. `OverlayService` 使用 `WindowManager.TYPE_APPLICATION_OVERLAY` 无条件覆盖所有应用

**解决方案：**
在 `AppMonitorService.kt` 中实现三层前台验证防护：

**第1层 - 事件接收时清除追踪：**
```kotlin
// 切换到 SlowDown 自己
if (packageName == this.packageName) {
    currentForegroundApp = null
    return
}
// 切换到系统应用
if (PackageUtils.isSystemCriticalApp(packageName)) {
    currentForegroundApp = null
    return
}
```

**第2层 - 同步回调验证：**
```kotlin
usageTrackingManager.setOnSyncCompleteListener { updatedPackages ->
    val currentFg = currentForegroundApp
    if (currentFg != null && currentFg in updatedPackages) {
        val actualForeground = rootInActiveWindow?.packageName?.toString()
        if (actualForeground == currentFg || actualForeground == null) {
            checkAndShowUsageWarning(currentFg)
        }
    }
}
```

**第3层 - 启动弹窗前最终验证：**
```kotlin
private fun launchDeepBreathOverlay(...) {
    val actualForeground = rootInActiveWindow?.packageName?.toString()
    if (actualForeground != null && actualForeground != packageName) {
        return  // 不匹配，跳过
    }
    // 继续启动弹窗
}
```

**关键学习：** 悬浮窗/弹窗必须在多个检查点验证前台状态，单一检查点不足以防止误触发。

---

### Bug 4：应用内持续操作时深呼吸弹窗不触发

**修复日期：** 2026-01-17

**问题表现：**
在被监控应用内持续滑动、点击时，即使 cooldown 时间到期，深呼吸弹窗也不会触发。只有退出到后台再返回时才会触发。

**根本原因：**
`handleRealtimeTracking` 函数在检测到同一应用的事件时直接 return：
```kotlin
// 如果是同一个应用，不需要处理
if (newPackageName == currentForegroundApp) return
```
这导致 cooldown 检查被完全跳过。

**解决方案：**
将使用时间记录和弹窗检查逻辑分离：
```kotlin
val isSameApp = newPackageName == currentForegroundApp

// 只有切换到不同应用时才记录使用时间和重置追踪
if (!isSameApp) {
    // ... 记录使用时间，更新追踪状态
}

// 检查是否需要触发弹窗（无论是否同一应用都检查）
checkAndShowUsageWarning(newPackageName)
```

**关键学习：** 记录逻辑和触发逻辑应该分离，避免一个条件跳过影响另一个功能。

---

### Bug 5：短视频模式在全屏播放时定时器停止

**修复日期：** 2026-01-17

**问题表现：**
刷短视频时没有弹窗反应，直到点进评论区才触发弹窗。

**根本原因：**
`rootInActiveWindow` 在全屏视频播放（SurfaceView/TextureView）时返回 `null`。原代码将 `null` 视为"用户已离开应用"并停止定时器：
```kotlin
// 错误：null 时停止定时器
if (actualForeground != targetApp) {
    stopVideoAppCheck()  // null != targetApp，误停止
    return
}
```

**解决方案：**
修改判断逻辑，只有当 `actualForeground` 明确是其他应用时才停止定时器：
```kotlin
// 正确：区分 null、匹配、不匹配三种情况
if (actualForeground != null && actualForeground != targetApp) {
    stopVideoAppCheck()  // 明确切换到其他应用才停止
    return
}

if (actualForeground == null) {
    Log.d(TAG, "Foreground is null (fullscreen video?), proceeding anyway")
}
// 继续检查...
```

**关键学习：** `rootInActiveWindow` 在全屏视频、WebView 渲染等特殊 UI 状态下会返回 `null`，不能将 `null` 等同于"用户离开"。

---

### Bug 6：浏览器应用内持续浏览时弹窗不触发

**修复日期：** 2026-01-17

**问题表现：**
在 Chrome 浏览器中持续浏览页面，弹窗不出现，退出再进入才触发。

**根本原因：**
与 Bug 5 同一问题。定期同步回调（`onSyncCompleteListener`）中对 `rootInActiveWindow == null` 的处理有误。

**解决方案：**
修改 sync callback 逻辑，当 `actualForeground == null` 时也继续检查弹窗：
```kotlin
// 正确：null 时也继续检查
if (actualForeground == currentFg || actualForeground == null) {
    checkWarning()
} else {
    skip()
}
```

**关键学习：** 同一个 `rootInActiveWindow == null` 问题影响了多个代码路径，需要全面排查。

---

## MIUI 兼容性处理

### 问题概述

**现象**：SlowDown 应用在三星 One UI 上正常工作，但在 MIUI（小米）设备上，当用户打开被监控的应用时，干预弹窗无法显示。

**根本原因**：MIUI 系统会"冻结"后台应用的 AccessibilityService，导致事件无法被接收。当应用处于后台时：
1. AccessibilityService 的进程被冻结
2. 系统不会向冻结的进程发送无障碍事件
3. 只有当用户"激活"应用（点击图标、打开通知等）时，进程才会解冻
4. 解冻后，之前积压的事件才会被送达

### 排查过程

#### 第一阶段：初步假设
1. MIUI 的"后台弹出界面"权限未开启
2. `startActivity()` 被 MIUI 拦截
3. 需要设置 MIUI 特定的 Intent 标志位

#### 第二阶段：尝试绕过方案
- **mMiuiFlags 反射设置**：失败，隐藏 API 被阻止
- **元反射绕过**：失败，`setHiddenApiExemptions` 只有系统应用能调用
- **moveTaskToFront**：失败，弹窗仍不显示

#### 第三阶段：关键发现
通过日志分析发现：用户浏览被监控应用时**没有任何事件被接收**。只有当用户点击 SlowDown 图标"激活"应用时，事件才开始送达。

**用户反馈**：
> "不是延迟发送吧，那为什么我在被限制应用里浏览了至少一分钟，还没有等到弹窗"

这句话是解开谜题的关键：事件不是"延迟"了，而是**根本没有被发送**。

### 解决方案：前台服务通知

让 AccessibilityService 显示一个持久的前台服务通知，告诉系统"这个服务正在执行重要任务"，从而阻止系统冻结进程。

#### 修改 1：NotificationHelper.kt
```kotlin
// 前台服务通知 - 防止 MIUI 冻结 AccessibilityService
private const val FOREGROUND_CHANNEL_ID = "slowdown_foreground"
const val FOREGROUND_NOTIFICATION_ID = 1002

fun buildForegroundNotification(context: Context): Notification {
    // ... 构建通知
}
```

#### 修改 2：AppMonitorService.kt
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
```xml
<service
    android:name=".service.AppMonitorService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:foregroundServiceType="specialUse"
    android:exported="false">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="accessibility_monitoring" />
</service>
```

### 关键教训

1. **日志是最重要的诊断工具**：没有日志就是盲人摸象
2. **要质疑自己的假设**：最初以为是"事件延迟"，用户纠正后才发现是"事件没有被发送"
3. **用户反馈是宝贵的信息**：用户说"我浏览了至少一分钟"是解开谜题的关键
4. **理解平台特性**：MIUI 的进程冻结机制是独特的，不能用通用 Android 思维来理解
5. **前台服务是保活的标准方案**：这是 Android 官方推荐的方式，比各种 Hack 更可靠

---

## rootInActiveWindow 的特殊行为

### 问题背景

`rootInActiveWindow` 是 AccessibilityService 获取当前前台应用的标准方法，但在某些情况下会返回 `null`：

| 场景 | rootInActiveWindow 返回值 | 用户实际状态 |
|------|--------------------------|-------------|
| 普通应用正常使用 | 应用包名 | 在该应用中 |
| 全屏视频播放（SurfaceView/TextureView） | **null** | 仍在视频应用中 |
| 浏览器 WebView 渲染 | **null** | 仍在浏览器中 |
| 某些游戏的特殊渲染模式 | **null** | 仍在游戏中 |
| 切换到其他应用 | 其他应用包名 | 已离开原应用 |

### 正确处理方式

```kotlin
// 错误做法：null 时跳过检查
if (actualForeground == targetApp) {
    checkWarning()
} else {
    skip()  // null 也会进入这里，导致误跳过
}

// 正确做法：三状态判断
when {
    actualForeground == targetApp -> checkWarning()  // 正常匹配
    actualForeground == null -> checkWarning()       // 特殊 UI 状态，继续检查
    else -> skip()                                   // 确实切换到其他应用
}
```

### 受影响的代码路径

| 代码位置 | 功能 | 处理状态 |
|---------|------|---------|
| `videoAppCheckRunnable` | 短视频模式定时器 | 已正确处理 |
| `onSyncCompleteListener` | 定期同步回调 | 已正确处理 |
| `launchDeepBreathOverlay` | 弹窗启动前验证 | 已正确处理 |
| `launchUsageWarningActivity` | 强制关闭弹窗验证 | 已正确处理 |

---

## 修改的文件总览

| 文件 | 修改内容 |
|------|---------|
| `AppDetailViewModel.kt` | 添加 `updateRestrictionMode()` 原子更新方法 |
| `AppDetailScreen.kt` | 修改 `onModeChange` 使用原子更新 |
| `AppListScreen.kt` | 添加 `Locale.ROOT` 和 `toHalfWidth()` |
| `AppMonitorService.kt` | 实现三层前台验证防护、修复 null 处理、添加 MIUI 前台服务 |
| `NotificationHelper.kt` | 新增前台服务通知构建方法 |
| `AndroidManifest.xml` | 添加 `foregroundServiceType="specialUse"` |
| `AppDatabase.kt` | 数据库版本升级，添加迁移脚本 |

---

*文档更新时间：2026-01-20*
