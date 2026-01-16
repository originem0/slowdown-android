package com.example.slowdown.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.slowdown.util.AppInfo
import com.example.slowdown.viewmodel.AppDetailViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    viewModel: AppDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val monitoredApp by viewModel.monitoredApp.collectAsState()
    val appInfo by viewModel.appInfo.collectAsState()
    val todayUsage by viewModel.todayUsage.collectAsState()
    val recentUsage by viewModel.recentUsage.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()

    val app = monitoredApp

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${app?.appName ?: "应用"} 设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (app == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // App Header
                item {
                    AppHeaderCard(appInfo = appInfo, appName = app.appName)
                }

                // Intervention Settings Section
                item {
                    SectionHeader(title = "干预设置")
                }

                item {
                    CountdownSliderCard(
                        currentValue = app.countdownSeconds,
                        onValueChange = { viewModel.updateCountdown(it) }
                    )
                }

                item {
                    RedirectAppSelector(
                        currentRedirect = app.redirectPackage,
                        installedApps = installedApps,
                        currentPackage = app.packageName,
                        onRedirectSelected = { viewModel.updateRedirectApp(it) }
                    )
                }

                // Time Limit Section
                item {
                    SectionHeader(title = "时间限制")
                }

                item {
                    DailyLimitCard(
                        currentLimit = app.dailyLimitMinutes,
                        onLimitChange = { viewModel.updateDailyLimit(it) }
                    )
                }

                item {
                    LimitModeSelector(
                        currentMode = app.limitMode,
                        onModeChange = { viewModel.updateLimitMode(it) }
                    )
                }

                // Usage Statistics Section
                item {
                    SectionHeader(title = "使用统计")
                }

                item {
                    UsageStatsCard(
                        todayUsage = todayUsage,
                        weeklyAverage = if (recentUsage.isNotEmpty()) {
                            recentUsage.sumOf { it.usageMinutes } / recentUsage.size
                        } else 0
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun AppHeaderCard(appInfo: AppInfo?, appName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (appInfo?.icon != null) {
                Image(
                    painter = rememberDrawablePainter(drawable = appInfo.icon),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
            } else {
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = appName.take(1),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (appInfo != null) {
                    Text(
                        text = appInfo.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
    HorizontalDivider()
}

@Composable
private fun CountdownSliderCard(
    currentValue: Int,
    onValueChange: (Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "倒计时",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "$currentValue 秒",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = currentValue.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 3f..30f,
                steps = 26
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RedirectAppSelector(
    currentRedirect: String?,
    installedApps: List<AppInfo>,
    currentPackage: String,
    onRedirectSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "跳转应用",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                val selectedApp = installedApps.find { it.packageName == currentRedirect }

                OutlinedTextField(
                    value = selectedApp?.appName ?: "无",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("无") },
                        onClick = {
                            onRedirectSelected(null)
                            expanded = false
                        }
                    )

                    installedApps
                        .filter { it.packageName != currentPackage }
                        .take(20) // Limit to avoid performance issues
                        .forEach { app ->
                            DropdownMenuItem(
                                text = { Text(app.appName) },
                                onClick = {
                                    onRedirectSelected(app.packageName)
                                    expanded = false
                                }
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun DailyLimitCard(
    currentLimit: Int?,
    onLimitChange: (Int?) -> Unit
) {
    var isUnlimited by remember(currentLimit) { mutableStateOf(currentLimit == null) }
    var sliderValue by remember(currentLimit) { mutableStateOf((currentLimit ?: 30).toFloat()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "每日限额",
                    style = MaterialTheme.typography.bodyLarge
                )
                if (!isUnlimited) {
                    Text(
                        text = "${sliderValue.toInt()} 分钟",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!isUnlimited) {
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = {
                        onLimitChange(sliderValue.toInt())
                    },
                    valueRange = 5f..120f,
                    steps = 22
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isUnlimited,
                    onClick = {
                        isUnlimited = true
                        onLimitChange(null)
                    }
                )
                Text(
                    text = "无限制",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun LimitModeSelector(
    currentMode: String,
    onModeChange: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .selectableGroup()
        ) {
            Text(
                text = "超时模式",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = currentMode == "soft",
                        onClick = { onModeChange("soft") },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentMode == "soft",
                    onClick = null
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = "软提醒",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "超时后提醒，但允许继续使用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = currentMode == "strict",
                        onClick = { onModeChange("strict") },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentMode == "strict",
                    onClick = null
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = "强制关闭",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "超时后今日无法再使用该应用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun UsageStatsCard(
    todayUsage: Int,
    weeklyAverage: Int
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "今日使用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$todayUsage 分钟",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "本周平均",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$weeklyAverage 分钟/天",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
