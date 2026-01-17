package com.example.slowdown.ui.screen

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.Canvas
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.slowdown.data.local.dao.AppStat
import com.example.slowdown.ui.components.*
import com.example.slowdown.viewmodel.DashboardViewModel
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
    val savedMinutes by viewModel.todaySavedMinutes.collectAsState()
    val topApps by viewModel.topApps.collectAsState()
    val serviceEnabled by viewModel.serviceEnabled.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshPermissions()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Header Section - Simplified
            item {
                StatusHeader(
                    serviceEnabled = serviceEnabled,
                    onToggleService = { viewModel.setServiceEnabled(it) }
                )
            }

            // Permissions
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
            } else if (permissionState.miuiPermissionsNeeded) {
                item {
                    AlertBanner(
                        message = stringResource(R.string.miui_permissions_needed),
                        icon = Icons.Default.Warning,
                        isWarning = false,
                        onClick = onNavigateToSettings,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Stats Cards
            item {
                StatsOverview(count = todayCount, savedMinutes = savedMinutes)
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
            .padding(horizontal = 24.dp, vertical = 32.dp), // Generous top spacing (Atmospheric)
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
private fun StatsOverview(count: Int, savedMinutes: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp)) // Minimal top spacer
        
        // Main Breathing Circle
        BreathingCircle(count = count)
        
        Spacer(modifier = Modifier.height(24.dp)) // Reduced spacer
        
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
                value = savedMinutes.toString(),
                label = stringResource(R.string.min_saved),
                icon = null
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
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

@Composable
private fun BreathingCircle(count: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    
    // Slow, meditative organic rhythm (8 seconds cycle)
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f, // Extremely subtle breath
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mainScale"
    )
    
    // Subtle alpha pulsing for texture
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Inner glow pulse
    val innerGlow by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "innerGlow"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface
    
    // Increased size for prominent abstracted visualization
    Box(
        modifier = Modifier.size(320.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val baseRadius = size.minDimension / 4
            
            // 1. Organic Outer Haze (Soft, expanding)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.15f * glowAlpha),
                        primaryColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * scale * 1.8f
                ),
                radius = baseRadius * scale * 1.8f,
                center = center
            )
            
            // 2. Secondary Texture Ring (Subtle ring)
            drawCircle(
                 brush = Brush.radialGradient(
                    0.7f to Color.Transparent,
                    0.85f to primaryColor.copy(alpha = 0.1f * innerGlow),
                    1.0f to Color.Transparent,
                    center = center,
                    radius = baseRadius * scale * 1.4f
                ),
                radius = baseRadius * scale * 1.4f,
                center = center
            )

            // 3. Main Circle Body with "Texture" Gradient
            // Using a complex radial gradient to simulate depth/texture
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        containerColor.copy(alpha = 0.9f),
                        containerColor,
                        primaryColor.copy(alpha = 0.1f)
                    ),
                    center = center,
                    radius = baseRadius * scale
                ),
                radius = baseRadius * scale,
                center = center
            )
            
            // 4. Subtle Rim / Border (Organic, not sharp)
            drawCircle(
                color = primaryColor.copy(alpha = 0.3f),
                radius = baseRadius * scale,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
            
            // 5. Inner Light (Glow from center)
             drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        surfaceColor.copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 0.6f
                ),
                radius = baseRadius * 0.6f,
                center = center
            )
        }
        // Text Content REMOVED as per user request
    }
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
