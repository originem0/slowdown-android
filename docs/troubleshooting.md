# SlowDown 问题排查指南

## 目录

- [常见问题解答](#常见问题解答)
- [调试技巧](#调试技巧)
- [已修复的 Bug](#已修复的-bug)
- [MIUI 兼容性处理](#miui-兼容性处理)
- [rootInActiveWindow 特殊行为](#rootinactivewindow-特殊行为)

---

## 常见问题解答

### 弹窗相关

#### Q1: 为什么冷却时间到了弹窗没有立即出现？

弹窗采用**被动触发机制**，依赖 AccessibilityEvent 事件。只有当用户在应用内产生操作（滑动、点击、页面跳转）时才会触发检查。如果用户只是静静观看内容，不会触发新的弹窗。

**例外**：视频应用模式会每 30 秒主动检查一次。

#### Q2: cooldown 时间是精确的吗？

cooldown 是**最小间隔**而非精确间隔。例如设置 5 分钟 cooldown：
- 用户 0:00 触发弹窗
- 用户在应用内静止观看 10 分钟
- 用户 10:00 产生操作 → 此时触发下一次弹窗
- 实际间隔是 10 分钟，而非 5 分钟

#### Q3: 为什么刷短视频时弹窗不触发？

短视频应用（抖音、B站等）使用 ViewPager2/RecyclerView 实现视频切换，竖滑视频时**不产生** `TYPE_WINDOW_STATE_CHANGED` 事件。

**解决方案**：在应用详情页开启"视频应用模式"，系统会每 30 秒主动检查。

#### Q4: 为什么使用不到 80% 时没有弹窗？

这是设计预期。SlowDown 的触发阈值是 80%：
- 使用 < 80%：不触发
- 使用 ≥ 80% 且 < 100%：深呼吸提醒
- 使用 ≥ 100%：根据模式决定

**例外**：无限制应用（未设每日限额）会在每个冷却周期后触发。

#### Q5: 严格模式下为什么每次打开都弹窗？

严格模式（LIMIT_REACHED_STRICT）和完全禁止模式**不受 cooldown 限制**，每次打开都会显示阻止界面。这是预期行为。

### 服务相关

#### Q6: 会影响电话和短信功能吗？

不会。应用内置系统应用白名单（`PackageUtils.isSystemCriticalApp`），自动排除电话、短信、系统设置等关键应用。

#### Q7: 应用会消耗大量电池吗？

正常情况下电池消耗 < 1%。影响电量的因素：
- 视频应用模式（每 30 秒检查）
- 短限额应用（同步间隔从 5 分钟降至 1 分钟）

#### Q8: 为什么通知栏有个常驻通知？

这是**前台服务通知**，用于防止 MIUI 等系统冻结 AccessibilityService。不可关闭，否则监控功能会失效。

#### Q9: 无障碍服务经常被关闭怎么办？

**MIUI 用户**：
1. 设置 → 应用设置 → 自启动 → 开启 SlowDown
2. 设置 → 电池 → SlowDown → 无限制
3. 最近任务中锁定 SlowDown（下滑应用卡片）

**其他设备**：
1. 关闭电池优化
2. 在系统设置中允许后台运行

### 数据相关

#### Q10: 使用时间不准确怎么办？

使用时间来自系统 `UsageStatsManager`，可能有以下情况：
- 同步延迟：默认 5 分钟同步一次
- 系统限制：部分定制系统限制 UsageStats 精度

**解决方案**：等待下一次同步，或重启应用强制同步。

#### Q11: 数据库升级后数据丢失了？

正常情况下不会丢失。如果遇到问题：
1. 检查是否使用了开发版本（可能有破坏性迁移）
2. 查看 Logcat 中的数据库迁移日志

---

## 调试技巧

### 查看日志

```bash
# 过滤 SlowDown 日志
adb logcat -s SlowDown:D SlowDownApp:D

# 查看特定功能
adb logcat | grep -E "\[Service\]|\[UsageWarning\]|\[VideoAppCheck\]"
```

### 日志标签含义

| 标签 | 含义 |
|------|------|
| `[Service]` | AppMonitorService 事件处理 |
| `[UsageWarning]` | 使用时间警告检查 |
| `[VideoAppCheck]` | 视频应用定时检查 |
| `[Cooldown]` | 冷却时间相关 |
| `[Overlay]` | 弹窗显示相关 |

### 常用调试命令

```bash
# 查看当前前台应用
adb shell dumpsys activity activities | grep mResumedActivity

# 查看 AccessibilityService 状态
adb shell settings get secure enabled_accessibility_services

# 查看使用统计权限
adb shell appops get <package> GET_USAGE_STATS

# 强制停止服务重启
adb shell am force-stop com.sharonZ.slowdown
```

### 关键检查点

1. **服务是否运行**：通知栏是否有前台服务通知
2. **权限是否完整**：无障碍服务、悬浮窗、使用情况访问
3. **应用是否被监控**：检查应用列表中的启用状态
4. **冷却是否到期**：查看 `[Cooldown]` 日志

---

## 已修复的 Bug

### Bug 1：限制模式切换逻辑错误

**修复日期**：2026-01-17

**问题**：选择"仅统计"后无法切换到其他模式。

**原因**：`onModeChange` 进行多次独立的 ViewModel 调用，导致状态竞争。

**解决**：添加原子更新方法：
```kotlin
fun updateRestrictionMode(isEnabled: Boolean, limitMode: String, dailyLimitMinutes: Int?) {
    viewModelScope.launch {
        _monitoredApp.value?.let { app ->
            repository.updateMonitoredApp(app.copy(
                isEnabled = isEnabled,
                limitMode = limitMode,
                dailyLimitMinutes = dailyLimitMinutes
            ))
        }
    }
}
```

**教训**：多个相关状态字段必须原子更新。

---

### Bug 2：英文字母搜索不准确

**修复日期**：2026-01-17

**问题**：应用列表搜索时英文字母匹配失败。

**原因**：CJK 输入法产生全角字母（`ａｂｃ`）+ Locale 大小写转换不一致。

**解决**：添加 `toHalfWidth()` 全角转半角 + 使用 `Locale.ROOT`。

**教训**：CJK 输入法兼容性需要考虑全角/半角转换。

---

### Bug 3：弹窗在离开应用后仍然弹出

**修复日期**：2026-01-17

**问题**：退出被监控应用后，在其他界面仍弹出深呼吸弹窗。

**原因**：
1. `currentForegroundApp` 未及时清除
2. 同步回调检查旧值
3. 缺少弹窗启动前验证

**解决**：实现三层前台验证防护：
1. 事件接收时：切换到 SlowDown 或系统应用时清除追踪
2. 同步回调时：验证 `rootInActiveWindow` 匹配
3. 弹窗启动前：最终验证前台应用

**教训**：弹窗必须在多个检查点验证前台状态。

---

### Bug 4：应用内持续操作时弹窗不触发

**修复日期**：2026-01-17

**问题**：在应用内持续操作，cooldown 到期后弹窗不触发。

**原因**：`handleRealtimeTracking` 检测到同一应用时直接 return，跳过了 cooldown 检查。

**解决**：分离使用时间记录和弹窗检查逻辑：
```kotlin
val isSameApp = newPackageName == currentForegroundApp
if (!isSameApp) {
    // 记录使用时间
}
// 无论是否同一应用都检查弹窗
checkAndShowUsageWarning(newPackageName)
```

**教训**：记录逻辑和触发逻辑应该分离。

---

### Bug 5：短视频全屏播放时定时器停止

**修复日期**：2026-01-17

**问题**：刷短视频时没有弹窗，点进评论区才触发。

**原因**：`rootInActiveWindow` 在全屏视频时返回 `null`，被误判为"离开应用"。

**解决**：三状态判断：
```kotlin
when {
    actualForeground == targetApp -> checkWarning()
    actualForeground == null -> checkWarning()  // 可能是全屏视频
    else -> stopVideoAppCheck()  // 明确切换到其他应用
}
```

**教训**：`rootInActiveWindow == null` 不等于"用户离开"。

---

### Bug 6：浏览器内持续浏览时弹窗不触发

**修复日期**：2026-01-17

**问题**：Chrome 浏览器内持续浏览，弹窗不出现。

**原因**：与 Bug 5 同一问题，WebView 渲染时 `rootInActiveWindow` 返回 `null`。

**解决**：同 Bug 5，在同步回调中也使用三状态判断。

---

## MIUI 兼容性处理

### 问题现象

SlowDown 在 MIUI（小米）设备上，弹窗无法正常显示。

### 根本原因

MIUI 会**冻结后台 AccessibilityService 进程**：
1. 应用进入后台后进程被冻结
2. 系统不向冻结进程发送无障碍事件
3. 只有用户"激活"应用时才解冻

### 解决方案

使用**前台服务通知**防止进程被冻结：

```kotlin
// AppMonitorService.kt
override fun onServiceConnected() {
    val notification = NotificationHelper.buildForegroundNotification(this)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ServiceCompat.startForeground(
            this,
            FOREGROUND_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
    } else {
        startForeground(FOREGROUND_NOTIFICATION_ID, notification)
    }
}
```

```xml
<!-- AndroidManifest.xml -->
<service
    android:name=".service.AppMonitorService"
    android:foregroundServiceType="specialUse">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="accessibility_monitoring" />
</service>
```

### MIUI 用户额外设置

1. **自启动**：设置 → 应用设置 → 自启动 → 开启
2. **后台弹出界面**：设置 → 应用设置 → 后台弹出界面 → 允许
3. **电池无限制**：设置 → 电池 → SlowDown → 无限制
4. **锁定应用**：最近任务中下滑 SlowDown 卡片锁定

### 排查经验

关键发现来自用户反馈：
> "我在被限制应用里浏览了至少一分钟，还没有等到弹窗"

这表明事件**根本没有被发送**，而非"延迟发送"。日志验证确认：用户浏览期间没有任何事件被接收。

---

## rootInActiveWindow 特殊行为

### 背景

`rootInActiveWindow` 是 AccessibilityService 获取当前前台应用的标准方法，但某些情况会返回 `null`。

### 返回值场景

| 场景 | 返回值 | 用户实际状态 |
|------|--------|-------------|
| 普通应用使用 | 应用包名 | 在该应用中 |
| 全屏视频播放 | `null` | 仍在视频应用中 |
| WebView 渲染 | `null` | 仍在浏览器中 |
| 游戏特殊渲染 | `null` | 仍在游戏中 |
| 切换到其他应用 | 其他包名 | 已离开原应用 |

### 正确处理方式

```kotlin
// 错误：null 会被跳过
if (actualForeground == targetApp) {
    checkWarning()
}

// 正确：三状态判断
when {
    actualForeground == targetApp -> checkWarning()
    actualForeground == null -> checkWarning()  // 特殊 UI 状态
    else -> skip()  // 明确切换到其他应用
}
```

### 受影响的代码路径

| 位置 | 功能 | 状态 |
|------|------|------|
| `videoAppCheckRunnable` | 视频应用定时器 | 已处理 |
| `onSyncCompleteListener` | 定期同步回调 | 已处理 |
| `launchDeepBreathOverlay` | 弹窗启动验证 | 已处理 |
| `launchUsageWarningActivity` | 强制关闭验证 | 已处理 |

### Bug 7：服务禁用后弹窗仍然显示

**修复日期**：2026-01-23

**问题**：用户关闭总开关后，弹窗仍然出现。

**原因**：`serviceEnabled` 检查只存在于 `handleAppLaunch()` 方法中，其他触发路径（实时追踪、视频应用检查、同步回调）没有检查服务状态。

**解决**：
1. 在所有弹窗触发路径添加 `serviceEnabled` 检查
2. 添加 `serviceEnabled` Flow 监听，禁用时清除所有状态：
```kotlin
private fun startServiceEnabledWatcher() {
    serviceScope.launch {
        repository.serviceEnabled.collect { enabled ->
            if (!enabled) {
                cooldownMap.clear()
                lastCheckTime.clear()
                shownLimitWarningToday.clear()
                stopVideoAppCheck()
            }
        }
    }
}
```
3. 在 OverlayViewModel.recordAndFinish() 中添加服务状态检查

**教训**：服务状态检查必须覆盖所有可能的触发路径。

---

### Bug 8：runBlocking 导致 ANR 风险

**修复日期**：2026-01-23

**问题**：UsageWarningActivity 在 `attachBaseContext` 中使用 `runBlocking` 阻塞主线程。

**原因**：`runBlocking` 会阻塞当前线程等待协程完成，在 Activity 创建时可能导致 ANR。

**解决**：使用 `SlowDownApp.cachedLanguage` 缓存的语言设置，避免 I/O 操作：
```kotlin
override fun attachBaseContext(newBase: Context?) {
    val app = newBase.applicationContext as? SlowDownApp
    val language = app?.cachedLanguage ?: "en"
    val localizedContext = LocaleHelper.setLocale(newBase, language)
    super.attachBaseContext(localizedContext)
}
```

**教训**：Activity 生命周期方法中绝对不能使用 `runBlocking`。

---

### Bug 9：Handler 内存泄漏

**修复日期**：2026-01-23

**问题**：`launchUsageWarningActivity` 中每次创建新的 Handler 实例。

**原因**：临时创建的 Handler 会持有 Activity 引用，可能导致内存泄漏。

**解决**：复用类级别的 `videoAppCheckHandler`：
```kotlin
// 之前：每次创建新 Handler
val handler = Handler(Looper.getMainLooper())

// 之后：复用类级别 handler
videoAppCheckHandler.post { moveSlowDownToFront() }
```

**教训**：Handler 应该在类级别定义并在 onDestroy 中清理。

---

### Bug 10：协程无限循环泄漏

**修复日期**：2026-01-23

**问题**：`startMapCleanupTask` 中的 `while(true)` 循环在服务销毁后可能继续运行。

**原因**：协程中的 `while(true)` 没有检查 `isActive`，即使 scope 被取消，delay 之后的代码仍可能执行。

**解决**：使用 `isActive` 检查：
```kotlin
serviceScope.launch {
    while (isActive) {
        delay(MAP_CLEANUP_INTERVAL_MS)
        if (isActive) cleanupStaleMaps()
    }
}
```

**教训**：协程无限循环必须使用 `isActive` 检查。

---

### Bug 11：同步时实时追踪数据丢失

**修复日期**：2026-01-23

**问题**：定期同步 UsageStats 时，实时追踪缓冲区的数据可能被覆盖。

**原因**：`syncUsageStats()` 会重新计算使用时间并覆盖数据库，但实时追踪的 `accumulatedRealtimeMs` 缓冲区数据未被包含。

**解决**：在同步前 flush 缓冲区：
```kotlin
suspend fun syncUsageStats() {
    flushRealtimeBuffer()  // 先写入缓冲数据
    // ... 然后执行同步
}
```

**教训**：多个数据源写入同一位置时，必须考虑同步顺序。

---

*最后更新：2026-01-23*
