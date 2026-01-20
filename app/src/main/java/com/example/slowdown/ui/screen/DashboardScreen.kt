package com.sharonZ.slowdown.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.sharonZ.slowdown.data.local.dao.AppStat
import com.sharonZ.slowdown.data.local.dao.SuccessRateStat
import com.sharonZ.slowdown.ui.components.*
import com.sharonZ.slowdown.viewmodel.AwarenessMoment
import com.sharonZ.slowdown.viewmodel.DashboardViewModel
import com.sharonZ.slowdown.viewmodel.MindfulState
import com.sharonZ.slowdown.viewmodel.PermissionState
import com.sharonZ.slowdown.viewmodel.PeriodStat
import com.sharonZ.slowdown.viewmodel.TimePeriod
import androidx.compose.ui.res.stringResource
import com.sharonZ.slowdown.R

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAppList: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val todayCount by viewModel.todayCount.collectAsState()
    val successRate by viewModel.todaySuccessRate.collectAsState()
    val topApps by viewModel.topApps.collectAsState()
    val serviceEnabled by viewModel.serviceEnabled.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()
    val mindfulState by viewModel.mindfulState.collectAsState()
    val awarenessMoments by viewModel.awarenessMoments.collectAsState()
    val allTodayMoments by viewModel.allTodayMoments.collectAsState()
    val averageDecisionTime by viewModel.averageDecisionTime.collectAsState()
    val periodDistribution by viewModel.periodDistribution.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    // 权限不足提示对话框状态
    var showPermissionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshPermissions()
        }
    }

    // 权限不足提示对话框
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(stringResource(R.string.permissions_needed)) },
            text = {
                Column {
                    Text(stringResource(R.string.please_enable_permissions))
                    Spacer(modifier = Modifier.height(8.dp))
                    permissionState.missingRequiredPermissions.forEach { permission ->
                        Text(
                            text = "• $permission",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    onNavigateToSettings()
                }) {
                    Text(stringResource(R.string.go_enable))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
            // Header Section - Simplified
            item {
                StatusHeader(
                    serviceEnabled = serviceEnabled,
                    onToggleService = { enabled ->
                        if (enabled && !permissionState.allRequiredPermissionsGranted) {
                            // 尝试开启但权限不足，显示提示
                            showPermissionDialog = true
                        } else {
                            viewModel.setServiceEnabled(enabled)
                        }
                    }
                )
            }

            // Permissions - 仅在必要权限未授予时显示提示
            if (!permissionState.allRequiredPermissionsGranted) {
                item {
                    AlertBanner(
                        message = stringResource(R.string.setup_required),
                        icon = Icons.Default.Warning,
                        isWarning = true,
                        onClick = onNavigateToSettings,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Stats Cards with State-aware Breathing Circle
            item {
                StatsOverview(
                    count = todayCount,
                    successRate = successRate,
                    mindfulState = mindfulState,
                    averageDecisionTime = averageDecisionTime
                )
            }

            // Today's Awareness Moments Section - 无标题，轻量化展示
            if (awarenessMoments.isNotEmpty()) {
                item {
                    AwarenessMomentsSection(
                        moments = awarenessMoments,
                        allMoments = allTodayMoments
                    )
                }
            }

            // Period Distribution Section - 已在卡片内含标题，无需外部标题
            if (periodDistribution.isNotEmpty() && periodDistribution.any { it.count > 0 }) {
                item {
                    PeriodDistributionSection(periodStats = periodDistribution)
                }
            }

            // Top Intercepted Apps Section - 卡片风格
            if (topApps.isNotEmpty()) {
                item {
                    TopAppsSection(
                        topApps = topApps.take(5),
                        onNavigateToAppList = onNavigateToAppList
                    )
                }
            }
        }
    }

@Composable
private fun StatusHeader(
    serviceEnabled: Boolean,
    onToggleService: (Boolean) -> Unit
) {
    // Skeuomorphic "Depth" style: No card, floating elements with atmospheric glow
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp), // Standardized top spacing
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.CenterStart) {
            // 1. Atmospheric Glow behind text (only when active)
            if (serviceEnabled) {
                val primaryColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.size(180.dp, 60.dp).offset(x = (-20).dp)) {
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            radius = size.width * 0.7f
                        )
                    )
                }
            }

            // 2. Text with subtle depth (Shadow)
            Column {
                Text(
                    text = if (serviceEnabled) stringResource(R.string.protection_active) else stringResource(R.string.protection_paused),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                            blurRadius = 4f
                        )
                    ),
                    color = if (serviceEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Bold
                )
                
                // Subtitle/Status Detail
                Text(
                    text = if (serviceEnabled) "System Guarding" else "Tap to Resume", // Simple engligh fallback or localized if available. User didn't ask for text change but style. keeping it simple.
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        
        // Switch with depth (Scale + Shadow)
        Box(
            modifier = Modifier
                .scale(1.1f)
                .shadow(
                    elevation = if (serviceEnabled) 8.dp else 2.dp,
                    shape = CircleShape,
                    spotColor = if (serviceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
        ) {
            Switch(
                checked = serviceEnabled,
                onCheckedChange = onToggleService,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
private fun StatsOverview(
    count: Int,
    successRate: SuccessRateStat,
    mindfulState: MindfulState,
    averageDecisionTime: Float?
) {
    // 计算成功率百分比
    val successPercent = if (successRate.total > 0) {
        (successRate.successful * 100 / successRate.total)
    } else {
        0
    }

    // 格式化决策时间
    val decisionTimeText = if (averageDecisionTime != null) {
        String.format("%.1f", averageDecisionTime) + stringResource(R.string.seconds_short)
    } else {
        "--"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp)) // Minimal top spacer

        // Main Breathing Circle with State Awareness
        MindfulBreathingCircle(mindfulState = mindfulState)

        Spacer(modifier = Modifier.height(16.dp)) // Reduced spacer

        // Stats Row - 三个指标
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = count.toString(),
                label = stringResource(R.string.intercepts),
                icon = null,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    .align(Alignment.CenterVertically)
            )

            StatItem(
                value = "$successPercent%",
                label = stringResource(R.string.success_rate),
                icon = null,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    .align(Alignment.CenterVertically)
            )

            StatItem(
                value = decisionTimeText,
                label = stringResource(R.string.decision_time),
                icon = null,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: Any?,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

// State-specific colors
private val CalmColor = Color(0xFF2A9D8F)       // Teal/Blue
private val AlertColor = Color(0xFFF4A261)       // Warm Orange
private val ExceededColor = Color(0xFF6B5B95)    // Dark Purple

/**
 * 正念呼吸圆圈 - 根据用户状态动态变化
 * 平静：青蓝色，3秒呼吸，正圆
 * 警觉：暖橙色，1秒快呼吸，微椭圆
 * 超限：暗紫色，5秒慢呼吸，略收缩
 */
@Composable
private fun MindfulBreathingCircle(mindfulState: MindfulState) {
    // State-based parameters
    val targetColor = when (mindfulState) {
        MindfulState.CALM -> CalmColor
        MindfulState.ALERT -> AlertColor
        MindfulState.EXCEEDED -> ExceededColor
    }

    val breathDuration = when (mindfulState) {
        MindfulState.CALM -> 3000      // 3 seconds
        MindfulState.ALERT -> 1000     // 1 second
        MindfulState.EXCEEDED -> 5000  // 5 seconds
    }

    // Animated color transition
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 1000),
        label = "circleColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "breathing")

    // Main scale animation (breathing)
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = when (mindfulState) {
            MindfulState.CALM -> 1.08f      // Normal breath
            MindfulState.ALERT -> 1.12f     // Slightly larger breath
            MindfulState.EXCEEDED -> 0.92f  // Contracted, restrained
        },
        animationSpec = infiniteRepeatable(
            animation = tween(breathDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mainScale"
    )

    // Horizontal stretch for ellipse effect (Alert state)
    val horizontalStretch by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = when (mindfulState) {
            MindfulState.CALM -> 1.0f       // Perfect circle
            MindfulState.ALERT -> 1.08f     // Slight ellipse
            MindfulState.EXCEEDED -> 1.0f   // Back to circle
        },
        animationSpec = infiniteRepeatable(
            animation = tween(breathDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hStretch"
    )

    // Glow alpha pulsing
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(breathDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Inner glow pulse
    val innerGlow by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween((breathDuration * 0.75f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "innerGlow"
    )

    val surfaceColor = MaterialTheme.colorScheme.surface

    // Larger size, no boundary feel
    Box(
        modifier = Modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(scaleX = horizontalStretch, scaleY = 1f)
        ) {
            val center = this.center
            val baseRadius = size.minDimension / 2.3f  // 扩大内部圈

            // 1. Outermost diffuse haze - creates boundaryless effect
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedColor.copy(alpha = 0.08f * glowAlpha),
                        animatedColor.copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * scale * 2.2f
                ),
                radius = baseRadius * scale * 2.2f,
                center = center
            )

            // 2. Secondary outer haze
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedColor.copy(alpha = 0.12f * glowAlpha),
                        animatedColor.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * scale * 1.7f
                ),
                radius = baseRadius * scale * 1.7f,
                center = center
            )

            // 3. Subtle texture ring (very faint)
            drawCircle(
                brush = Brush.radialGradient(
                    0.6f to Color.Transparent,
                    0.8f to animatedColor.copy(alpha = 0.06f * innerGlow),
                    1.0f to Color.Transparent,
                    center = center,
                    radius = baseRadius * scale * 1.4f
                ),
                radius = baseRadius * scale * 1.4f,
                center = center
            )

            // 4. Main circle body - soft gradient, no hard edge
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedColor.copy(alpha = 0.55f),
                        animatedColor.copy(alpha = 0.35f),
                        animatedColor.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * scale * 1.35f
                ),
                radius = baseRadius * scale * 1.35f,
                center = center
            )

            // 5. Inner core glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        surfaceColor.copy(alpha = 0.1f),
                        animatedColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 0.6f
                ),
                radius = baseRadius * 0.6f,
                center = center
            )
        }
    }
}

/**
 * 今日觉知时刻列表 - 轻量化卡片风格，支持展开/收起
 */
@Composable
private fun AwarenessMomentsSection(
    moments: List<AwarenessMoment>,
    allMoments: List<AwarenessMoment>
) {
    // 展开状态
    var isExpanded by remember { mutableStateOf(false) }
    val displayMoments = if (isExpanded) allMoments else moments
    val hasMoreMoments = allMoments.size > 3

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            displayMoments.forEach { moment ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Leaf icon - 小一点
                    Text(
                        text = "🌿",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 10.dp)
                    )

                    // Time - 小字体，淡色
                    Text(
                        text = moment.timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(42.dp)
                    )

                    // Message - 小字体，淡色
                    Text(
                        text = stringResource(moment.messageResId, *moment.messageArgs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Expand/Collapse button
            if (hasMoreMoments) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(top = 8.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(
                            if (isExpanded) R.string.collapse_moments else R.string.view_all_moments
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 时段分布 - 轻量化卡片风格，显示4个时段的拦截次数分布
 */
@Composable
private fun PeriodDistributionSection(periodStats: List<PeriodStat>) {
    val maxCount = periodStats.maxOfOrNull { it.count } ?: 0
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            // 小标题
            Text(
                text = stringResource(R.string.period_distribution_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            periodStats.forEach { stat ->
                val progress = if (maxCount > 0) stat.count.toFloat() / maxCount else 0f
                val isMax = stat.count == maxCount && maxCount > 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Period label - 小字体
                    Text(
                        text = stringResource(stat.period.labelResId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.width(48.dp)
                    )

                    // Progress bar - 更细更圆润
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(primaryColor.copy(alpha = 0.7f))
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Count - 小字体
                    Text(
                        text = stringResource(R.string.period_count, stat.count),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.width(42.dp)
                    )

                    // Fire icon for max
                    if (isMax) {
                        Text(
                            text = "🔥",
                            fontSize = 12.sp
                        )
                    } else {
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            }
        }
    }
}

// Keep the old BreathingCircle for backward compatibility (unused)
@Composable
private fun BreathingCircle(count: Int) {
    MindfulBreathingCircle(mindfulState = MindfulState.CALM)
}

/**
 * 最常拦截应用 - 卡片风格
 */
@Composable
private fun TopAppsSection(
    topApps: List<AppStat>,
    onNavigateToAppList: () -> Unit
) {
    val maxCount = topApps.firstOrNull()?.count ?: 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            // 小标题
            Text(
                text = stringResource(R.string.most_intercepted),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            topApps.forEach { app ->
                TopAppItem(
                    app = app,
                    maxCount = maxCount,
                    onClick = onNavigateToAppList
                )
            }

            // 查看全部按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToAppList)
                    .padding(top = 8.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.view_all_apps),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


@Composable
private fun TopAppItem(
    app: AppStat,
    maxCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App initial circle - 小一点
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.appName.take(1).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Progress Bar - 更细
            val progress = if (maxCount > 0) app.count.toFloat() / maxCount else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f))
                )
            }
        }

        Text(
            text = "${app.count}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

val EaseInOutQuad: Easing = Easing { fraction ->
    if (fraction < 0.5f) 2 * fraction * fraction else 1 - (-2 * fraction + 2) * (-2 * fraction + 2) / 2
}

val EaseOutQuad: Easing = Easing { fraction ->
    1 - (1 - fraction) * (1 - fraction)
}
