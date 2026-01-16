package com.example.slowdown.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.slowdown.data.local.entity.MonitoredApp
import com.example.slowdown.util.AppInfo
import com.example.slowdown.viewmodel.AppListViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAppDetail: (String) -> Unit = {}
) {
    val installedApps by viewModel.installedApps.collectAsState()
    val monitoredApps by viewModel.monitoredApps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val usageMap by viewModel.usageMap.collectAsState()

    val monitoredPackages = remember(monitoredApps) {
        monitoredApps.map { it.packageName }.toSet()
    }

    val monitoredAppMap = remember(monitoredApps) {
        monitoredApps.associateBy { it.packageName }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择监控应用") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(installedApps, key = { it.packageName }) { app ->
                    val isMonitored = monitoredPackages.contains(app.packageName)
                    val monitoredApp = monitoredAppMap[app.packageName]
                    val todayUsage = usageMap[app.packageName] ?: 0

                    AppListItem(
                        app = app,
                        isMonitored = isMonitored,
                        monitoredApp = monitoredApp,
                        todayUsage = todayUsage,
                        onToggle = { viewModel.toggleApp(app, it) },
                        onClick = {
                            if (isMonitored) {
                                onNavigateToAppDetail(app.packageName)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppListItem(
    app: AppInfo,
    isMonitored: Boolean,
    monitoredApp: MonitoredApp?,
    todayUsage: Int,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(enabled = isMonitored, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isMonitored)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            if (app.icon != null) {
                Image(
                    painter = rememberDrawablePainter(drawable = app.icon),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(app.appName.take(1), style = MaterialTheme.typography.titleLarge)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // App info and progress
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium
                )

                if (isMonitored && monitoredApp != null) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Usage info
                    val dailyLimit = monitoredApp.dailyLimitMinutes
                    if (dailyLimit != null && dailyLimit > 0) {
                        Text(
                            text = "今日 $todayUsage/$dailyLimit 分钟",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Progress bar
                        val progress = (todayUsage.toFloat() / dailyLimit).coerceIn(0f, 1f)
                        val progressColor = when {
                            progress >= 1f -> MaterialTheme.colorScheme.error
                            progress >= 0.8f -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    } else {
                        Text(
                            text = "今日 $todayUsage 分钟 (无限制)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Intervention mode info
                    val modeText = when (monitoredApp.limitMode) {
                        "strict" -> "强制关闭"
                        else -> "软提醒"
                    }
                    val redirectText = if (monitoredApp.redirectPackage != null) {
                        " | 跳转: ${getAppNameFromPackage(monitoredApp.redirectPackage)}"
                    } else ""

                    Text(
                        text = "倒计时: ${monitoredApp.countdownSeconds}秒 | $modeText$redirectText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Toggle switch
            Switch(
                checked = isMonitored,
                onCheckedChange = { onToggle(isMonitored) }
            )
        }
    }
}

// Helper function to get app name from package (simplified)
private fun getAppNameFromPackage(packageName: String): String {
    return packageName.substringAfterLast('.')
        .replaceFirstChar { it.uppercase() }
}
