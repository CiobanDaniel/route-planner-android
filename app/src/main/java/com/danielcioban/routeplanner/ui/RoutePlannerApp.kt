package com.danielcioban.routeplanner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.danielcioban.routeplanner.RoutePlannerApplication
import com.danielcioban.routeplanner.ui.navigation.AppDestinations
import com.danielcioban.routeplanner.ui.routes.EditRouteScreen
import com.danielcioban.routeplanner.ui.routes.EditRouteViewModel
import com.danielcioban.routeplanner.ui.routes.RouteDetailScreen
import com.danielcioban.routeplanner.ui.routes.RouteDetailViewModel
import com.danielcioban.routeplanner.ui.routes.RouteListScreen
import com.danielcioban.routeplanner.ui.routes.RouteListViewModel

@Composable
fun RoutePlannerApp() {
    val context = LocalContext.current
    val repository = remember {
        (context.applicationContext as RoutePlannerApplication).repository
    }
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppDestinations.ROUTE_LIST,
    ) {
        composable(AppDestinations.ROUTE_LIST) {
            val viewModel: RouteListViewModel = viewModel(
                factory = RouteListViewModel.Factory(repository),
            )
            RouteListScreen(
                viewModel = viewModel,
                onCreateRoute = { navController.navigate(AppDestinations.routeCreate()) },
                onOpenRoute = { routeId -> navController.navigate(AppDestinations.routeDetail(routeId)) },
            )
        }

        composable(
            route = AppDestinations.ROUTE_DETAIL,
            arguments = listOf(navArgument("routeId") { type = NavType.LongType }),
        ) { entry ->
            val routeId = entry.arguments?.getLong("routeId") ?: return@composable
            val viewModel: RouteDetailViewModel = viewModel(
                factory = RouteDetailViewModel.Factory(repository, routeId),
            )
            RouteDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(AppDestinations.routeEdit(id)) },
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
                onSaved = { savedId ->
                    navController.navigate(AppDestinations.routeDetail(savedId)) {
                        popUpTo(AppDestinations.ROUTE_LIST)
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
