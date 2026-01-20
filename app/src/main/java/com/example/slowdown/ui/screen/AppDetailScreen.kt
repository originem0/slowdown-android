package com.sharonZ.slowdown.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sharonZ.slowdown.ui.components.*
import com.sharonZ.slowdown.util.AppInfo
import com.sharonZ.slowdown.viewmodel.AppDetailViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import androidx.compose.ui.res.stringResource
import com.sharonZ.slowdown.R

/**
 * 限制模式枚举
 */
private enum class RestrictionMode {
    NO_INTERVENTION,    // 不干预：仅监控统计
    SOFT_REMINDER,      // 温和提醒：显示深呼吸，可继续使用
    STRICT_LIMIT,       // 严格限制：达到限额后禁止使用
    COMPLETELY_BLOCKED  // 完全禁止：打开即阻止
}

/**
 * 根据当前设置判断限制模式
 */
private fun getCurrentMode(dailyLimit: Int?, limitMode: String, isEnabled: Boolean): RestrictionMode {
    if (!isEnabled) return RestrictionMode.NO_INTERVENTION
    return when {
        dailyLimit == null && limitMode == "strict" -> RestrictionMode.COMPLETELY_BLOCKED
        dailyLimit == null && limitMode == "soft" -> RestrictionMode.SOFT_REMINDER
        dailyLimit != null && limitMode == "strict" -> RestrictionMode.STRICT_LIMIT
        dailyLimit != null && limitMode == "soft" -> RestrictionMode.SOFT_REMINDER
        else -> RestrictionMode.SOFT_REMINDER
    }
}

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
    val globalCooldownMinutes by viewModel.globalCooldownMinutes.collectAsState()

    val app = monitoredApp

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = app?.appName ?: stringResource(R.string.app_settings),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (app == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(strokeWidth = 3.dp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // 应用头部
                item {
                    AppHeader(appInfo = appInfo, appName = app.appName)
                }

                // 使用统计
                item {
                    SectionTitle(title = stringResource(R.string.today_usage), paddingTop = 8.dp)
                }

                item {
                    UsageStatsSection(
                        todayUsage = todayUsage,
                        weeklyAverage = if (recentUsage.isNotEmpty()) {
                            recentUsage.sumOf { it.usageMinutes } / recentUsage.size
                        } else 0
                    )
                }

                // 限制模式（统一设置区域）
                item {
                    SectionTitle(title = stringResource(R.string.restriction_mode), paddingTop = 8.dp)
                }

                item {
                    RestrictionModeSection(
                        currentLimit = app.dailyLimitMinutes,
                        currentMode = app.limitMode,
                        isEnabled = app.isEnabled,
                        onModeChange = { mode, limit ->
                            // 使用原子更新，一次性设置所有字段，避免状态竞争
                            when (mode) {
                                RestrictionMode.NO_INTERVENTION -> {
                                    // 仅统计：禁用限制，重置为 soft 模式和无时间限制
                                    viewModel.updateRestrictionMode(
                                        isEnabled = false,
                                        limitMode = "soft",
                                        dailyLimitMinutes = null
                                    )
                                }
                                RestrictionMode.SOFT_REMINDER -> {
                                    // 温和提醒：启用，soft 模式，保留传入的时间限制
                                    viewModel.updateRestrictionMode(
                                        isEnabled = true,
                                        limitMode = "soft",
                                        dailyLimitMinutes = limit
                                    )
                                }
                                RestrictionMode.STRICT_LIMIT -> {
                                    // 严格限制：启用，strict 模式，必须有时间限制
                                    viewModel.updateRestrictionMode(
                                        isEnabled = true,
                                        limitMode = "strict",
                                        dailyLimitMinutes = limit
                                    )
                                }
                                RestrictionMode.COMPLETELY_BLOCKED -> {
                                    // 完全禁止：启用，strict 模式，无时间限制
                                    viewModel.updateRestrictionMode(
                                        isEnabled = true,
                                        limitMode = "strict",
                                        dailyLimitMinutes = null
                                    )
                                }
                            }
                        }
                    )
                }

                // 其他设置
                item {
                    SectionTitle(title = stringResource(R.string.other_settings), paddingTop = 8.dp)
                }

                // 视频应用模式
                item {
                    VideoAppModeSection(
                        isVideoApp = app.isVideoApp,
                        onVideoAppModeChange = { viewModel.updateVideoAppMode(it) }
                    )
                }

                // 冷却时间设置
                item {
                    CooldownSection(
                        currentCooldown = app.cooldownMinutes,
                        globalCooldown = globalCooldownMinutes,
                        onCooldownChange = { viewModel.updateCooldownMinutes(it) }
                    )
                }

                item {
                    RedirectSection(
                        currentRedirect = app.redirectPackage,
                        installedApps = installedApps,
                        currentPackage = app.packageName,
                        onRedirectSelected = { viewModel.updateRedirectApp(it) }
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun AppHeader(appInfo: AppInfo?, appName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (appInfo?.icon != null) {
            Image(
                painter = rememberDrawablePainter(drawable = appInfo.icon),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appName.take(1),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = appName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
            if (appInfo != null) {
                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UsageStatsSection(
    todayUsage: Int,
    weeklyAverage: Int
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
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatValue(
                value = stringResource(R.string.minutes_format, todayUsage),
                label = stringResource(R.string.today_usage),
                valueColor = MaterialTheme.colorScheme.primary
            )
            StatValue(
                value = "$weeklyAverage ${stringResource(R.string.minutes_per_day)}",
                label = stringResource(R.string.weekly_average),
                valueColor = MaterialTheme.colorScheme.secondary
            )
        }
        ListDivider(startIndent = 0.dp)
    }
}

/**
 * 统一的限制模式设置区域
 * 将模式选择和时间限制整合为四个清晰的选项
 */
@Composable
private fun RestrictionModeSection(
    currentLimit: Int?,
    currentMode: String,
    isEnabled: Boolean,
    onModeChange: (RestrictionMode, Int?) -> Unit
) {
    val currentRestrictionMode = getCurrentMode(currentLimit, currentMode, isEnabled)
    var showTimePicker by remember { mutableStateOf(false) }
    var editingMode by remember { mutableStateOf<RestrictionMode?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 选项1：仅统计
        RestrictionModeItem(
            title = stringResource(R.string.tracking_only_title),
            description = stringResource(R.string.tracking_only_desc),
            selected = currentRestrictionMode == RestrictionMode.NO_INTERVENTION,
            onClick = { onModeChange(RestrictionMode.NO_INTERVENTION, null) }
        )
        ListDivider(startIndent = 52.dp)

        // 选项2：温和提醒
        RestrictionModeItem(
            title = stringResource(R.string.gentle_reminder_title),
            description = when {
                currentRestrictionMode == RestrictionMode.SOFT_REMINDER && currentLimit != null ->
                    stringResource(R.string.gentle_reminder_with_limit, currentLimit)
                currentRestrictionMode == RestrictionMode.SOFT_REMINDER ->
                    stringResource(R.string.gentle_reminder_every_open)
                else -> stringResource(R.string.gentle_reminder_desc)
            },
            selected = currentRestrictionMode == RestrictionMode.SOFT_REMINDER,
            showTimeConfig = currentRestrictionMode == RestrictionMode.SOFT_REMINDER,
            currentLimit = if (currentRestrictionMode == RestrictionMode.SOFT_REMINDER) currentLimit else null,
            onClick = {
                // 直接切换到温和提醒模式（保留当前时间限制）
                onModeChange(RestrictionMode.SOFT_REMINDER, currentLimit)
            },
            onTimeClick = {
                editingMode = RestrictionMode.SOFT_REMINDER
                showTimePicker = true
            }
        )
        ListDivider(startIndent = 52.dp)

        // 选项3：严格限制
        RestrictionModeItem(
            title = stringResource(R.string.strict_limit_title),
            description = when {
                currentRestrictionMode == RestrictionMode.STRICT_LIMIT && currentLimit != null ->
                    stringResource(R.string.strict_limit_with_limit, currentLimit)
                else -> stringResource(R.string.strict_limit_desc)
            },
            selected = currentRestrictionMode == RestrictionMode.STRICT_LIMIT,
            showTimeConfig = currentRestrictionMode == RestrictionMode.STRICT_LIMIT,
            currentLimit = if (currentRestrictionMode == RestrictionMode.STRICT_LIMIT) currentLimit else null,
            onClick = {
                // 严格限制必须有时间限额，如果没有则弹出选择器
                if (currentLimit != null) {
                    onModeChange(RestrictionMode.STRICT_LIMIT, currentLimit)
                } else {
                    editingMode = RestrictionMode.STRICT_LIMIT
                    showTimePicker = true
                }
            },
            onTimeClick = {
                editingMode = RestrictionMode.STRICT_LIMIT
                showTimePicker = true
            },
            accentColor = MaterialTheme.colorScheme.tertiary
        )
        ListDivider(startIndent = 52.dp)

        // 选项4：完全禁止
        RestrictionModeItem(
            title = stringResource(R.string.completely_blocked_title),
            description = stringResource(R.string.completely_blocked_desc),
            selected = currentRestrictionMode == RestrictionMode.COMPLETELY_BLOCKED,
            onClick = { onModeChange(RestrictionMode.COMPLETELY_BLOCKED, null) },
            accentColor = MaterialTheme.colorScheme.error
        )
        ListDivider(startIndent = 0.dp)
    }

    // 时间选择对话框
    if (showTimePicker && editingMode != null) {
        TimeLimitPickerDialog(
            currentValue = currentLimit,  // 传入实际值，null 表示无限制
            allowNoLimit = editingMode == RestrictionMode.SOFT_REMINDER,
            onDismiss = {
                showTimePicker = false
                editingMode = null
            },
            onConfirm = { minutes ->
                onModeChange(editingMode!!, minutes)
                showTimePicker = false
                editingMode = null
            }
        )
    }
}

/**
 * 单个限制模式选项
 * 使用 clickable 替代 selectable 以提高响应性
 */
@Composable
private fun RestrictionModeItem(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    showTimeConfig: Boolean = false,
    currentLimit: Int? = null,
    onTimeClick: (() -> Unit)? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),  // 增加垂直 padding 使点击区域更大
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,  // RadioButton 也响应点击
            colors = RadioButtonDefaults.colors(
                selectedColor = accentColor
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 时间配置按钮（仅在选中且需要时显示）
        if (showTimeConfig && onTimeClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onTimeClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (currentLimit != null) stringResource(R.string.minutes_format, currentLimit) else stringResource(R.string.no_limit),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

/**
 * 时间限制选择对话框
 */
@Composable
private fun TimeLimitPickerDialog(
    currentValue: Int?,  // 改为可空，null 表示无限制
    allowNoLimit: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit
) {
    // 根据当前值初始化状态
    // 如果 currentValue 是 null（无限制）且允许无限制，则 selectedPreset 为 null
    // 否则使用 currentValue 或默认值 30
    var selectedPreset by remember { mutableStateOf(currentValue) }
    var showCustomInput by remember { mutableStateOf(false) }
    var customValue by remember { mutableStateOf((currentValue ?: 30).toString()) }

    val presets = listOf(
        15 to "15 分钟",
        30 to "30 分钟",
        60 to "1 小时",
        120 to "2 小时"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_daily_limit)) },
        text = {
            Column {
                if (allowNoLimit) {
                    // 无限制选项
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedPreset == null && !showCustomInput,
                                onClick = {
                                    selectedPreset = null
                                    showCustomInput = false
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPreset == null && !showCustomInput,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.no_limit_remind_every_open))
                    }
                }

                // 预设选项
                presets.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedPreset == value && !showCustomInput,
                                onClick = {
                                    selectedPreset = value
                                    showCustomInput = false
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPreset == value && !showCustomInput,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }

                // 自定义选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = showCustomInput,
                            onClick = { showCustomInput = true },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = showCustomInput,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.custom))
                }

                // 自定义输入框
                if (showCustomInput) {
                    OutlinedTextField(
                        value = customValue,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                customValue = newValue
                            }
                        },
                        label = { Text(stringResource(R.string.enter_minutes)) },
                        suffix = { Text(stringResource(R.string.minutes_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val result = if (showCustomInput) {
                        customValue.toIntOrNull()
                    } else {
                        selectedPreset
                    }
                    onConfirm(result)
                },
                enabled = if (showCustomInput) {
                    customValue.toIntOrNull()?.let { it in 1..1440 } == true
                } else {
                    true
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

// ========== 以下为旧组件，保留以备参考 ==========

/**
 * [已弃用] 每日限额设置
 * 包含"完全禁止"快捷选项（无限制+强制关闭模式）
 */
@Composable
private fun DailyLimitSection(
    currentLimit: Int?,
    currentMode: String,
    onLimitChange: (Int?) -> Unit,
    onCompletelyBlocked: () -> Unit
) {
    var showCustomDialog by remember { mutableStateOf(false) }

    // 检查是否为完全禁止模式
    val isCompletelyBlocked = currentLimit == null && currentMode == "strict"

    // 预设选项
    val presetOptions = listOf(
        null to "无限制",
        15 to "15 分钟",
        30 to "30 分钟",
        60 to "1 小时",
        120 to "2 小时"
    )

    // 检查当前值是否在预设中（排除完全禁止的情况）
    val isCustomValue = !isCompletelyBlocked && currentLimit != null && presetOptions.none { it.first == currentLimit }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .selectableGroup()
    ) {
        // 完全禁止选项（置顶，带特殊样式）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = isCompletelyBlocked,
                    onClick = { onCompletelyBlocked() },
                    role = Role.RadioButton
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isCompletelyBlocked,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.error
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.completely_blocked_simple),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isCompletelyBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isCompletelyBlocked) FontWeight.Medium else FontWeight.Normal
                )
                Text(
                    text = stringResource(R.string.blocked_on_open),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        ListDivider(startIndent = 52.dp)

        // 预设选项
        presetOptions.forEach { (value, label) ->
            // 无限制选项：只有当 limitMode 不是 strict 时才选中
            val selected = if (value == null) {
                currentLimit == null && currentMode != "strict"
            } else {
                currentLimit == value
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected,
                        onClick = { onLimitChange(value) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected,
                    onClick = null
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
            }
            ListDivider(startIndent = 52.dp)
        }

        // 自定义选项
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = isCustomValue,
                    onClick = { showCustomDialog = true },
                    role = Role.RadioButton
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isCustomValue,
                onClick = null
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.custom),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (isCustomValue && currentLimit != null) {
                Text(
                    text = stringResource(R.string.minutes_format, currentLimit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ListDivider(startIndent = 0.dp)
    }

    // 自定义输入对话框
    if (showCustomDialog) {
        CustomLimitDialog(
            currentValue = currentLimit ?: 30,
            onDismiss = { showCustomDialog = false },
            onConfirm = { minutes ->
                onLimitChange(minutes)
                showCustomDialog = false
            }
        )
    }
}

@Composable
private fun CustomLimitDialog(
    currentValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var inputValue by remember { mutableStateOf(currentValue.toString()) }
    val isValid = inputValue.toIntOrNull()?.let { it in 1..1440 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_daily_limit)) },
        text = {
            Column {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            inputValue = newValue
                        }
                    },
                    label = { Text(stringResource(R.string.enter_minutes)) },
                    suffix = { Text(stringResource(R.string.minute_unit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = inputValue.isNotEmpty() && !isValid,
                    modifier = Modifier.fillMaxWidth()
                )
                if (inputValue.isNotEmpty() && !isValid) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.input_range_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { inputValue.toIntOrNull()?.let { onConfirm(it) } },
                enabled = isValid
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * 限制模式选择
 * 完全禁止模式时此区域禁用
 */
@Composable
private fun LimitModeSection(
    currentMode: String,
    isCompletelyBlocked: Boolean,
    onModeChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .selectableGroup()
    ) {
        // 完全禁止时显示提示
        if (isCompletelyBlocked) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.already_blocked_mode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ListDivider(startIndent = 0.dp)
            return@Column
        }

        // 软提醒
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = currentMode == "soft",
                    onClick = { onModeChange("soft") },
                    role = Role.RadioButton
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = currentMode == "soft",
                onClick = null
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.soft_reminder),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.soft_reminder_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ListDivider(startIndent = 52.dp)

        // 强制关闭
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = currentMode == "strict",
                    onClick = { onModeChange("strict") },
                    role = Role.RadioButton
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = currentMode == "strict",
                onClick = null
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.force_close),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.force_close_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ListDivider(startIndent = 0.dp)
    }
}

/**
 * 视频应用模式开关
 * 开启后使用定时器主动触发弹窗检查，适用于抖音、B站等短视频应用
 */
@Composable
private fun VideoAppModeSection(
    isVideoApp: Boolean,
    onVideoAppModeChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onVideoAppModeChange(!isVideoApp) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.video_mode),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (isVideoApp) stringResource(R.string.video_mode_enabled) else stringResource(R.string.video_mode_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isVideoApp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isVideoApp,
                onCheckedChange = onVideoAppModeChange
            )
        }
        ListDivider(startIndent = 0.dp)
    }
}

/**
 * 冷却时间设置
 * 可选择使用全局设置或为当前应用设置单独的冷却时间
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CooldownSection(
    currentCooldown: Int?,
    globalCooldown: Int,
    onCooldownChange: (Int?) -> Unit
) {
    // 预设的冷却时间选项（1, 3, 5, 10, 15, 20, 25, 30分钟）
    val cooldownOptions = listOf(1, 3, 5, 10, 15, 20, 25, 30)

    // 是否使用全局设置
    val useGlobal = currentCooldown == null

    // 当前选中的值（用于下拉框显示）
    val selectedValue = currentCooldown ?: globalCooldown

    // 下拉框展开状态
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 标题行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cooldown_time),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.cooldown_time_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 使用全局设置复选框
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (!useGlobal) {
                        onCooldownChange(null)
                    }
                }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = useGlobal,
                onCheckedChange = { checked ->
                    if (checked) {
                        onCooldownChange(null)
                    } else {
                        // 切换到自定义时，使用最接近当前值的预设选项
                        val defaultValue = cooldownOptions.minByOrNull { kotlin.math.abs(it - selectedValue) } ?: 5
                        onCooldownChange(defaultValue)
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.use_global_cooldown, globalCooldown),
                style = MaterialTheme.typography.bodyMedium,
                color = if (useGlobal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }

        // 自定义下拉框
        if (!useGlobal) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
            ) {
                OutlinedTextField(
                    value = stringResource(R.string.minutes_format, selectedValue),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.custom_cooldown)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    cooldownOptions.forEach { minutes ->
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.minutes_format, minutes)) },
                            onClick = {
                                onCooldownChange(minutes)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        } else {
            // 使用全局设置时显示占位符
            Spacer(modifier = Modifier.height(8.dp))
        }

        ListDivider(startIndent = 0.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RedirectSection(
    currentRedirect: String?,
    installedApps: List<AppInfo>,
    currentPackage: String,
    onRedirectSelected: (String?) -> Unit
) {
    var showAppPicker by remember { mutableStateOf(false) }
    val selectedApp = installedApps.find { it.packageName == currentRedirect }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAppPicker = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.redirect_app),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.redirect_app_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 当前选择
            Text(
                text = selectedApp?.appName ?: stringResource(R.string.none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ListDivider(startIndent = 0.dp)
    }

    // 应用选择对话框
    if (showAppPicker) {
        AppPickerDialog(
            installedApps = installedApps,
            currentPackage = currentPackage,
            currentSelection = currentRedirect,
            onDismiss = { showAppPicker = false },
            onAppSelected = { packageName ->
                onRedirectSelected(packageName)
                showAppPicker = false
            }
        )
    }
}

/**
 * 应用选择对话框
 * 支持搜索过滤，显示所有可用应用
 */
@Composable
private fun AppPickerDialog(
    installedApps: List<AppInfo>,
    currentPackage: String,
    currentSelection: String?,
    onDismiss: () -> Unit,
    onAppSelected: (String?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // 过滤应用列表
    val filteredApps = remember(searchQuery, installedApps) {
        val query = searchQuery.trim().lowercase()
        installedApps
            .filter { it.packageName != currentPackage }
            .filter { app ->
                if (query.isEmpty()) true
                else app.appName.lowercase().contains(query) ||
                     app.packageName.lowercase().contains(query)
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_redirect_app)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                // 搜索框
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_apps_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // "无" 选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAppSelected(null) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentSelection == null,
                        onClick = { onAppSelected(null) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.no_redirect))
                }

                ListDivider(startIndent = 0.dp)

                // 应用列表（使用 LazyColumn 支持滚动）
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredApps.size) { index ->
                        val app = filteredApps[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppSelected(app.packageName) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSelection == app.packageName,
                                onClick = { onAppSelected(app.packageName) }
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            if (app.icon != null) {
                                Image(
                                    painter = rememberDrawablePainter(drawable = app.icon),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            Column {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    if (filteredApps.isEmpty() && searchQuery.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.no_apps_found),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
