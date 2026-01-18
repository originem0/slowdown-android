package com.example.slowdown.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slowdown.R
import com.example.slowdown.ui.theme.SlowDownDesign as DS
import com.example.slowdown.viewmodel.OnboardingViewModel
import com.example.slowdown.viewmodel.PermissionItem
import com.example.slowdown.viewmodel.PermissionPriority

/**
 * Onboarding Flow for SlowDown
 *
 * A 4-step guided setup experience:
 * 1. Welcome - Value proposition
 * 2. Permissions - Required system access
 * 3. App Selection - Choose apps to monitor
 * 4. Complete - Success and next steps
 */

// ========================================
// Main Onboarding Screen Container
// ========================================
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val permissionItems by viewModel.permissionItems.collectAsState()
    val availableApps by viewModel.availableApps.collectAsState()
    val selectedApps by viewModel.selectedApps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Progress Indicator
            OnboardingProgress(
                currentStep = currentStep,
                totalSteps = 4,
                modifier = Modifier.padding(
                    horizontal = DS.Spacing.screenHorizontal,
                    vertical = DS.Spacing.md
                )
            )

            // Step Content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }
                },
                modifier = Modifier.weight(1f),
                label = "onboarding_step"
            ) { step ->
                when (step) {
                    0 -> WelcomeStep(
                        onNext = { viewModel.nextStep() }
                    )
                    1 -> PermissionsStep(
                        permissionItems = permissionItems,
                        onRequestPermission = { viewModel.requestPermission(it) },
                        onRefresh = { viewModel.refreshPermissions() },
                        onNext = { viewModel.nextStep() },
                        onBack = { viewModel.previousStep() }
                    )
                    2 -> AppSelectionStep(
                        availableApps = availableApps,
                        selectedApps = selectedApps,
                        isLoading = isLoading,
                        onToggleApp = { viewModel.toggleAppSelection(it) },
                        onNext = {
                            viewModel.saveSelectedApps()
                            viewModel.nextStep()
                        },
                        onBack = { viewModel.previousStep() }
                    )
                    3 -> CompleteStep(
                        selectedAppCount = selectedApps.size,
                        onComplete = {
                            viewModel.completeOnboarding()
                            onComplete()
                        }
                    )
                }
            }
        }
    }
}

// ========================================
// Progress Indicator
// ========================================
@Composable
private fun OnboardingProgress(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DS.Spacing.xs)
    ) {
        repeat(totalSteps) { index ->
            val isActive = index <= currentStep
            val progress = if (index < currentStep) 1f
                          else if (index == currentStep) 0.5f
                          else 0f

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(if (isActive) 1f else 0f)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                )
            }
        }
    }
}

// ========================================
// Step 1: Welcome
// ========================================
@Composable
private fun WelcomeStep(
    onNext: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "welcome_breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(DS.Duration.breathingFull, easing = DS.Easing.breathing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcome_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(DS.Spacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(0.15f))

        // Hero Visual - Breathing Circle
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(scale),
            contentAlignment = Alignment.Center
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val containerColor = MaterialTheme.colorScheme.primaryContainer

            Canvas(modifier = Modifier.fillMaxSize()) {
                // Outer glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        radius = size.minDimension / 1.5f
                    )
                )
                // Main circle
                drawCircle(
                    color = containerColor,
                    radius = size.minDimension / 3f
                )
                // Inner highlight
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        radius = size.minDimension / 5f
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(DS.Spacing.xxl))

        // Title
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(DS.Spacing.md))

        // Subtitle
        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = DS.Spacing.lg)
        )

        Spacer(modifier = Modifier.weight(0.2f))

        // Value Props
        Column(
            verticalArrangement = Arrangement.spacedBy(DS.Spacing.md)
        ) {
            ValuePropItem(
                icon = Icons.Outlined.Timer,
                text = stringResource(R.string.onboarding_value_1)
            )
            ValuePropItem(
                icon = Icons.Outlined.Psychology,
                text = stringResource(R.string.onboarding_value_2)
            )
            ValuePropItem(
                icon = Icons.Outlined.TrendingUp,
                text = stringResource(R.string.onboarding_value_3)
            )
        }

        Spacer(modifier = Modifier.weight(0.25f))

        // CTA Button
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(DS.Size.buttonHeightLg),
            shape = DS.Shapes.buttonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = stringResource(R.string.onboarding_get_started),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(DS.Spacing.xxl))
    }
}

@Composable
private fun ValuePropItem(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DS.Spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(DS.Size.avatarSm)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(DS.Size.iconSm)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ========================================
// Step 2: Permissions
// ========================================
@Composable
private fun PermissionsStep(
    permissionItems: List<PermissionItem>,
    onRequestPermission: (PermissionItem) -> Unit,
    onRefresh: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val allGranted = permissionItems.all { it.isGranted }
    // Count how many critical permissions are missing
    val missingCritical = permissionItems.count {
        it.priority == PermissionPriority.CRITICAL && !it.isGranted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(DS.Spacing.screenHorizontal)
    ) {
        Spacer(modifier = Modifier.height(DS.Spacing.lg))

        // Header
        Text(
            text = stringResource(R.string.onboarding_permissions_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(DS.Spacing.xs))

        Text(
            text = stringResource(R.string.onboarding_permissions_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(DS.Spacing.xl))

        // Permission List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(DS.Spacing.sm)
        ) {
            items(permissionItems) { item ->
                PermissionCard(
                    item = item,
                    onClick = {
                        if (!item.isGranted) {
                            onRequestPermission(item)
                        }
                    }
                )
            }

            // Refresh hint
            item {
                TextButton(
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(DS.Size.iconSm)
                    )
                    Spacer(modifier = Modifier.width(DS.Spacing.xs))
                    Text(stringResource(R.string.onboarding_refresh_permissions))
                }
            }
        }

        Spacer(modifier = Modifier.height(DS.Spacing.md))

        // Warning if critical permissions missing
        if (missingCritical > 0 && !allGranted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = DS.Spacing.md),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(DS.Radius.md)
            ) {
                Row(
                    modifier = Modifier.padding(DS.Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(DS.Size.iconSm)
                    )
                    Spacer(modifier = Modifier.width(DS.Spacing.xs))
                    Text(
                        text = stringResource(R.string.onboarding_permissions_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DS.Spacing.md)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(DS.Size.buttonHeight),
                shape = DS.Shapes.buttonShape
            ) {
                Text(stringResource(R.string.back))
            }

            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(2f)
                    .height(DS.Size.buttonHeight),
                shape = DS.Shapes.buttonShape
                // Removed: enabled = allGranted - Now always enabled!
            ) {
                Text(
                    text = stringResource(R.string.onboarding_continue),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Skip option when not all granted
        if (!allGranted) {
            TextButton(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.onboarding_skip_for_now),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(DS.Spacing.md))
    }
}

@Composable
private fun PermissionCard(
    item: PermissionItem,
    onClick: () -> Unit
) {
    val backgroundColor = if (item.isGranted) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (item.isGranted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !item.isGranted, onClick = onClick),
        shape = DS.Shapes.cardShape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = androidx.compose.foundation.BorderStroke(DS.Border.thin, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DS.Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(DS.Size.avatarMd)
                    .clip(CircleShape)
                    .background(
                        if (item.isGranted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isGranted) Icons.Default.Check else item.icon,
                    contentDescription = null,
                    tint = if (item.isGranted) MaterialTheme.colorScheme.onPrimary
                          else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(DS.Size.iconMd)
                )
            }

            Spacer(modifier = Modifier.width(DS.Spacing.md))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action
            if (!item.isGranted) {
                FilledTonalButton(
                    onClick = onClick,
                    shape = DS.Shapes.chipShape,
                    contentPadding = PaddingValues(horizontal = DS.Spacing.md, vertical = DS.Spacing.xs)
                ) {
                    Text(
                        text = stringResource(R.string.go_enable),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

// ========================================
// Step 3: App Selection
// ========================================
@Composable
private fun AppSelectionStep(
    availableApps: List<AppInfo>,
    selectedApps: Set<String>,
    isLoading: Boolean,
    onToggleApp: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(DS.Spacing.screenHorizontal)
    ) {
        Spacer(modifier = Modifier.height(DS.Spacing.lg))

        // Header
        Text(
            text = stringResource(R.string.onboarding_apps_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(DS.Spacing.xs))

        Text(
            text = stringResource(R.string.onboarding_apps_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Selection count
        if (selectedApps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(DS.Spacing.sm))
            Text(
                text = stringResource(R.string.onboarding_apps_selected, selectedApps.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(DS.Spacing.lg))

        // App List
        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DS.Spacing.xs)
            ) {
                // Suggested apps section
                item {
                    Text(
                        text = stringResource(R.string.onboarding_suggested_apps),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = DS.Spacing.xs)
                    )
                }

                val suggestedPackages = listOf(
                    "com.zhiliaoapp.musically", // TikTok
                    "com.ss.android.ugc.aweme", // Douyin
                    "com.instagram.android",
                    "com.twitter.android",
                    "com.facebook.katana",
                    "com.google.android.youtube"
                )

                val suggestedApps = availableApps.filter { it.packageName in suggestedPackages }
                val otherApps = availableApps.filter { it.packageName !in suggestedPackages }

                items(suggestedApps) { app ->
                    AppSelectionItem(
                        app = app,
                        isSelected = app.packageName in selectedApps,
                        onToggle = { onToggleApp(app.packageName) }
                    )
                }

                if (otherApps.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(DS.Spacing.md))
                        Text(
                            text = stringResource(R.string.onboarding_other_apps),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = DS.Spacing.xs)
                        )
                    }

                    items(otherApps) { app ->
                        AppSelectionItem(
                            app = app,
                            isSelected = app.packageName in selectedApps,
                            onToggle = { onToggleApp(app.packageName) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(DS.Spacing.md))

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DS.Spacing.md)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(DS.Size.buttonHeight),
                shape = DS.Shapes.buttonShape
            ) {
                Text(stringResource(R.string.back))
            }

            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(2f)
                    .height(DS.Size.buttonHeight),
                shape = DS.Shapes.buttonShape,
                enabled = selectedApps.isNotEmpty()
            ) {
                Text(
                    text = stringResource(R.string.onboarding_continue),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Skip option
        TextButton(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.onboarding_skip_for_now),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(DS.Spacing.md))
    }
}

@Composable
private fun AppSelectionItem(
    app: AppInfo,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DS.Radius.md))
            .clickable(onClick = onToggle)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .padding(DS.Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App initial
        Box(
            modifier = Modifier
                .size(DS.Size.avatarMd)
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

        Spacer(modifier = Modifier.width(DS.Spacing.md))

        Text(
            text = app.appName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

// ========================================
// Step 4: Complete
// ========================================
@Composable
private fun CompleteStep(
    selectedAppCount: Int,
    onComplete: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "complete_celebration")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = DS.Easing.emphasized),
            repeatMode = RepeatMode.Reverse
        ),
        label = "complete_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(DS.Spacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(0.2f))

        // Success Icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(DS.Spacing.xxl))

        // Title
        Text(
            text = stringResource(R.string.onboarding_complete_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(DS.Spacing.md))

        // Subtitle
        Text(
            text = stringResource(R.string.onboarding_complete_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = DS.Spacing.lg)
        )

        Spacer(modifier = Modifier.height(DS.Spacing.xl))

        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = DS.Shapes.cardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(DS.Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DS.Spacing.xxl)
                ) {
                    SummaryItem(
                        value = selectedAppCount.toString(),
                        label = stringResource(R.string.onboarding_apps_monitored)
                    )
                    SummaryItem(
                        value = stringResource(R.string.onboarding_gentle_mode_short),
                        label = stringResource(R.string.onboarding_default_mode)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.3f))

        // CTA Button
        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(DS.Size.buttonHeightLg),
            shape = DS.Shapes.buttonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = stringResource(R.string.onboarding_start_journey),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(DS.Spacing.xxl))
    }
}

@Composable
private fun SummaryItem(
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ========================================
// Data Classes
// ========================================
data class AppInfo(
    val packageName: String,
    val appName: String
)
