package com.danielcioban.routeplanner.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.danielcioban.routeplanner.RoutePlannerApplication
import com.danielcioban.routeplanner.data.settings.AppSettings
import com.danielcioban.routeplanner.ui.about.AboutScreen
import com.danielcioban.routeplanner.ui.library.StopLibraryScreen
import com.danielcioban.routeplanner.ui.library.StopLibraryViewModel
import com.danielcioban.routeplanner.ui.navigation.AppDestinations
import com.danielcioban.routeplanner.ui.routes.EditRouteScreen
import com.danielcioban.routeplanner.ui.routes.EditRouteViewModel
import com.danielcioban.routeplanner.ui.routes.RouteDetailScreen
import com.danielcioban.routeplanner.ui.routes.RouteDetailViewModel
import com.danielcioban.routeplanner.ui.routes.RouteListScreen
import com.danielcioban.routeplanner.ui.routes.RouteListViewModel
import com.danielcioban.routeplanner.ui.settings.SettingsScreen
import com.danielcioban.routeplanner.ui.settings.SettingsViewModel
import com.danielcioban.routeplanner.util.NetworkStatus

@Composable
fun RoutePlannerApp(
    settings: AppSettings = AppSettings(),
) {
    val context = LocalContext.current
    val app = remember { context.applicationContext as RoutePlannerApplication }
    val repository = remember { app.repository }
    val settingsRepository = remember { app.settingsRepository }
    val deliverySessionStore = remember { app.deliverySessionStore }
    val navController = rememberNavController()
    val appVersion = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { "0.3.1" }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = AppDestinations.ROUTE_LIST,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(AppDestinations.ROUTE_LIST) {
                val viewModel: RouteListViewModel = viewModel(
                    factory = RouteListViewModel.Factory(repository, deliverySessionStore),
                )
                RouteListScreen(
                    viewModel = viewModel,
                    onCreateRoute = { navController.navigate(AppDestinations.routeCreate()) },
                    onOpenRoute = { routeId ->
                        navController.navigate(AppDestinations.routeDetail(routeId))
                    },
                    onOpenSettings = { navController.navigate(AppDestinations.SETTINGS) },
                    onOpenAbout = { navController.navigate(AppDestinations.ABOUT) },
                    onOpenStopLibrary = { navController.navigate(AppDestinations.STOP_LIBRARY) },
                )
            }

            composable(
                route = AppDestinations.ROUTE_DETAIL,
                arguments = listOf(navArgument("routeId") { type = NavType.LongType }),
            ) { entry ->
                val routeId = entry.arguments?.getLong("routeId") ?: return@composable
                val viewModel: RouteDetailViewModel = viewModel(
                    factory = RouteDetailViewModel.Factory(
                        repository = repository,
                        routeId = routeId,
                        deliverySessionStore = deliverySessionStore,
                        isOnline = { NetworkStatus.isOnline(app) },
                    ),
                )
                RouteDetailScreen(
                    viewModel = viewModel,
                    settings = settings,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(AppDestinations.routeEdit(id)) },
                    onOpenStopLibrary = { navController.navigate(AppDestinations.STOP_LIBRARY) },
                )
            }

            composable(
                route = AppDestinations.ROUTE_EDIT,
                arguments = listOf(navArgument("routeId") { type = NavType.LongType }),
            ) { entry ->
                val rawId = entry.arguments?.getLong("routeId") ?: -1L
                val routeId = rawId.takeIf { it > 0 }
                val viewModel: EditRouteViewModel = viewModel(
                    factory = EditRouteViewModel.Factory(repository, routeId),
                )
                EditRouteScreen(
                    viewModel = viewModel,
                    isNew = routeId == null,
                    onBack = { navController.popBackStack() },
                    onOpenStopLibrary = { navController.navigate(AppDestinations.STOP_LIBRARY) },
                    onSaved = { savedId ->
                        navController.navigate(AppDestinations.routeDetail(savedId)) {
                            popUpTo(AppDestinations.ROUTE_LIST)
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(AppDestinations.SETTINGS) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(settingsRepository),
                )
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(AppDestinations.ABOUT) {
                AboutScreen(
                    appVersion = appVersion,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(AppDestinations.STOP_LIBRARY) {
                val viewModel: StopLibraryViewModel = viewModel(
                    factory = StopLibraryViewModel.Factory(repository),
                )
                StopLibraryScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
