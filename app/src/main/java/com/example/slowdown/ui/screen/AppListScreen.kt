package com.example.slowdown.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.slowdown.data.local.entity.MonitoredApp
import com.example.slowdown.ui.components.*
import com.example.slowdown.util.AppInfo
import com.example.slowdown.viewmodel.AppListViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.example.slowdown.R

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

    var searchQuery by remember { mutableStateOf("") }
    var appToRemove by remember { mutableStateOf<AppInfo?>(null) }

    val monitoredPackages = monitoredApps.map { it.packageName }.toSet()
    val monitoredAppMap = monitoredApps.associateBy { it.packageName }

    val filteredApps = if (searchQuery.isBlank()) {
        installedApps
    } else {
        val query = searchQuery.trim().lowercase(Locale.ROOT).toHalfWidth()
        installedApps.filter { app ->
            val appNameLower = app.appName.lowercase(Locale.ROOT).toHalfWidth()
            val packageNameLower = app.packageName.lowercase(Locale.ROOT)
            appNameLower.contains(query) || packageNameLower.contains(query)
        }
    }

    val monitoredList = filteredApps.filter { monitoredPackages.contains(it.packageName) }
    val unmonitoredList = filteredApps.filter { !monitoredPackages.contains(it.packageName) }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
                // Removed Header with Back Button
                
                // Search Section
                item {
                    SearchSection(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                }

                // Monitored Apps
                if (monitoredList.isNotEmpty()) {
                    item {
                        SectionTitle(
                            title = stringResource(R.string.protected_apps_count, monitoredList.size),
                            paddingTop = 8.dp // Reduced from 16.dp
                        )
                    }

                    items(monitoredList, key = { it.packageName }) { app ->
                        MonitoredAppItem(
                            app = app,
                            monitoredApp = monitoredAppMap[app.packageName],
                            todayUsage = usageMap[app.packageName] ?: 0,
                            onClick = { onNavigateToAppDetail(app.packageName) },
                            onRemove = { appToRemove = app }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Unmonitored Apps
                if (unmonitoredList.isNotEmpty()) {
                    item {
                        SectionTitle(
                            title = stringResource(R.string.available_apps_count, unmonitoredList.size),
                            paddingTop = 8.dp // Reduced from 16.dp
                        )
                    }

                    items(unmonitoredList, key = { it.packageName }) { app ->
                        UnmonitoredAppItem(
                            app = app,
                            onAdd = { viewModel.toggleApp(app, false) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

    // Remove Dialog
    appToRemove?.let { app ->
        AlertDialog(
            onDismissRequest = { appToRemove = null },
            title = { Text(stringResource(R.string.remove_protection)) },
            text = { Text(stringResource(R.string.remove_protection_confirm, app.appName)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleApp(app, true)
                        appToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { appToRemove = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun StatsSummaryCard(
    monitoredCount: Int,
    unmonitoredCount: Int
) {
    // Ultra minimal stats line, using small typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
         Text(
            text = stringResource(R.string.label_protected) + ": $monitoredCount",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = stringResource(R.string.label_available) + ": $unmonitoredCount",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatsCard(
    count: Int,
    label: String,
    color: Color,
    onColor: Color,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = onColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = onColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun MonitoredAppItem(
    app: AppInfo,
    monitoredApp: MonitoredApp?,
    todayUsage: Int,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    // Prominent, spacious row for Protected apps
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp), // Increased padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Larger Icon for emphasis
        AppIcon(app = app, size = 52)

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Larger App Name
            Text(
                text = app.appName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Mode Badge with tap hint
            val modeText = when {
                monitoredApp?.isEnabled != true -> stringResource(R.string.tracking_only)
                // 完全禁止：strict 模式 + 无时间限制
                monitoredApp.limitMode == "strict" && monitoredApp.dailyLimitMinutes == null ->
                    stringResource(R.string.completely_blocked_simple)
                // 严格限制：strict 模式 + 有时间限制
                monitoredApp.limitMode == "strict" -> stringResource(R.string.strict_mode)
                else -> stringResource(R.string.gentle_mode)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = modeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))

        // Remove Button - 使用更明显的颜色
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun UnmonitoredAppItem(
    app: AppInfo,
    onAdd: () -> Unit
) {
    // Compact row for "Available" apps
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAdd)
            .padding(horizontal = 16.dp, vertical = 6.dp), // Reduced padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Smaller icon for available apps
        AppIcon(app = app, size = 32)

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = app.appName,
            style = MaterialTheme.typography.bodyMedium, // Smaller text
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        Icon(
            Icons.Default.Add,
            contentDescription = "Add",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AppIcon(app: AppInfo, size: Int) {
    if (app.icon != null) {
        Image(
            painter = rememberDrawablePainter(drawable = app.icon),
            contentDescription = null,
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.appName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun String.toHalfWidth(): String {
    val sb = StringBuilder()
    for (char in this) {
        val code = char.code
        if (code == 0x3000) {
            sb.append(' ')
        } else if (code in 0xFF01..0xFF5E) {
            sb.append((code - 0xFEE0).toChar())
        } else {
            sb.append(char)
        }
    }
    return sb.toString()
}
