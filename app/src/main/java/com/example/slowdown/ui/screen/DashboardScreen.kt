package com.example.slowdown.ui.screen

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
import com.example.slowdown.data.local.dao.AppStat
import com.example.slowdown.data.local.dao.SuccessRateStat
import com.example.slowdown.ui.components.*
import com.example.slowdown.viewmodel.AwarenessMoment
import com.example.slowdown.viewmodel.DashboardViewModel
import com.example.slowdown.viewmodel.MindfulState
import com.example.slowdown.viewmodel.PermissionState
import androidx.compose.ui.res.stringResource
import com.example.slowdown.R

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
                    mindfulState = mindfulState
                )
            }

            // Today's Awareness Moments Card
            if (awarenessMoments.isNotEmpty()) {
                item {
                    AwarenessMomentsCard(
                        moments = awarenessMoments,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Top Intercepted Apps
            if (topApps.isNotEmpty()) {
                item {
                    SectionTitle(title = stringResource(R.string.most_intercepted), paddingTop = 16.dp)
                }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = UserBorder
                    ) {
                        Column {
                            topApps.take(5).forEachIndexed { index, app ->
                                TopAppItem(
                                    app = app,
                                    maxCount = topApps.first().count,
                                    onClick = onNavigateToAppList,
                                    showDivider = index < 4
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onNavigateToAppList)
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.view_all_apps),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

val UserBorder @Composable get() = androidx.compose.foundation.BorderStroke(
    1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
)


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
private fun StatsOverview(count: Int, successRate: SuccessRateStat, mindfulState: MindfulState) {
    // 计算成功率百分比
    val successPercent = if (successRate.total > 0) {
        (successRate.successful * 100 / successRate.total)
    } else {
        0
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp)) // Minimal top spacer

        // Main Breathing Circle with State Awareness
        MindfulBreathingCircle(mindfulState = mindfulState)

        Spacer(modifier = Modifier.height(16.dp)) // Reduced spacer

        // Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = count.toString(),
                label = stringResource(R.string.intercepts),
                icon = null
            )

            Box(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
                    .align(Alignment.CenterVertically)
            )

            StatItem(
                value = "$successPercent%",
                label = stringResource(R.string.success_rate),
                icon = null
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun StatItem(value: String, label: String, icon: Any?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            val baseRadius = size.minDimension / 2.8f

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
 * 今日觉知时刻卡片 - 简约融入背景
 */
@Composable
private fun AwarenessMomentsCard(
    moments: List<AwarenessMoment>,
    modifier: Modifier = Modifier
) {
    // Subtle colors that blend with background
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val timeColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        // Moments list - no title, just content
        moments.forEachIndexed { index, moment ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leaf icon
                Text(
                    text = "🌿",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )

                // Time badge - 直接显示时间戳
                Text(
                    text = moment.timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = timeColor,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Message - 使用资源 ID 和参数动态获取字符串
                Text(
                    text = stringResource(moment.messageResId, *moment.messageArgs),
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                    lineHeight = 18.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            // Subtle divider (except for last item)
            if (index < moments.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 40.dp, end = 16.dp),
                    thickness = 0.5.dp,
                    color = dividerColor
                )
            }
        }
    }
}

// Keep the old BreathingCircle for backward compatibility (unused)
@Composable
private fun BreathingCircle(count: Int) {
    MindfulBreathingCircle(mindfulState = MindfulState.CALM)
}


@Composable
private fun TopAppItem(
    app: AppStat,
    maxCount: Int,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App initial circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
             Text(
                text = app.appName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Custom Progress Bar
            val progress = if (maxCount > 0) app.count.toFloat() / maxCount else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.secondary)
                )
            }
        }

        Text(
            text = "${app.count}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 72.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

val EaseInOutQuad: Easing = Easing { fraction ->
    if (fraction < 0.5f) 2 * fraction * fraction else 1 - (-2 * fraction + 2) * (-2 * fraction + 2) / 2
}

val EaseOutQuad: Easing = Easing { fraction ->
    1 - (1 - fraction) * (1 - fraction)
}
