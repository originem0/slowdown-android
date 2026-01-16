package com.example.slowdown.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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

            if (permissionState.isMiui) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "MIUI 专属设置",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                item {
                    ListItem(
                        headlineContent = { Text("自启动权限") },
                        supportingContent = { Text("允许应用开机自启") },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        trailingContent = {
                            TextButton(onClick = { viewModel.openMiuiAutoStartSettings() }) {
                                Text("去开启")
                            }
                        }
                    )
                }

                item {
                    ListItem(
                        headlineContent = { Text("后台弹出界面") },
                        supportingContent = { Text("允许后台弹出干预界面") },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        trailingContent = {
                            TextButton(onClick = { viewModel.openMiuiBackgroundPopupSettings() }) {
                                Text("去开启")
                            }
                        }
                    )
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
