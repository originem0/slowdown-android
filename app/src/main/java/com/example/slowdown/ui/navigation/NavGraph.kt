package com.sharonZ.slowdown.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.sharonZ.slowdown.R
import com.sharonZ.slowdown.data.repository.SlowDownRepository
import com.sharonZ.slowdown.ui.screen.AppDetailScreen
import com.sharonZ.slowdown.ui.screen.AppListScreen
import com.sharonZ.slowdown.ui.screen.DashboardScreen
import com.sharonZ.slowdown.ui.screen.SettingsScreen
import com.sharonZ.slowdown.ui.screen.StatisticsScreen
import com.sharonZ.slowdown.viewmodel.AppDetailViewModel
import com.sharonZ.slowdown.viewmodel.AppListViewModel
import com.sharonZ.slowdown.viewmodel.DashboardViewModel
import com.sharonZ.slowdown.viewmodel.SettingsViewModel
import com.sharonZ.slowdown.viewmodel.StatisticsViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object AppList : Screen("app_list")
    data object Statistics : Screen("statistics")
    data object Settings : Screen("settings")
    data object AppDetail : Screen("app_detail/{packageName}") {
        fun createRoute(packageName: String): String {
            val encoded = URLEncoder.encode(packageName, StandardCharsets.UTF_8.toString())
            return "app_detail/$encoded"
        }
    }
}

/**
 * 底部导航项定义 - 使用 outlined/filled 图标区分选中状态
 */
data class BottomNavItem(
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelResId: Int
)

/**
 * 底部导航栏项目列表
 */
val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, Icons.Filled.Home, Icons.Outlined.Home, R.string.nav_home),
    BottomNavItem(Screen.AppList, Icons.Filled.Apps, Icons.Outlined.Apps, R.string.nav_apps),
    BottomNavItem(Screen.Statistics, Icons.Filled.BarChart, Icons.Outlined.BarChart, R.string.nav_statistics),
    BottomNavItem(Screen.Settings, Icons.Filled.Settings, Icons.Outlined.Settings, R.string.nav_settings)
)

@Composable
fun SlowDownNavGraph(
    navController: NavHostController,
    repository: SlowDownRepository,
    context: Context
) {
    Scaffold(
        bottomBar = {
            SlowDownBottomBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                val viewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.Factory(repository, context)
                )
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAppList = {
                        navController.navigate(Screen.AppList.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.AppList.route) {
                val viewModel: AppListViewModel = viewModel(
                    factory = AppListViewModel.Factory(repository, context)
                )
                AppListScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAppDetail = { packageName ->
                        navController.navigate(Screen.AppDetail.createRoute(packageName))
                    }
                )
            }

            composable(Screen.Statistics.route) {
                val viewModel: StatisticsViewModel = viewModel(
                    factory = StatisticsViewModel.Factory(repository, context)
                )
                StatisticsScreen(
                    viewModel = viewModel
                )
            }

            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(repository, context)
                )
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.AppDetail.route,
                arguments = listOf(
                    navArgument("packageName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encodedPackageName = backStackEntry.arguments?.getString("packageName") ?: ""
                val packageName = URLDecoder.decode(encodedPackageName, StandardCharsets.UTF_8.toString())

                val viewModel: AppDetailViewModel = viewModel(
                    factory = AppDetailViewModel.Factory(repository, context, packageName)
                )
                AppDetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun SlowDownBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // 只在主要导航页面显示底部栏，在详情页隐藏
    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.screen.route }

    if (showBottomBar) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = NavigationBarDefaults.Elevation
        ) {
            bottomNavItems.forEach { item ->
                val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true

                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = stringResource(item.labelResId)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(item.labelResId),
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                        )
                    },
                    selected = selected,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    onClick = {
                        navController.navigate(item.screen.route) {
                            // 避免在后退栈中累积多个相同目的地
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // 避免在顶部重复创建相同目的地
                            launchSingleTop = true
                            // 重新选择之前选中的项目时恢复状态
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}
