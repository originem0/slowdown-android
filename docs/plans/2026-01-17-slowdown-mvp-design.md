# SlowDown MVP 设计文档

## 概述

SlowDown 是一款数字正念 Android 应用，通过在用户尝试访问分散注意力的应用时引入干预机制，帮助用户养成更健康的手机使用习惯。

**目标用户**：开发者自用
**目标平台**：Android 10+ (API 29+)，MIUI 14 适配
**技术栈**：Kotlin + Jetpack Compose + Room + DataStore

---

## MVP 功能范围

| 包含 | 不包含（后续迭代） |
|------|-------------------|
| 倒计时延迟干预 | 文本匹配挑战 |
| 应用跳转引导 | 图片观看任务 |
| 历史统计（日/周趋势） | 成就系统 |
| MIUI 权限适配 | 数据加密/备份导出 |

---

## 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                      SlowDown App                        │
├─────────────────────────────────────────────────────────┤
│  UI Layer (Jetpack Compose)                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │ MainActivity│  │OverlayView │  │ SettingsUI  │     │
│  │ (仪表板)    │  │ (干预界面)  │  │ (配置页)    │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
├─────────────────────────────────────────────────────────┤
│  Domain Layer                                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │ AppMonitor  │  │Intervention │  │ Statistics  │     │
│  │ ViewModel   │  │ ViewModel   │  │ ViewModel   │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
├─────────────────────────────────────────────────────────┤
│  Data Layer                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │    Room     │  │  DataStore  │  │ Repository  │     │
│  │ (干预记录)  │  │ (用户设置)  │  │ (数据聚合)  │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
├─────────────────────────────────────────────────────────┤
│  Service Layer                                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │          AppMonitorService                       │   │
│  │      (AccessibilityService 子类)                 │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

---

## 数据模型

### Room 实体

```kotlin
// 干预记录表
@Entity(tableName = "intervention_records")
data class InterventionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,           // 目标应用包名
    val appName: String,               // 目标应用名称
    val timestamp: Long,               // 触发时间戳
    val interventionType: String,      // "countdown" | "redirect"
    val userChoice: String,            // "continued" | "redirected" | "cancelled"
    val countdownDuration: Int,        // 倒计时秒数
    val actualWaitTime: Int            // 实际等待秒数
)

// 监控应用配置表
@Entity(tableName = "monitored_apps")
data class MonitoredApp(
    @PrimaryKey val packageName: String,
    val appName: String,
    val interventionType: String,      // "countdown" | "redirect"
    val countdownSeconds: Int = 10,
    val redirectPackage: String? = null,
    val isEnabled: Boolean = true
)
```

### DataStore 偏好

```kotlin
object PreferenceKeys {
    val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
    val DEFAULT_COUNTDOWN = intPreferencesKey("default_countdown")
    val COOLDOWN_MINUTES = intPreferencesKey("cooldown_minutes")
}
```

---

## 核心流程

### 干预触发流程

```
用户点击目标应用
        │
        ▼
┌───────────────────┐
│  目标应用启动      │
└───────────────────┘
        │
        ▼ TYPE_WINDOW_STATE_CHANGED
┌───────────────────┐
│ AppMonitorService │
└───────────────────┘
        │
        ▼ 检查条件
┌───────────────────────────────────┐
│ 1. 包名在监控列表？               │
│ 2. 干预功能已启用？               │
│ 3. 不在冷却期？                   │
│ 4. 非系统关键应用？               │
└───────────────────────────────────┘
        │ 全部满足
        ▼
┌───────────────────┐
│ 启动 OverlayActivity │
└───────────────────┘
        │
        ▼
    干预界面显示
```

### 用户选择处理

| 用户选择 | 系统行为 | 记录值 |
|----------|----------|--------|
| 等待后继续 | 关闭干预界面 | `continued` |
| 打开替代应用 | 启动替代应用 | `redirected` |
| 返回桌面 | 回到 Launcher | `cancelled` |

---

## 项目结构

```
app/src/main/java/com/example/slowdown/
├── SlowDownApp.kt
├── service/
│   └── AppMonitorService.kt
├── ui/
│   ├── MainActivity.kt
│   ├── theme/Theme.kt
│   ├── screen/
│   │   ├── DashboardScreen.kt
│   │   ├── AppListScreen.kt
│   │   ├── SettingsScreen.kt
│   │   └── StatsScreen.kt
│   └── overlay/
│       └── OverlayActivity.kt
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   │   ├── InterventionDao.kt
│   │   │   └── MonitoredAppDao.kt
│   │   └── entity/
│   │       ├── InterventionRecord.kt
│   │       └── MonitoredApp.kt
│   ├── preferences/
│   │   └── UserPreferences.kt
│   └── repository/
│       └── SlowDownRepository.kt
├── viewmodel/
│   ├── DashboardViewModel.kt
│   ├── AppListViewModel.kt
│   └── OverlayViewModel.kt
└── util/
    ├── PackageUtils.kt
    └── PermissionHelper.kt
```

---

## 权限配置

### AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<service
    android:name=".service.AppMonitorService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="false">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

### MIUI 适配

需要引导用户开启：
- 自启动权限
- 后台弹窗权限
- 电池优化白名单

---

## 技术依赖

```kotlin
dependencies {
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

---

## UI 设计

### 仪表板

```
┌─────────────────────────────────────┐
│  SlowDown                      ⚙️   │
├─────────────────────────────────────┤
│            今日已拦截                │
│              12 次                  │
│          节省约 45 分钟              │
├─────────────────────────────────────┤
│  本周趋势                           │
│   [简单柱状图]                      │
├─────────────────────────────────────┤
│  最常拦截                           │
│   抖音  ████████░░  42次            │
│   微信  █████░░░░░  28次            │
├─────────────────────────────────────┤
│      [ ＋ 管理监控应用 ]             │
└─────────────────────────────────────┘
```

### 干预界面

```
┌─────────────────────────────────────┐
│                                     │
│        你正在打开 抖音               │
│                                     │
│             [ 10 ]                  │
│            倒计时中                  │
│                                     │
│    ┌─────────────────────────┐      │
│    │   打开微信读书 (绿色)    │      │
│    └─────────────────────────┘      │
│    ┌─────────────────────────┐      │
│    │   继续访问 (倒计时后可用) │      │
│    └─────────────────────────┘      │
│         返回桌面                    │
│                                     │
└─────────────────────────────────────┘
```

---

## 后续迭代方向

1. **更多干预类型**：文本匹配、图片观看
2. **成就系统**：连续天数、里程碑奖励
3. **数据导出**：备份/恢复配置
4. **小组件**：桌面统计 Widget
