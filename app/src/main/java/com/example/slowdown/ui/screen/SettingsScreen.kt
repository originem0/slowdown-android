package com.example.slowdown.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.slowdown.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val defaultCountdown by viewModel.defaultCountdown.collectAsState()
    val cooldownMinutes by viewModel.cooldownMinutes.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item {
                Text(
                    text = "权限设置",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            item {
                PermissionItem(
                    title = "无障碍服务",
                    description = "用于检测应用启动",
                    isEnabled = permissionState.accessibilityEnabled,
                    onClick = { viewModel.openAccessibilitySettings() }
                )
            }

            item {
                PermissionItem(
                    title = "悬浮窗权限",
                    description = "用于显示干预界面",
                    isEnabled = permissionState.overlayEnabled,
                    onClick = { viewModel.openOverlaySettings() }
                )
            }

            item {
                PermissionItem(
                    title = "忽略电池优化",
                    description = "防止服务被系统杀死",
                    isEnabled = permissionState.batteryOptimizationDisabled,
                    onClick = { viewModel.openBatterySettings() }
                )
            }

            item {
                PermissionItem(
                    title = "使用统计权限",
                    description = "用于获取应用使用时长数据",
                    isEnabled = permissionState.usageStatsEnabled,
                    onClick = { viewModel.openUsageStatsSettings() }
                )
            }

            if (permissionState.isMiui) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "MIUI 专属设置（必须开启）",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (permissionState.miuiPermissionsNeeded)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                item {
                    MiuiPermissionItem(
                        title = "自启动权限",
                        description = "允许应用开机自启",
                        isConfirmed = permissionState.miuiAutoStartConfirmed,
                        onOpenSettings = { viewModel.openMiuiAutoStartSettings() },
                        onConfirm = { viewModel.confirmMiuiAutoStart() }
                    )
                }

                item {
                    // 使用实际权限检测状态
                    MiuiBackgroundPopupItem(
                        title = "后台弹出界面",
                        description = "允许后台弹出干预界面（核心功能！必须开启）",
                        isGranted = permissionState.miuiBackgroundPopupGranted,
                        onOpenSettings = { viewModel.openMiuiBackgroundPopupSettings() },
                        onRefresh = { viewModel.refreshPermissions() }
                    )
                }

                item {
                    MiuiPermissionItem(
                        title = "省电策略设为「无限制」",
                        description = "防止后台服务被冻结（关键！）",
                        isConfirmed = permissionState.miuiBatterySaverConfirmed,
                        onOpenSettings = { viewModel.openMiuiBatterySettings() },
                        onConfirm = { viewModel.confirmMiuiBatterySaver() }
                    )
                }

                item {
                    MiuiManualItem(
                        title = "锁定应用",
                        description = "在最近任务中下拉 SlowDown 卡片，点击「锁定」",
                        isConfirmed = permissionState.miuiLockAppConfirmed,
                        onConfirm = { viewModel.confirmMiuiLockApp() }
                    )
                }

                if (!permissionState.miuiBackgroundPopupGranted) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "必须开启「后台弹出界面」权限！",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "操作步骤：\n" +
                                           "1. 点击上方「去开启」\n" +
                                           "2. 找到「其他权限」或「权限管理」\n" +
                                           "3. 找到「后台弹出界面」或「显示悬浮窗时在后台弹出界面」\n" +
                                           "4. 设置为「允许」",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "干预设置",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            item {
                SliderSettingItem(
                    title = "默认倒计时",
                    value = defaultCountdown,
                    valueRange = 3f..30f,
                    unit = "秒",
                    onValueChange = { viewModel.setDefaultCountdown(it) }
                )
            }

            item {
                SliderSettingItem(
                    title = "冷却时间",
                    value = cooldownMinutes,
                    valueRange = 1f..30f,
                    unit = "分钟",
                    onValueChange = { viewModel.setCooldownMinutes(it) }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        leadingContent = {
            Icon(
                imageVector = if (isEnabled) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isEnabled) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.error
            )
        },
        trailingContent = {
            if (!isEnabled) {
                TextButton(onClick = onClick) {
                    Text("去开启")
                }
            }
        },
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun SliderSettingItem(
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = "$value $unit", style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = valueRange,
            steps = (valueRange.endInclusive - valueRange.start).toInt() - 1
        )
    }
}

@Composable
private fun MiuiPermissionItem(
    title: String,
    description: String,
    isConfirmed: Boolean,
    onOpenSettings: () -> Unit,
    onConfirm: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        leadingContent = {
            Icon(
                imageVector = if (isConfirmed) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isConfirmed) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.error
            )
        },
        trailingContent = {
            if (!isConfirmed) {
                Row {
                    TextButton(onClick = onOpenSettings) {
                        Text("去开启")
                    }
                    TextButton(onClick = onConfirm) {
                        Text("已开启")
                    }
                }
            }
        },
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun MiuiManualItem(
    title: String,
    description: String,
    isConfirmed: Boolean,
    onConfirm: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        leadingContent = {
            Icon(
                imageVector = if (isConfirmed) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isConfirmed) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.error
            )
        },
        trailingContent = {
            if (!isConfirmed) {
                TextButton(onClick = onConfirm) {
                    Text("已完成")
                }
            }
        },
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun MiuiBackgroundPopupItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Column {
                Text(description)
                if (isGranted) {
                    Text(
                        text = "✓ 已检测到权限已开启",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "✗ 检测到权限未开启",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        leadingContent = {
            Icon(
                imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.error
            )
        },
        trailingContent = {
            Row {
                if (!isGranted) {
                    TextButton(onClick = onOpenSettings) {
                        Text("去开启")
                    }
                }
                TextButton(onClick = onRefresh) {
                    Text("刷新")
                }
            }
        },
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}
