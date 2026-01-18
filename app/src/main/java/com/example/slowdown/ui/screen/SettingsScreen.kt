package com.example.slowdown.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import android.app.Activity
import com.example.slowdown.R
import com.example.slowdown.ui.components.*
import com.example.slowdown.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val defaultCountdown by viewModel.defaultCountdown.collectAsState()
    val cooldownMinutes by viewModel.cooldownMinutes.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    // 使用 repeatOnLifecycle 在每次 Resume 时刷新权限
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshPermissions()
        }
    }

    // 计算权限状态：只根据必要权限判断
    val allRequiredGranted = permissionState.allRequiredPermissionsGranted

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 权限状态横幅
        item {
            PermissionStatusBanner(
                allGranted = allRequiredGranted,
                onClick = { /* 滚动到权限部分 */ }
            )
        }

        // ========== 必要权限区块 ==========
        item {
            SectionTitle(title = stringResource(R.string.required_permissions), paddingTop = 8.dp)
        }

        item {
            PermissionItem(
                title = stringResource(R.string.accessibility_service),
                subtitle = stringResource(R.string.accessibility_subtitle),
                icon = Icons.Outlined.Security,
                isEnabled = permissionState.accessibilityEnabled,
                onClick = { viewModel.openAccessibilitySettings() }
            )
        }

        item {
            PermissionItem(
                title = stringResource(R.string.overlay_permission),
                subtitle = stringResource(R.string.overlay_subtitle),
                icon = Icons.Outlined.Layers,
                isEnabled = permissionState.overlayEnabled,
                onClick = { viewModel.openOverlaySettings() }
            )
        }

        item {
            PermissionItem(
                title = stringResource(R.string.usage_stats),
                subtitle = stringResource(R.string.usage_subtitle),
                icon = Icons.Outlined.Timer,
                isEnabled = permissionState.usageStatsEnabled,
                onClick = { viewModel.openUsageStatsSettings() }
            )
        }

        // MIUI 后台弹窗权限（必要权限）
        if (permissionState.isMiui) {
            item {
                MiuiBackgroundPopupItem(
                    title = stringResource(R.string.background_popup),
                    subtitle = stringResource(R.string.background_popup_subtitle),
                    isGranted = permissionState.miuiBackgroundPopupGranted,
                    onOpenSettings = { viewModel.openMiuiBackgroundPopupSettings() },
                    onRefresh = { viewModel.refreshPermissions() }
                )
            }

            if (!permissionState.miuiBackgroundPopupGranted) {
                item {
                    MiuiWarningBanner()
                }
            }
        }

        item {
            ListDivider(startIndent = 0.dp)
        }

        // ========== 建议权限区块 ==========
        item {
            SectionTitle(
                title = stringResource(R.string.recommended_permissions),
                subtitle = stringResource(R.string.recommended_permissions_subtitle),
                paddingTop = 8.dp
            )
        }

        item {
            PermissionItem(
                title = stringResource(R.string.battery_optimization),
                subtitle = stringResource(R.string.battery_subtitle),
                icon = Icons.Outlined.BatteryAlert,
                isEnabled = permissionState.batteryOptimizationDisabled,
                onClick = { viewModel.openBatterySettings() }
            )
        }

        // MIUI 建议权限
        if (permissionState.isMiui) {
            item {
                MiuiPermissionItem(
                    title = stringResource(R.string.auto_start),
                    subtitle = stringResource(R.string.auto_start_subtitle),
                    isConfirmed = permissionState.miuiAutoStartConfirmed,
                    onOpenSettings = { viewModel.openMiuiAutoStartSettings() },
                    onConfirm = { viewModel.confirmMiuiAutoStart() },
                    onReset = { viewModel.resetMiuiAutoStart() }
                )
            }

            item {
                MiuiPermissionItem(
                    title = stringResource(R.string.battery_saver),
                    subtitle = stringResource(R.string.battery_saver_subtitle),
                    isConfirmed = permissionState.miuiBatterySaverConfirmed,
                    onOpenSettings = { viewModel.openMiuiBatterySettings() },
                    onConfirm = { viewModel.confirmMiuiBatterySaver() },
                    onReset = { viewModel.resetMiuiBatterySaver() }
                )
            }

            item {
                MiuiManualItem(
                    title = stringResource(R.string.lock_app),
                    subtitle = stringResource(R.string.lock_app_subtitle),
                    isConfirmed = permissionState.miuiLockAppConfirmed,
                    onConfirm = { viewModel.confirmMiuiLockApp() },
                    onReset = { viewModel.resetMiuiLockApp() }
                )
            }
        }

        item {
            ListDivider(startIndent = 0.dp)
        }

        // 干预设置
        item {
            SectionTitle(title = stringResource(R.string.intervention_settings), paddingTop = 8.dp)
        }

        item {
            SliderSettingItem(
                title = stringResource(R.string.default_countdown),
                subtitle = stringResource(R.string.countdown_subtitle),
                value = defaultCountdown,
                valueRange = 3f..30f,
                unit = stringResource(R.string.seconds),
                onValueChange = { viewModel.setDefaultCountdown(it) }
            )
        }

        item {
            SliderSettingItem(
                title = stringResource(R.string.cooldown_time),
                subtitle = stringResource(R.string.cooldown_subtitle),
                value = cooldownMinutes,
                valueRange = 1f..30f,
                unit = stringResource(R.string.minutes),
                onValueChange = { viewModel.setCooldownMinutes(it) }
            )
            ListDivider(startIndent = 0.dp)
        }

        // 语言设置
        item {
            SectionTitle(title = stringResource(R.string.language_section), paddingTop = 8.dp)
        }

        item {
            LanguageSettingItem(
                currentLanguage = currentLanguage,
                onToggle = { viewModel.toggleLanguage() }
            )
            ListDivider(startIndent = 0.dp)
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun PermissionStatusBanner(
    allGranted: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (allGranted)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    else
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)

    val contentColor = if (allGranted)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onErrorContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (allGranted) Icons.Default.Check else Icons.Default.Warning,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (allGranted) stringResource(R.string.permissions_complete) else stringResource(R.string.permissions_needed),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
            Text(
                text = if (allGranted) stringResource(R.string.all_permissions_granted) else stringResource(R.string.please_enable_permissions),
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isEnabled)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        StatusBadge(
            text = if (isEnabled) stringResource(R.string.enabled) else stringResource(R.string.go_enable),
            isPositive = isEnabled
        )
    }

    ListDivider(startIndent = 56.dp)
}

@Composable
private fun MiuiPermissionItem(
    title: String,
    subtitle: String,
    isConfirmed: Boolean,
    onOpenSettings: () -> Unit,
    onConfirm: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onOpenSettings)  // 始终允许点击进入设置
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isConfirmed) Icons.Default.Check else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isConfirmed)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!isConfirmed) {
            TextButton(
                onClick = onConfirm,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(stringResource(R.string.confirm), style = MaterialTheme.typography.labelMedium)
            }
        } else {
            TextButton(
                onClick = onReset,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    stringResource(R.string.confirmed),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    ListDivider(startIndent = 56.dp)
}

@Composable
private fun MiuiBackgroundPopupItem(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit
) {
    val backgroundColor = if (isGranted)
        MaterialTheme.colorScheme.surface
    else
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isGranted) Icons.Default.Check else Icons.Outlined.Close,
            contentDescription = null,
            tint = if (isGranted)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isGranted) stringResource(R.string.permission_detected_on) else stringResource(R.string.permission_detected_off),
                style = MaterialTheme.typography.labelSmall,
                color = if (isGranted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            if (!isGranted) {
                FilledTonalButton(
                    onClick = onOpenSettings,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(stringResource(R.string.go_enable), style = MaterialTheme.typography.labelMedium)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            TextButton(
                onClick = onRefresh,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    stringResource(R.string.refresh),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    ListDivider(startIndent = 56.dp)
}

@Composable
private fun MiuiManualItem(
    title: String,
    subtitle: String,
    isConfirmed: Boolean,
    onConfirm: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isConfirmed) Icons.Default.Check else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isConfirmed)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!isConfirmed) {
            FilledTonalButton(
                onClick = onConfirm,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(stringResource(R.string.confirmed), style = MaterialTheme.typography.labelMedium)
            }
        } else {
            TextButton(
                onClick = onReset,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    stringResource(R.string.confirmed),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun MiuiWarningBanner() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.miui_popup_required_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.miui_popup_steps),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun SliderSettingItem(
    title: String,
    subtitle: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "$value $unit",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = valueRange,
            steps = (valueRange.endInclusive - valueRange.start).toInt() - 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
    }

    ListDivider(startIndent = 16.dp)
}

@Composable
private fun LanguageSettingItem(
    currentLanguage: String,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // 使用 userTriggeredChange 标志来区分用户主动切换和 DataStore 初始加载
    // 只有用户主动点击切换时才 recreate，避免初始加载时的竞态条件
    var userTriggeredChange by remember { mutableStateOf(false) }
    var lastKnownLanguage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentLanguage) {
        if (lastKnownLanguage != null && lastKnownLanguage != currentLanguage && userTriggeredChange) {
            // 用户主动切换语言，延迟后 recreate
            delay(150)  // 稍微增加延迟确保 DataStore 写入完成
            userTriggeredChange = false  // 重置标志
            activity?.recreate()
        }
        lastKnownLanguage = currentLanguage
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = {
                userTriggeredChange = true  // 标记为用户主动切换
                onToggle()
            })
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = if (currentLanguage == "zh")
                    stringResource(R.string.chinese)
                else
                    stringResource(R.string.english),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = if (currentLanguage == "zh")
                    stringResource(R.string.switch_to_english)
                else
                    stringResource(R.string.switch_to_chinese),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = currentLanguage == "zh",
            onCheckedChange = {
                userTriggeredChange = true  // 标记为用户主动切换
                onToggle()
            }
        )
    }
}


