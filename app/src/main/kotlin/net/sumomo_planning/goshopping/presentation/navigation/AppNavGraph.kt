package net.sumomo_planning.goshopping.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.sumomo_planning.goshopping.presentation.auth.AuthViewModel
import net.sumomo_planning.goshopping.presentation.auth.HomeScreen
import net.sumomo_planning.goshopping.presentation.main.MainScreen

/**
 * Root navigation graph.
 *
 * Start destination is determined by the current auth state:
 * - Not signed in  → [AppRoute.Home]
 * - Signed in      → [AppRoute.Main]
 *
 * The [AuthViewModel] is scoped to the NavHost so it is shared between
 * composables that need auth state (e.g. a sign-out button in MainScreen).
 */
@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    // Determine start destination from current auth state (avoids flicker on re-compose)
    val authViewModel: AuthViewModel = hiltViewModel()
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val startDestination = if (uiState.currentUser != null) AppRoute.Main.route
                           else AppRoute.Home.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(AppRoute.Home.route) {
            HomeScreen(
                onAuthSuccess = {
                    navController.navigate(AppRoute.Main.route) {
                        popUpTo(AppRoute.Home.route) { inclusive = true }
                    }
                },
            )
        }

        composable(AppRoute.Main.route) {
            MainScreen(
                onSignOut = {
                    navController.navigate(AppRoute.Home.route) {
                        popUpTo(AppRoute.Main.route) { inclusive = true }
                    }
                },
            )
        }
    }
}
