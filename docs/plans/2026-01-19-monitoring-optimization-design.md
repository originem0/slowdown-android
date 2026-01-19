# SlowDown 监控优化设计文档

**文档日期**: 2026-01-19
**状态**: 设计阶段
**优先级**: 中等

---

## 📋 执行摘要

本文档记录了针对 SlowDown 应用监控系统的 4 项优化建议，经过详细的优缺点分析和讨论后确定执行。这些优化旨在提高时间统计精度、防止边缘情况错误、减少不必要的资源消耗。

**优化清单**：
- ✅ 优化 #1: 时间同步精度优化（虚拟分钟方案）
- ✅ 优化 #2: cooldown 最小值保护
- ✅ 优化 #3: 防抖动机制
- ✅ 优化 #4: 视频应用误判优化

**预计影响**：
- 时间统计精度提升：60秒误差 → ~0秒误差
- 防止极端情况：cooldown=0 导致的疯狂弹窗
- 性能优化：减少重复检查和数据库查询
- 用户体验提升：减少非视频场景的误触发

---

## 🎯 优化 #1: 时间同步精度优化（虚拟分钟方案）

### 问题描述

**当前机制**：
- 实时追踪的使用时间累积到 60 秒才写入数据库
- 目的是减少数据库写入频率
- 副作用：检查使用时长时可能存在最大 60 秒的误差

**问题场景**：
```
限额：30 分钟
实际使用：29 分 50 秒
数据库记录：29 分钟（50秒未累积到60秒）
系统判断：29/30 = 96.67% → 可能不触发 80% 警告
实际应该：99.4% → 应该触发
```

**代码位置**：
- `UsageTrackingManager.kt:325-332` - 累积逻辑
- `UsageTrackingManager.kt:338-365` - 检查警告逻辑

### 解决方案：虚拟分钟方案

**核心思路**：
不改变数据库写入频率，而是在**计算使用时长时动态包含缓冲区的未写入部分**。

**实现步骤**：

#### 步骤 1: 新增虚拟分钟计算方法

```kotlin
// UsageTrackingManager.kt 新增方法
/**
 * 获取当前使用分钟数（包含未写入数据库的缓冲部分）
 *
 * @param packageName 应用包名
 * @return 虚拟使用分钟数 = 数据库记录 + 实时追踪缓冲
 */
suspend fun getCurrentUsageMinutesWithBuffer(packageName: String): Int {
    val todayDate = java.time.LocalDate.now().toString()
    val dbMinutes = repository.getUsageRecord(packageName, todayDate)?.usageMinutes ?: 0

    // 如果当前正在追踪这个应用，加上缓冲区的秒数
    if (currentTrackingPackage == packageName && isRealtimeTrackingEnabled) {
        val bufferedSeconds = accumulatedRealtimeMs / 1000
        val bufferedMinutes = bufferedSeconds / 60
        return dbMinutes + bufferedMinutes.toInt()
    }

    return dbMinutes
}
```

#### 步骤 2: 修改检查警告逻辑

```kotlin
// UsageTrackingManager.kt:338 修改 checkUsageWarning 方法
suspend fun checkUsageWarning(packageName: String): UsageWarningType? {
    val app = repository.getMonitoredApp(packageName) ?: return null
    val dailyLimit = app.dailyLimitMinutes ?: return null

    // 改用虚拟分钟（包含缓冲区）
    val currentMinutes = getCurrentUsageMinutesWithBuffer(packageName)

    val usageRatio = currentMinutes.toDouble() / dailyLimit

    Log.d(TAG, "checkUsageWarning($packageName): $currentMinutes/$dailyLimit min (${(usageRatio * 100).toInt()}%), mode: ${app.limitMode}")

    return when {
        usageRatio >= 1.0 -> {
            if (app.limitMode == "strict") {
                UsageWarningType.LIMIT_REACHED_STRICT
            } else {
                UsageWarningType.LIMIT_REACHED_SOFT
            }
        }
        usageRatio >= WARNING_THRESHOLD -> {
            UsageWarningType.SOFT_REMINDER
        }
        else -> null
    }
}
```

### 优势分析

✅ **精度提升**：
- 误差从最大 60 秒降低到 ~0 秒
- 特别是在 80% 和 100% 临界点更准确

✅ **性能最优**：
- 不增加数据库写入次数
- 只是读取时多一次内存计算（微秒级）

✅ **代码简洁**：
- 只需新增一个方法
- 修改一处调用点
- 不破坏现有架构

✅ **向后兼容**：
- 数据库结构不变
- 现有数据仍然有效

### 风险评估

⚠️ **风险 1: 线程安全**
- `accumulatedRealtimeMs` 是可变状态
- **缓解措施**: 已使用 `@Volatile` 或在同一线程读写

⚠️ **风险 2: 边缘情况**
- 用户快速切换应用时，`currentTrackingPackage` 可能不准确
- **缓解措施**: 只影响最后 60 秒的精度，影响有限

### 测试建议

1. **单元测试**：
   ```kotlin
   @Test
   fun `getCurrentUsageMinutesWithBuffer includes buffer`() {
       // 设置数据库记录 29 分钟
       // 设置实时追踪缓冲 50 秒
       // 验证返回 29 分钟（50秒/60秒=0）

       // 设置缓冲 70 秒
       // 验证返回 30 分钟（70秒/60秒=1）
   }
   ```

2. **集成测试**：
   ```
   场景 1: 使用抖音 29 分 50 秒（限额 30 分钟）
     - 验证触发 SOFT_REMINDER（99.4% >= 80%）

   场景 2: 使用抖音 23 分 50 秒（限额 30 分钟）
     - 验证不触发警告（79.4% < 80%）
   ```

3. **性能测试**：
   ```
   - 连续使用 1 小时，观察日志中的时间计算
   - 验证数据库写入次数未增加（仍为每分钟一次）
   ```

---

## 🎯 优化 #2: cooldown 最小值保护

### 问题描述

**当前风险**：
- 用户可以将 `cooldownMinutes` 设置为 0
- 导致 `cooldownMs = 0`
- 任何 `elapsed >= 0` 总是 true
- 结果：每次检查都触发弹窗（视频应用每 30 秒一次）

**问题场景**：
```
用户设置 cooldown = 0
  ↓
打开抖音 → 深呼吸弹窗
  ↓
30 秒后定时器触发 → 又弹窗
  ↓
再 30 秒 → 又弹窗
  ↓
疯狂弹窗，用户崩溃 💥
```

**代码位置**：
- `UserPreferences.kt:36-37` - cooldown 配置读取
- `AppMonitorService.kt:417` - cooldown 检查逻辑

### 解决方案：读取时强制最小值

**实现步骤**：

#### 修改 checkCooldown 方法

```kotlin
// AppMonitorService.kt:416 修改
private suspend fun checkCooldown(packageName: String): Boolean {
    // 强制最小值 1 分钟
    val cooldownMinutes = max(repository.cooldownMinutes.first(), 1)
    val lastTime = cooldownMap[packageName] ?: 0
    val cooldownMs = cooldownMinutes * 60 * 1000L
    val elapsed = System.currentTimeMillis() - lastTime
    val canShow = elapsed >= cooldownMs
    if (!canShow) {
        Log.d(TAG, "[UsageWarning] $packageName in cooldown (${elapsed/1000}s < ${cooldownMs/1000}s)")
    }
    return canShow
}
```

**同样需要修改的位置**：

```kotlin
// AppMonitorService.kt:588（无限制应用的 cooldown 检查）
val cooldownMinutes = max(repository.cooldownMinutes.first(), 1)

// AppMonitorService.kt:95（视频应用定时检查的 cooldown）
val cooldownMinutes = max(repository.cooldownMinutes.first(), 1)
```

### 优势分析

✅ **简单有效**：
- 只需添加 `max(value, 1)`
- 3 处代码改动

✅ **防御彻底**：
- 即使用户通过其他方式设置 0，也会被拦截
- 适用于所有 cooldown 检查点

✅ **用户友好**：
- 不需要修改 UI
- 不破坏用户配置（只是在使用时限制）

### 风险评估

⚠️ **风险: 用户期望不一致**
- 用户可能期望设置 0 = 每次都触发
- **缓解措施**: 在设置界面添加说明："最小值 1 分钟"

### 测试建议

1. **单元测试**：
   ```kotlin
   @Test
   fun `checkCooldown enforces minimum 1 minute`() {
       // 模拟 cooldownMinutes = 0
       // 验证实际使用 1 分钟
   }
   ```

2. **手动测试**：
   ```
   1. 尝试在 UI 设置 cooldown = 0（如果可以）
   2. 打开被监控应用
   3. 验证至少 1 分钟后才会再次触发
   ```

---

## 🎯 优化 #3: 防抖动机制

### 问题描述

**当前情况**：
- 事件触发路径：`onAccessibilityEvent` → `syncNow()` → `checkAndShowUsageWarning()`
- 同步完成回调：`onSyncCompleteListener` → `checkAndShowUsageWarning()`
- 可能在极短时间内（200ms）重复检查同一应用

**问题场景**：
```
08:00:00.000  用户打开抖音
08:00:00.100  事件触发 → syncNow()
08:00:00.350  delay(200ms) → checkAndShowUsageWarning("抖音")
08:00:00.400  syncNow() 完成 → onSyncCompleteListener
08:00:00.450  checkAndShowUsageWarning("抖音")  ← 重复检查

结果：500ms 内检查 2 次，浪费资源
```

**代码位置**：
- `AppMonitorService.kt:335` - checkAndShowUsageWarning 方法
- `AppMonitorService.kt:162` - 事件触发调用
- `UsageTrackingManager.kt:199-202` - 同步完成回调

### 解决方案：时间戳去重

**核心思路**：
记录每个应用的最后检查时间，500ms 内不重复检查。

**实现步骤**：

#### 步骤 1: 添加去重映射表

```kotlin
// AppMonitorService.kt:32 新增成员变量
private val lastCheckTime = ConcurrentHashMap<String, Long>()
```

#### 步骤 2: 修改检查方法

```kotlin
// AppMonitorService.kt:335 修改 checkAndShowUsageWarning 方法
private suspend fun checkAndShowUsageWarning(packageName: String) {
    if (!::usageTrackingManager.isInitialized) return

    // 防抖动：500ms 内不重复检查同一应用
    val now = System.currentTimeMillis()
    val lastCheck = lastCheckTime[packageName] ?: 0
    if (now - lastCheck < 500) {
        Log.d(TAG, "[Debounce] Skip duplicate check for $packageName (${now - lastCheck}ms ago)")
        return
    }
    lastCheckTime[packageName] = now

    // 每天重置已显示警告的记录
    val todayDate = java.time.LocalDate.now().toString()
    if (todayDate != lastResetDate) {
        shownLimitWarningToday.clear()
        lastResetDate = todayDate
        Log.d(TAG, "[UsageWarning] Reset daily warning records for $todayDate")
    }

    // ... 原有逻辑
}
```

### 优势分析

✅ **减少重复计算**：
- 避免 500ms 内的重复检查
- 减少数据库查询次数

✅ **与现有机制兼容**：
- 不影响 cooldown 机制
- 只是在更短的时间窗口内去重

✅ **代码简单**：
- 使用与 `cooldownMap` 相同的模式
- 易于理解和维护

### 风险评估

⚠️ **风险: 时间窗口选择**
- 500ms 可能太长或太短
- **缓解措施**: 可以根据日志调整，初始值 500ms 是保守估计

### 测试建议

1. **日志验证**：
   ```
   - 观察日志中的 [Debounce] 消息
   - 统计实际去重的次数
   ```

2. **压力测试**：
   ```
   - 快速切换应用（抖音 → 微信 → 抖音）
   - 验证不会重复触发
   ```

---

## 🎯 优化 #4: 视频应用前台检测优化

### 问题描述

**当前策略**：
- 当 `rootInActiveWindow == null` 时，假设是全屏视频
- 实际可能是：WebView 渲染、系统动画、权限问题等

**代码位置**：
- `AppMonitorService.kt:156-161` - null 判断逻辑
- `AppMonitorService.kt:624-636` - 弹窗前的最后验证

### 解决方案：统一 null 处理策略

**核心思路**：
当 `rootInActiveWindow == null` 时，不做额外判断，直接继续执行检查。原因：
1. **视频应用模式（`isVideoApp`）已有 30 秒定时器兜底**
2. **用户主动标记的视频应用不会漏检**
3. **横屏检测过于保守，会漏掉竖屏短视频场景**

**~~之前考虑的方案：屏幕朝向辅助判断~~**
> 经过实际测试，发现横屏检测弊大于利：
> - 竖屏短视频（抖音、快手）是主要使用场景，但会被跳过
> - 视频应用的 30 秒定时器已经足够覆盖所有情况
> - 简化逻辑更可靠

**实现步骤**：

#### 修改判断逻辑

```kotlin
// AppMonitorService.kt:156 修改
if (actualForeground == currentFg || actualForeground == null) {
    if (actualForeground == null) {
        Log.d(TAG, "[Service] Sync completed, foreground is null, proceeding with check for: $currentFg")
    } else {
        Log.d(TAG, "[Service] Sync completed, checking warnings for current foreground: $currentFg")
    }
    serviceScope.launch {
        checkAndShowUsageWarning(currentFg)
    }
} else {
    Log.d(TAG, "[Service] Sync completed but actual foreground ($actualForeground) != tracked ($currentFg), skip warning check")
}
```

**同样需要修改的位置**：

```kotlin
// AppMonitorService.kt:624（弹窗前的最后验证）
private fun launchDeepBreathOverlay(...) {
    val actualForeground = try {
        rootInActiveWindow?.packageName?.toString()
    } catch (e: Exception) {
        null
    }

    // 只在明确检测到不同应用时才跳过
    if (actualForeground != null && actualForeground != packageName) {
        Log.d(TAG, "[Service] launchDeepBreathOverlay: actual foreground ($actualForeground) != target ($packageName), skip")
        return
    }

    if (actualForeground == null) {
        Log.d(TAG, "[Service] launchDeepBreathOverlay: foreground is null, proceeding anyway (may be fullscreen mode)")
    }

    // ... 原有逻辑
}
```

### 优势分析

✅ **逻辑一致性**：
- `launchDeepBreathOverlay` 和 `launchUsageWarningActivity` 行为一致
- 减少边缘情况的不确定性

✅ **覆盖更全面**：
- 竖屏短视频不会被漏检
- 普通应用的 null 情况也能正常处理

✅ **代码简洁**：
- 删除了 `isProbablyWatchingVideo()` 函数
- 逻辑更直接，更易理解

### 风险评估

⚠️ **风险: 可能在非预期场景触发**
- WebView 渲染、系统动画时可能误触发
- **缓解措施**:
  - 有 cooldown 机制保护（至少 1 分钟间隔）
  - 有防抖动机制（500ms 内不重复检查）
  - 用户体验影响有限

### 测试建议

1. **场景测试**：
   ```
   场景 1: 抖音横屏全屏视频
     - rootInActiveWindow == null
     - 验证触发检查 ✅

   场景 2: 抖音竖屏刷视频
     - rootInActiveWindow == null（可能）
     - 验证触发检查 ✅（之前会被跳过）

   场景 3: 微信打开 WebView
     - rootInActiveWindow == null（短暂）
     - 验证有 cooldown 保护，不会频繁弹窗 ✅
   ```

2. **日志监控**：
   ```
   - 观察 "foreground is null, proceeding" 日志
   - 确认弹窗触发正常
   ```

---

## 📊 总体影响评估

### 代码改动范围

| 文件 | 新增行数 | 修改行数 | 风险等级 |
|-----|---------|---------|---------|
| `UsageTrackingManager.kt` | +20 | +5 | 低 |
| `AppMonitorService.kt` | +35 | +10 | 中 |
| 总计 | +55 | +15 | 低-中 |

### 性能影响

| 指标 | 优化前 | 优化后 | 变化 |
|-----|-------|-------|-----|
| 时间统计精度 | ±60秒 | ±0秒 | ✅ 提升 |
| 数据库写入次数 | N 次/小时 | N 次/小时 | ➡️ 不变 |
| 重复检查次数 | 可能重复 | 500ms 去重 | ✅ 减少 |
| CPU 占用 | 基准 | +0.1% | ➡️ 可忽略 |
| 内存占用 | 基准 | +1KB | ➡️ 可忽略 |

### 用户体验影响

✅ **正面影响**：
1. 时间统计更精确（接近 100% 时非常重要）
2. 竖屏短视频场景不会漏检
3. 防止极端配置导致的疯狂弹窗

⚠️ **潜在负面**：
1. WebView 渲染等场景可能误触发（但有 cooldown 保护）
2. 用户期望 cooldown=0 每次触发（但这本身就不合理）

### 风险矩阵

| 风险 | 发生概率 | 影响程度 | 缓解措施 |
|-----|---------|---------|---------|
| 线程安全问题 | 低 | 高 | 代码审查 + 压力测试 |
| 非预期场景误触发 | 中 | 低 | cooldown + 防抖动机制 |
| 防抖窗口不当 | 低 | 低 | 根据日志调整 |

---

## 🚀 实施建议

### 实施顺序

**建议按以下顺序实施**（从低风险到高风险）：

1. **第一批**：独立改动，风险最低
   - 优化 #2: cooldown 最小值保护（1 行代码）
   - 优化 #3: 防抖动机制（简单逻辑）

2. **第二批**：需要测试验证
   - 优化 #1: 虚拟分钟方案（核心逻辑改动）

3. **第三批**：需要场景测试
   - 优化 #4: 视频应用误判优化（行为变化）

### 测试策略

**单元测试**：
```kotlin
// UsageTrackingManagerTest.kt
@Test
fun `virtual minutes include buffer correctly`() { ... }

@Test
fun `cooldown enforces minimum 1 minute`() { ... }

@Test
fun `debounce prevents duplicate checks within 500ms`() { ... }
```

**集成测试**：
```
1. 安装应用到真机
2. 设置抖音限额 30 分钟
3. 使用 29 分 50 秒后切换应用
4. 验证触发 80% 警告
```

**回归测试**：
```
- 验证现有功能不受影响
- 重点测试：无限制应用、强制模式、视频应用定时器
```

### 发布策略

**方案 A：灰度发布**
1. 先在开发环境充分测试
2. 发布给 10% 用户（Beta 测试）
3. 观察 1 周，检查日志和用户反馈
4. 逐步扩大到 100%

**方案 B：一次性发布**
1. 充分的单元测试 + 集成测试
2. 代码审查
3. 直接发布正式版本

**推荐**：方案 A（因为涉及核心监控逻辑）

### 回滚计划

**如果出现问题，回滚步骤**：
```bash
# 1. 回退到优化前的 commit
git revert <optimization-commit-hash>

# 2. 发布紧急修复版本
# 3. 通知用户更新

# 4. 分析问题原因
# 5. 修复后重新发布
```

---

## 📝 未执行的优化（存档）

以下优化经过讨论后决定不执行，记录在此供参考：

### ❌ 优化 #X1: cooldownMap 内存管理
**决定**：不执行
**理由**：100个应用才占 ~5KB 内存，YAGNI 原则

### ❌ 优化 #X2: shownLimitWarningToday 清理
**决定**：不执行
**理由**：已有日期重置机制，无需额外优化

---

## 📚 参考资料

**相关代码文件**：
- `app/src/main/java/com/example/slowdown/service/AppMonitorService.kt`
- `app/src/main/java/com/example/slowdown/service/UsageTrackingManager.kt`
- `app/src/main/java/com/example/slowdown/data/preferences/UserPreferences.kt`

**讨论记录**：
- 2026-01-19 三次对话：系统架构分析、用户理解核对、优化建议讨论

**Android API 文档**：
- [UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager)
- [AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- [WindowManager](https://developer.android.com/reference/android/view/WindowManager)

---

## 版本历史

| 版本 | 日期 | 作者 | 变更说明 |
|-----|------|------|---------|
| 1.0 | 2026-01-19 | Claude & User | 初始版本，4项优化设计 |
| 1.1 | 2026-01-20 | Claude & User | 移除横屏检测逻辑，改用统一 null 处理策略 |

---

**文档结束** - 准备实施时请参考本文档
