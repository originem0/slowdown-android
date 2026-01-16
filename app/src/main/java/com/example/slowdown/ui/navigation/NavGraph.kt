package com.example.slowdown.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.slowdown.data.repository.SlowDownRepository
import com.example.slowdown.ui.screen.AppDetailScreen
import com.example.slowdown.ui.screen.AppListScreen
import com.example.slowdown.ui.screen.DashboardScreen
import com.example.slowdown.ui.screen.SettingsScreen
import com.example.slowdown.viewmodel.AppDetailViewModel
import com.example.slowdown.viewmodel.AppListViewModel
import com.example.slowdown.viewmodel.DashboardViewModel
import com.example.slowdown.viewmodel.SettingsViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object AppList : Screen("app_list")
    data object Settings : Screen("settings")
    data object AppDetail : Screen("app_detail/{packageName}") {
        fun createRoute(packageName: String): String {
            val encoded = URLEncoder.encode(packageName, StandardCharsets.UTF_8.toString())
            return "app_detail/$encoded"
        }
    }
}

@Composable
fun SlowDownNavGraph(
    navController: NavHostController,
    repository: SlowDownRepository,
    context: Context
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            val viewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.Factory(repository, context)
            )
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToAppList = { navController.navigate(Screen.AppList.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
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
