# SlowDown 开发指导规范

本文档为 SlowDown 项目的开发指导规范，适用于个人开发和未来可能的协作开发。

## 目录
- [项目结构](#项目结构)
- [开发环境配置](#开发环境配置)
- [代码规范](#代码规范)
- [Git 工作流](#git-工作流)
- [文档管理](#文档管理)
- [测试策略](#测试策略)
- [发布流程](#发布流程)

---

## 项目结构

```
SlowDown/
├── app/
│   └── src/main/
│       ├── java/com/example/slowdown/
│       │   ├── data/              # 数据层
│       │   │   ├── local/         # Room 数据库
│       │   │   │   ├── dao/       # DAO 接口
│       │   │   │   └── entity/    # 实体类
│       │   │   ├── preferences/   # DataStore 偏好设置
│       │   │   └── repository/    # 数据仓库
│       │   ├── service/           # 后台服务
│       │   │   ├── AppMonitorService.kt    # AccessibilityService
│       │   │   └── UsageTrackingManager.kt # 使用时间追踪
│       │   ├── ui/                # UI 层 (Jetpack Compose)
│       │   │   ├── screens/       # 各页面
│       │   │   ├── components/    # 可复用组件
│       │   │   ├── overlay/       # 弹窗界面
│       │   │   └── theme/         # 主题配置
│       │   ├── util/              # 工具类
│       │   └── viewmodel/         # ViewModel
│       └── res/
│           ├── values/            # 英文资源
│           └── values-zh/         # 中文资源
├── docs/
│   ├── architecture.md            # 技术架构文档 (应用监控与弹窗触发机制.md)
│   ├── troubleshooting.md         # 问题排查与开发日志
│   ├── function.md                # 用户功能说明书
│   ├── logcat.md                  # Logcat 日志分析
│   └── plans/                     # 设计文档存档
├── CHANGELOG.md                   # 变更日志
├── CONTRIBUTING.md                # 本文档
└── README.md                      # 项目介绍
```

---

## 开发环境配置

### 必需工具
- **Android Studio**: Hedgehog (2023.1.1) 或更高版本
- **JDK**: 17
- **Kotlin**: 1.9+
- **Gradle**: 8.0+

### 项目配置
- **Minimum SDK**: 23 (Android 6.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34

### 关键依赖
```groovy
// Jetpack Compose
implementation "androidx.compose.ui:ui:$compose_version"
implementation "androidx.compose.material3:material3:$material3_version"

// Room
implementation "androidx.room:room-runtime:$room_version"
implementation "androidx.room:room-ktx:$room_version"
ksp "androidx.room:room-compiler:$room_version"

// DataStore
implementation "androidx.datastore:datastore-preferences:$datastore_version"

// Coroutines
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version"
```

---

## 代码规范

### Kotlin 代码风格

1. **命名规范**
   - 类名：PascalCase (`AppMonitorService`)
   - 函数名/变量名：camelCase (`checkAndShowUsageWarning`)
   - 常量：UPPER_SNAKE_CASE (`COOLDOWN_DEFAULT_MINUTES`)
   - 文件名与类名一致

2. **日志规范**
   ```kotlin
   // 使用统一的 TAG 格式
   companion object {
       private const val TAG = "SlowDown"  // 或 "SlowDownApp"
   }

   // 使用方括号标记功能模块
   Log.d(TAG, "[Service] onAccessibilityEvent: type=$eventType, pkg=$pkg")
   Log.d(TAG, "[UsageWarning] $packageName in cooldown")
   Log.d(TAG, "[VideoAppCheck] Starting timer for $packageName")
   ```

3. **协程使用**
   ```kotlin
   // 在 ViewModel 中使用 viewModelScope
   viewModelScope.launch {
       repository.updateMonitoredApp(app)
   }

   // 在 Service 中使用自定义 scope
   private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
   ```

4. **空安全处理**
   ```kotlin
   // 优先使用 ?.let 和 ?: 而非 !!
   val app = repository.getMonitoredApp(packageName) ?: return

   // 对于可能为 null 的系统 API，使用 try-catch
   val actualForeground = try {
       rootInActiveWindow?.packageName?.toString()
   } catch (e: Exception) {
       null
   }
   ```

### Compose UI 规范

1. **组件命名**
   - Screen 后缀：`DashboardScreen`, `AppDetailScreen`
   - 私有组件使用 private 修饰符
   ```kotlin
   @Composable
   fun DashboardScreen(viewModel: DashboardViewModel) { }

   @Composable
   private fun StatisticsCard(data: StatData) { }
   ```

2. **状态管理**
   ```kotlin
   // 使用 collectAsState 收集 Flow
   val countdown by viewModel.countdown.collectAsState()

   // 使用 remember 缓存计算结果
   val formattedTime = remember(totalMinutes) {
       formatTime(totalMinutes)
   }
   ```

### 数据库规范

1. **迁移脚本**
   ```kotlin
   // 每次修改 Entity 必须添加迁移
   private val MIGRATION_3_4 = object : Migration(3, 4) {
       override fun migrate(database: SupportSQLiteDatabase) {
           database.execSQL("""
               ALTER TABLE monitored_apps ADD COLUMN cooldownMinutes INTEGER DEFAULT NULL
           """.trimIndent())
       }
   }
   ```

2. **版本管理**
   - 修改 Entity 后必须增加 `version`
   - 在 `getInstance()` 中注册新的 Migration
   - 永远不要使用 `fallbackToDestructiveMigration()`（会丢失用户数据）

---

## Git 工作流

### 分支策略

```
main                # 稳定发布版本
  └── dev           # 开发分支
       ├── feature/xxx    # 新功能
       ├── fix/xxx        # Bug 修复
       └── refactor/xxx   # 重构
```

### Commit 规范

使用 [Conventional Commits](https://www.conventionalcommits.org/) 格式：

```
<type>(<scope>): <description>

[optional body]
```

**类型**：
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `refactor`: 重构（不改变功能）
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建/配置相关
- `ui`: UI 相关改动

**示例**：
```
feat(service): add short video mode with 30s timer
fix(overlay): prevent popup when user left monitored app
refactor: remove OverlayService and unify overlay launch strategy
docs: add monitoring optimization design document
```

### PR 流程

1. 从 `dev` 创建功能分支
2. 开发并测试
3. 提交 PR 到 `dev`
4. 代码审查（如有协作者）
5. 合并到 `dev`
6. 定期将 `dev` 合并到 `main` 发布

---

## 文档管理

### 文档分类

| 文档 | 用途 | 更新时机 |
|------|------|---------|
| `README.md` | 项目介绍、快速开始 | 发布新版本时 |
| `CHANGELOG.md` | 版本变更记录 | 每次发布 |
| `docs/architecture.md` | 技术架构、流程图 | 架构变更时 |
| `docs/troubleshooting.md` | 问题排查、Bug 记录 | 修复重要 Bug 时 |
| `docs/function.md` | 用户功能说明 | 功能变更时 |
| `docs/plans/*.md` | 设计文档存档 | 新功能设计时 |

### 文档规范

1. **使用中文编写**（目标用户为中文开发者）
2. **包含代码示例**（便于理解和复制）
3. **使用 Mermaid 流程图**（Markdown 原生支持）
4. **记录"为什么"而非仅"是什么"**

### 临时文件处理

- 临时文件（Claude Code 会话记录等）应添加到 `.gitignore`
- 有价值的内容提取到正式文档后再删除
- 保留 `logcat.md` 作为调试参考

---

## 测试策略

### 单元测试

```kotlin
// 使用 JUnit 5 + MockK
@Test
fun `checkCooldown returns false when in cooldown period`() {
    // Given
    val cooldownMap = ConcurrentHashMap<String, Long>()
    cooldownMap["com.test.app"] = System.currentTimeMillis()

    // When
    val result = checkCooldown("com.test.app", cooldownMap, 5)

    // Then
    assertFalse(result)
}
```

### 集成测试

```kotlin
// 使用 AndroidX Test
@RunWith(AndroidJUnit4::class)
class UsageTrackingManagerTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun syncUsageData_updatesDatabase() = runTest {
        // 测试 UsageStatsManager 数据同步
    }
}
```

### 手动测试清单

**每次发布前必须测试**：
- [ ] 无限制应用 + 软提醒模式
- [ ] 无限制应用 + 强制模式
- [ ] 有限制应用 < 80%
- [ ] 有限制应用 ≥ 80%
- [ ] 有限制应用 ≥ 100% + 软模式
- [ ] 有限制应用 ≥ 100% + 强制模式
- [ ] 短视频模式（抖音/B站）
- [ ] MIUI 设备兼容性
- [ ] Samsung One UI 兼容性

---

## 发布流程

### 版本号规则

使用 [SemVer](https://semver.org/)：`MAJOR.MINOR.PATCH`

- **MAJOR**: 不兼容的 API 变更
- **MINOR**: 新功能（向后兼容）
- **PATCH**: Bug 修复

### 发布步骤

1. **更新版本号**
   ```groovy
   // app/build.gradle.kts
   versionCode = 2
   versionName = "1.1.0"
   ```

2. **更新 CHANGELOG.md**
   - 移动 [Unreleased] 内容到新版本号下
   - 添加发布日期

3. **构建签名 APK**
   ```bash
   ./gradlew assembleRelease
   ```

4. **创建 Git Tag**
   ```bash
   git tag -a v1.1.0 -m "Release v1.1.0"
   git push origin v1.1.0
   ```

5. **发布到 GitHub Releases**
   - 上传 APK
   - 复制 CHANGELOG 内容作为 Release Notes

---

## 常见问题

### Q: 如何调试 AccessibilityService？

1. 在 `AppMonitorService` 中添加详细日志
2. 使用 `adb logcat -s SlowDown:D` 过滤日志
3. 参考 `docs/logcat.md` 的日志格式

### Q: 如何处理数据库迁移失败？

1. 检查迁移脚本 SQL 语法
2. 测试从旧版本升级
3. 使用 `fallbackToDestructiveMigrationOnDowngrade()` 仅处理降级

### Q: 如何测试 MIUI 兼容性？

1. 使用 MIUI 真机测试
2. 检查前台服务通知是否显示
3. 验证 AccessibilityService 不被冻结
4. 参考 `docs/troubleshooting.md` 的 MIUI 章节

---

*本文档会随项目发展持续更新*
