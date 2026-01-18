package com.example.slowdown.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.slowdown.ui.components.*
import com.example.slowdown.ui.theme.ChartColors
import com.example.slowdown.viewmodel.AppUsageData
import com.example.slowdown.viewmodel.DayUsageData
import com.example.slowdown.viewmodel.StatisticsViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlin.math.abs
import androidx.compose.ui.res.stringResource
import com.example.slowdown.R

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val formattedDate by viewModel.formattedDate.collectAsState()
    val todayTotalMinutes by viewModel.todayTotalMinutes.collectAsState()
    val yesterdayTotalMinutes by viewModel.yesterdayTotalMinutes.collectAsState()
    val todayUsageByApp by viewModel.todayUsageByApp.collectAsState()
    val weeklyUsage by viewModel.weeklyUsage.collectAsState()
    val monthTotalMinutes by viewModel.monthTotalMinutes.collectAsState()

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface) // Unified flat background
    ) {
        // 日期选择器
        item {
            DateSelector(
                formattedDate = formattedDate,
                onPreviousDay = { viewModel.selectPreviousDay() },
                onNextDay = { viewModel.selectNextDay() },
                canSelectNext = viewModel.canSelectNextDay(),
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            )
            ListDivider(startIndent = 0.dp)
        }

        // 今日总计 - 圆形进度图
        item {
            TodayCircularSection(
                totalMinutes = todayTotalMinutes,
                yesterdayMinutes = yesterdayTotalMinutes,
                appUsages = todayUsageByApp,
                formatDuration = viewModel::formatDuration
            )
        }

        // 各应用使用详情 (Now first)
        item {
            SectionTitle(title = stringResource(R.string.app_details), paddingTop = 8.dp)
        }

        if (todayUsageByApp.isEmpty()) {
            item {
                EmptyState(
                    message = stringResource(R.string.no_usage_today),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        } else {
            items(todayUsageByApp) { app ->
                AppUsageItem(
                    app = app,
                    formatDuration = viewModel::formatDuration
                )
                ListDivider(startIndent = 68.dp)
            }
        }

        // 本周趋势 (Now second)
        item {
            SectionTitle(title = stringResource(R.string.weekly_trend), paddingTop = 16.dp)
        }

        item {
            WeeklyBarSection(
                weeklyUsage = weeklyUsage,
                formatDuration = viewModel::formatDuration
            )
        }

        // 本月总计
        item {
            SectionTitle(title = stringResource(R.string.this_month), paddingTop = 8.dp)
        }

        item {
            MonthSection(
                monthTotalMinutes = monthTotalMinutes,
                formatDuration = viewModel::formatDuration
            )
        }

        // 底部留白
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun TodayCircularSection(
    totalMinutes: Int,
    yesterdayMinutes: Int,
    appUsages: List<AppUsageData>,
    formatDuration: (Int) -> String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Removed background(surface) as it is now redundant and we want flat look
            .padding(vertical = 32.dp), // Increased vertical spacing for breathable layout
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 计算与昨天的对比
        val diff = totalMinutes - yesterdayMinutes
        val isIncrease = diff > 0
        val comparisonText = if (diff != 0) {
            if (isIncrease) {
                stringResource(R.string.compared_to_yesterday_more, formatDuration(abs(diff)))
            } else {
                stringResource(R.string.compared_to_yesterday_less, formatDuration(abs(diff)))
            }
        } else null

        // 创建图表段
        val segments = remember(appUsages, totalMinutes) {
            if (totalMinutes > 0) {
                appUsages.mapIndexed { index, app ->
                    ChartSegment(
                        value = app.usageMinutes.toFloat() / totalMinutes,
                        color = ChartColors[index % ChartColors.size],
                        label = app.appName
                    )
                }
            } else emptyList()
        }

        // 圆形进度图
        CircularProgressChart(
            segments = segments,
            totalText = formatDuration(totalMinutes),
            comparisonText = comparisonText,
            isIncrease = if (diff != 0) isIncrease else null,
            size = 180.dp,
            strokeWidth = 20.dp
        )

        // 图例
        if (appUsages.isNotEmpty() && totalMinutes > 0) {
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    appUsages.take(4).chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEachIndexed { _, app ->
                                val index = appUsages.indexOf(app)
                                val percent = (app.usageMinutes.toFloat() / totalMinutes * 100).toInt()
                                LegendItem(
                                    color = ChartColors[index % ChartColors.size],
                                    name = app.appName,
                                    percent = percent,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // 如果行中只有一个元素，添加空白占位
                            if (row.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    name: String,
    percent: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 60.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeeklyBarSection(
    weeklyUsage: List<DayUsageData>,
    formatDuration: (Int) -> String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Flat design - no separate background
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (weeklyUsage.isEmpty()) {
            EmptyState(message = stringResource(R.string.no_data_yet))
        } else {
            // 转换为 BarData
            val maxMinutes = weeklyUsage.maxOfOrNull { it.totalMinutes }?.toFloat() ?: 1f
            val barData = remember(weeklyUsage) {
                weeklyUsage.map { day ->
                    BarData(
                        label = day.dayOfWeek,
                        value = day.totalMinutes.toFloat(),
                        displayText = formatDuration(day.totalMinutes),
                        isSelected = day.isSelected
                    )
                }
            }

            HorizontalBarChart(
                data = barData,
                maxValue = maxMinutes,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 本周总计
            val weekTotal = weeklyUsage.sumOf { it.totalMinutes }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.weekly_total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDuration(weekTotal),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AppUsageItem(
    app: AppUsageData,
    formatDuration: (Int) -> String
) {
    val hasLimit = app.dailyLimitMinutes != null && app.dailyLimitMinutes > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon
        if (app.appInfo?.icon != null) {
            Image(
                painter = rememberDrawablePainter(drawable = app.appInfo.icon),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // 应用名
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (hasLimit) {
                // 有限额：显示进度条
                val progress = app.getUsagePercent()
                val progressColor = when {
                    progress >= 1f -> MaterialTheme.colorScheme.error
                    progress >= 0.8f -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(3.dp))
                                .background(progressColor)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 显示 "45分/60分" 格式
                    Text(
                        text = "${formatDuration(app.usageMinutes)}/${formatDuration(app.dailyLimitMinutes ?: 0)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = progressColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // 无限额：纯文字显示
                Text(
                    text = formatDuration(app.usageMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun MonthSection(
    monthTotalMinutes: Int,
    formatDuration: (Int) -> String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        ValueListItem(
            title = stringResource(R.string.month_total_usage),
            value = formatDuration(monthTotalMinutes),
            valueColor = MaterialTheme.colorScheme.secondary
        )
        ListDivider(startIndent = 0.dp)
    }
}
