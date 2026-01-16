package com.example.slowdown.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.slowdown.data.local.dao.AppStat
import com.example.slowdown.data.local.dao.DailyStat
import com.example.slowdown.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAppList: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val todayCount by viewModel.todayCount.collectAsState()
    val savedMinutes by viewModel.todaySavedMinutes.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val topApps by viewModel.topApps.collectAsState()
    val serviceEnabled by viewModel.serviceEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SlowDown") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                TodayStatsCard(
                    count = todayCount,
                    savedMinutes = savedMinutes,
                    serviceEnabled = serviceEnabled,
                    onToggleService = { viewModel.setServiceEnabled(it) }
                )
            }

            item { WeeklyChartCard(stats = weeklyStats) }

            item { TopAppsCard(apps = topApps) }

            item {
                Button(
                    onClick = onNavigateToAppList,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("管理监控应用")
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun TodayStatsCard(
    count: Int,
    savedMinutes: Int,
    serviceEnabled: Boolean,
    onToggleService: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (serviceEnabled) "监控中" else "已暂停",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (serviceEnabled) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.error
                )
                Switch(checked = serviceEnabled, onCheckedChange = onToggleService)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "今日已拦截",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$count 次",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "节省约 $savedMinutes 分钟",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyChartCard(stats: List<DailyStat>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = "本周趋势", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            if (stats.isEmpty()) {
                Text(
                    text = "暂无数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                SimpleBarChart(
                    data = stats,
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            }
        }
    }
}

@Composable
private fun SimpleBarChart(data: List<DailyStat>, modifier: Modifier = Modifier) {
    val maxValue = data.maxOfOrNull { it.count } ?: 1
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val barWidth = size.width / (data.size * 2)
        val maxHeight = size.height * 0.8f

        data.forEachIndexed { index, stat ->
            val barHeight = if (maxValue > 0) (stat.count.toFloat() / maxValue) * maxHeight else 0f
            val x = barWidth * (index * 2 + 0.5f)
            val y = size.height - barHeight

            drawRect(
                color = primaryColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )
        }
    }
}

@Composable
private fun TopAppsCard(apps: List<AppStat>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = "最常拦截", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (apps.isEmpty()) {
                Text(
                    text = "暂无数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                apps.forEach { app ->
                    AppStatRow(app = app, maxCount = apps.first().count)
                }
            }
        }
    }
}

@Composable
private fun AppStatRow(app: AppStat, maxCount: Int) {
    val progress = if (maxCount > 0) app.count.toFloat() / maxCount else 0f

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = app.appName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(80.dp)
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.weight(1f).height(8.dp).padding(horizontal = 8.dp),
        )
        Text(
            text = "${app.count}次",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(40.dp)
        )
    }
}
