# 统计页面 UI 优化实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 重新设计统计页面 UI，参考 Google Digital Wellbeing 风格，修复数据问题

**Architecture:** 渐进式优化，保留现有代码结构，逐步替换 UI 组件

**Tech Stack:** Jetpack Compose, Material3, Canvas 绑图

---

## 需求总结

### 问题
1. **数据问题**：本周/上周数据混淆
2. **UI 问题**：布局混乱、视觉不美观、交互功能少、性能体验差

### 目标
- 参考 Google Digital Wellbeing 设计风格
- 圆形进度图展示今日总计
- 条形图展示本周趋势
- 日期选择器支持查看历史数据
- 各应用限额进度清晰展示

---

## 页面布局设计

```
┌─────────────────────────────────────┐
│  ← 今日  2026年1月17日  →          │  ← 日期选择器
├─────────────────────────────────────┤
│                                     │
│         [圆形进度图]                │  ← 外圈：各应用占比
│          2小时45分                  │     中心：今日总计
│        ▲ 比昨天多15分              │     底部：与昨天对比
│                                     │
├─────────────────────────────────────┤
│  本周趋势                           │
│  ┌─────────────────────────────┐   │
│  │  周一 ████████        2h    │   │  ← 本周 7 天横向条形图
│  │  周二 ██████          1.5h  │   │     今天高亮显示
│  │  ...                        │   │
│  └─────────────────────────────┘   │
├─────────────────────────────────────┤
│  各应用详情                         │
│  ┌─────────────────────────────┐   │
│  │ [图标] 抖音                  │   │
│  │ ████████████░░░░  45分/60分 │   │  ← 有限额：显示进度条
│  │                              │   │
│  │ [图标] B站                  │   │
│  │ 90分                        │   │  ← 无限额：纯文字
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

---

## 色彩方案

| 元素 | 颜色 | 说明 |
|------|------|------|
| 主进度色 | `#4285F4` | 柔和蓝，用于主要进度 |
| 应用颜色 | 多色系 | 蓝/绿/橙/紫/红，区分不同应用 |
| 背景 | `#F5F5F5` | 浅灰背景 |
| 卡片 | `#FFFFFF` | 白色 + 轻微阴影 |
| 主文字 | `#333333` | 深灰 |
| 次要文字 | `#666666` | 中灰 |
| 增加提示 | `#E53935` | 红色箭头 ▲ |
| 减少提示 | `#43A047` | 绿色箭头 ▼ |

---

## 实现任务

### Task 1: 修复周边界计算逻辑

**Files:**
- Modify: `app/src/main/java/com/example/slowdown/viewmodel/StatisticsViewModel.kt`

**问题分析:**
当前可能使用 `Calendar.WEEK_OF_YEAR` 或类似方法计算周，需要检查：
1. 一周的开始是周一还是周日
2. 跨年周的处理

**Step 1: 检查当前周计算逻辑**

查找 `StatisticsViewModel.kt` 中的周数据计算代码，确认问题。

**Step 2: 修复为正确的周边界**

使用 `java.time.LocalDate` 和 `java.time.DayOfWeek.MONDAY` 确保：
- 周一为一周的开始
- 周日为一周的结束

```kotlin
// 获取本周的起止日期
fun getWeekRange(date: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> {
    // 找到本周的周一
    val monday = date.with(DayOfWeek.MONDAY)
    // 本周日
    val sunday = monday.plusDays(6)
    return Pair(monday, sunday)
}
```

**Step 3: 验证修复**

打印日志确认周边界正确。

---

### Task 2: 创建通用 UI 组件

**Files:**
- Create: `app/src/main/java/com/example/slowdown/ui/components/CircularProgressChart.kt`
- Create: `app/src/main/java/com/example/slowdown/ui/components/HorizontalBarChart.kt`
- Create: `app/src/main/java/com/example/slowdown/ui/components/DateSelector.kt`

#### 2.1 圆形进度图组件

```kotlin
@Composable
fun CircularProgressChart(
    segments: List<ChartSegment>,  // 各应用的占比和颜色
    totalText: String,             // 中心显示的文字 "2小时45分"
    comparisonText: String?,       // 比较文字 "▲ 比昨天多15分"
    isIncrease: Boolean?,          // true=增加(红) false=减少(绿) null=不显示
    modifier: Modifier = Modifier
)

data class ChartSegment(
    val value: Float,      // 占比 0-1
    val color: Color,      // 颜色
    val label: String      // 应用名
)
```

**实现要点:**
- 使用 Canvas 绑制外圈弧段
- 中心使用 Box + Text 显示总时长
- 底部显示对比文字（带箭头和颜色）

#### 2.2 横向条形图组件

```kotlin
@Composable
fun HorizontalBarChart(
    data: List<BarData>,
    maxValue: Float,           // 最大值（用于计算比例）
    highlightIndex: Int = -1,  // 高亮的条目索引（今天）
    modifier: Modifier = Modifier
)

data class BarData(
    val label: String,    // "周一"
    val value: Float,     // 分钟数
    val displayText: String  // "2h"
)
```

#### 2.3 日期选择器组件

```kotlin
@Composable
fun DateSelector(
    currentDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
)
```

**实现要点:**
- 左右箭头按钮
- 中间显示日期文字
- 点击箭头切换日期（±1 天）

---

### Task 3: 更新 ViewModel 支持日期参数

**Files:**
- Modify: `app/src/main/java/com/example/slowdown/viewmodel/StatisticsViewModel.kt`

**Step 1: 添加日期状态**

```kotlin
private val _selectedDate = MutableStateFlow(LocalDate.now())
val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

fun selectDate(date: LocalDate) {
    _selectedDate.value = date
    loadDataForDate(date)
}
```

**Step 2: 修改数据加载逻辑**

根据选择的日期加载对应数据：
- 今日使用数据
- 昨日数据（用于对比）
- 本周数据（基于选择日期所在的周）

---

### Task 4: 重构 StatisticsScreen 布局

**Files:**
- Modify: `app/src/main/java/com/example/slowdown/ui/screen/StatisticsScreen.kt`

**Step 1: 替换顶部为日期选择器**

```kotlin
DateSelector(
    currentDate = selectedDate,
    onDateChange = { viewModel.selectDate(it) }
)
```

**Step 2: 替换今日统计为圆形进度图**

```kotlin
CircularProgressChart(
    segments = appSegments,
    totalText = formatDuration(todayTotalMinutes),
    comparisonText = comparisonText,
    isIncrease = todayTotalMinutes > yesterdayTotalMinutes
)
```

**Step 3: 替换本周趋势为条形图**

```kotlin
HorizontalBarChart(
    data = weeklyBarData,
    maxValue = maxDailyUsage,
    highlightIndex = todayIndex
)
```

**Step 4: 优化各应用详情列表**

- 有限额：显示进度条 + "45分/60分"
- 无限额：显示纯使用时长

---

### Task 5: 性能优化

**Files:**
- Modify: `app/src/main/java/com/example/slowdown/ui/screen/StatisticsScreen.kt`
- Modify: `app/src/main/java/com/example/slowdown/viewmodel/StatisticsViewModel.kt`

**优化点:**
1. 使用 `remember` 缓存计算结果
2. 避免不必要的 recomposition
3. 数据库查询在 IO 线程执行
4. 图表数据使用 `derivedStateOf`

---

## 验证步骤

1. **数据验证**：确认本周数据只包含本周的记录
2. **UI 验证**：确认布局符合设计稿
3. **交互验证**：日期切换功能正常
4. **性能验证**：加载速度无明显卡顿

---

## 预估工作量

| Task | 预估时间 |
|------|---------|
| Task 1: 修复周边界 | 30 分钟 |
| Task 2: 创建 UI 组件 | 2-3 小时 |
| Task 3: 更新 ViewModel | 1 小时 |
| Task 4: 重构布局 | 1-2 小时 |
| Task 5: 性能优化 | 30 分钟 |

**总计**: 约 5-7 小时
