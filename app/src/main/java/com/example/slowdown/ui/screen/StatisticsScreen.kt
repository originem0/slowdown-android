package com.example.slowdown.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.slowdown.viewmodel.AppUsageData
import com.example.slowdown.viewmodel.DayUsageData
import com.example.slowdown.viewmodel.StatisticsViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val todayTotalMinutes by viewModel.todayTotalMinutes.collectAsState()
    val todayUsageByApp by viewModel.todayUsageByApp.collectAsState()
    val weeklyUsage by viewModel.weeklyUsage.collectAsState()
    val monthTotalMinutes by viewModel.monthTotalMinutes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计") }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // 今日总计卡片
                item {
                    TodayTotalCard(
                        totalMinutes = todayTotalMinutes,
                        appUsages = todayUsageByApp,
                        formatDuration = viewModel::formatDuration
                    )
                }

                // 本周趋势卡片
                item {
                    WeeklyTrendCard(
                        weeklyUsage = weeklyUsage,
                        formatDuration = viewModel::formatDuration
                    )
                }

                // 各应用使用详情卡片
                item {
                    AppUsageDetailCard(
                        appUsages = todayUsageByApp,
                        totalMinutes = todayTotalMinutes,
                        formatDuration = viewModel::formatDuration
                    )
                }

                // 本月总计
                item {
                    MonthTotalCard(
                        monthTotalMinutes = monthTotalMinutes,
                        formatDuration = viewModel::formatDuration
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) } // 为底部导航留空间
            }
        }
    }
}

@Composable
private fun TodayTotalCard(
    totalMinutes: Int,
    appUsages: List<AppUsageData>,
    formatDuration: (Int) -> String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "今日总计",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = formatDuration(totalMinutes),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (appUsages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                // 简单饼图展示各应用占比
                SimplePieChart(
                    data = appUsages,
                    totalMinutes = totalMinutes,
                    modifier = Modifier.size(150.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 图例
                PieChartLegend(
                    appUsages = appUsages.take(5), // 最多显示5个应用
                    totalMinutes = totalMinutes
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "暂无使用数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SimplePieChart(
    data: List<AppUsageData>,
    totalMinutes: Int,
    modifier: Modifier = Modifier
) {
    if (totalMinutes <= 0 || data.isEmpty()) return

    val colors = remember {
        listOf(
            Color(0xFF6366F1), // Indigo
            Color(0xFF8B5CF6), // Violet
            Color(0xFFEC4899), // Pink
            Color(0xFFF59E0B), // Amber
            Color(0xFF10B981), // Emerald
            Color(0xFF3B82F6), // Blue
            Color(0xFFEF4444), // Red
            Color(0xFF84CC16)  // Lime
        )
    }

    Canvas(modifier = modifier) {
        val strokeWidth = 30f
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        var startAngle = -90f

        data.forEachIndexed { index, appUsage ->
            val sweepAngle = (appUsage.usageMinutes.toFloat() / totalMinutes) * 360f
            val color = colors[index % colors.size]

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )

            startAngle += sweepAngle
        }
    }
}

@Composable
private fun PieChartLegend(
    appUsages: List<AppUsageData>,
    totalMinutes: Int
) {
    val colors = remember {
        listOf(
            Color(0xFF6366F1),
            Color(0xFF8B5CF6),
            Color(0xFFEC4899),
            Color(0xFFF59E0B),
            Color(0xFF10B981)
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        appUsages.forEachIndexed { index, app ->
            val percent = if (totalMinutes > 0) {
                (app.usageMinutes.toFloat() / totalMinutes * 100).toInt()
            } else 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 颜色指示器
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(
                        color = colors[index % colors.size],
                        radius = size.minDimension / 2
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeeklyTrendCard(
    weeklyUsage: List<DayUsageData>,
    formatDuration: (Int) -> String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "本周趋势",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (weeklyUsage.isEmpty()) {
                Text(
                    text = "暂无数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                // 柱状图
                WeeklyBarChart(
                    data = weeklyUsage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 星期标签
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    weeklyUsage.forEach { day ->
                        Text(
                            text = day.dayOfWeek,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 本周总计
                val weekTotal = weeklyUsage.sumOf { it.totalMinutes }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "本周总计: ${formatDuration(weekTotal)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun WeeklyBarChart(
    data: List<DayUsageData>,
    modifier: Modifier = Modifier
) {
    val maxMinutes = data.maxOfOrNull { it.totalMinutes } ?: 1
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        val barCount = data.size
        val barWidth = size.width / (barCount * 2)
        val maxBarHeight = size.height * 0.85f
        val bottomPadding = size.height * 0.05f

        data.forEachIndexed { index, day ->
            val barHeight = if (maxMinutes > 0) {
                (day.totalMinutes.toFloat() / maxMinutes) * maxBarHeight
            } else 0f

            val x = barWidth * (index * 2 + 0.5f)
            val y = size.height - barHeight - bottomPadding

            // 背景条
            drawRect(
                color = surfaceVariant,
                topLeft = Offset(x, bottomPadding),
                size = Size(barWidth, maxBarHeight)
            )

            // 实际数据条
            if (barHeight > 0) {
                drawRect(
                    color = primaryColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight)
                )
            }
        }
    }
}

@Composable
private fun AppUsageDetailCard(
    appUsages: List<AppUsageData>,
    totalMinutes: Int,
    formatDuration: (Int) -> String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "各应用使用详情",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (appUsages.isEmpty()) {
                Text(
                    text = "暂无使用数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                appUsages.forEach { app ->
                    AppUsageRow(
                        app = app,
                        totalMinutes = totalMinutes,
                        formatDuration = formatDuration
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun AppUsageRow(
    app: AppUsageData,
    totalMinutes: Int,
    formatDuration: (Int) -> String
) {
    val percent = if (totalMinutes > 0) {
        (app.usageMinutes.toFloat() / totalMinutes * 100).toInt()
    } else 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon
        if (app.appInfo?.icon != null) {
            Image(
                painter = rememberDrawablePainter(drawable = app.appInfo.icon),
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        } else {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName.take(1),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatDuration(app.usageMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { (percent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp),
                    color = getProgressColor(percent),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun getProgressColor(percent: Int): Color {
    return when {
        percent >= 75 -> MaterialTheme.colorScheme.error
        percent >= 50 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
private fun MonthTotalCard(
    monthTotalMinutes: Int,
    formatDuration: (Int) -> String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "本月总计",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatDuration(monthTotalMinutes),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
