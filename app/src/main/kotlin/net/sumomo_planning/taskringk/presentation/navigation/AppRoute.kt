package net.sumomo_planning.taskringk.presentation.navigation

/** Type-safe navigation routes. */
sealed class AppRoute(val route: String) {
    /** Authentication home (sign-in / sign-up). */
    data object Home : AppRoute("home")

    /** Main content — shown when user is signed in. Placeholder until Phase 3+. */
    data object Main : AppRoute("main")
}
