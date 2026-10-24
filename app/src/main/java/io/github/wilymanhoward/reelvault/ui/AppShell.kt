package io.github.wilymanhoward.reelvault.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.wilymanhoward.reelvault.ui.navigation.CollectionDetailRoute
import io.github.wilymanhoward.reelvault.ui.navigation.CollectionsRoute
import io.github.wilymanhoward.reelvault.ui.navigation.HomeRoute
import io.github.wilymanhoward.reelvault.ui.navigation.ItemDetailRoute
import io.github.wilymanhoward.reelvault.ui.navigation.OnboardingRoute
import io.github.wilymanhoward.reelvault.ui.navigation.SearchRoute
import io.github.wilymanhoward.reelvault.ui.navigation.SettingsRoute
import io.github.wilymanhoward.reelvault.ui.navigation.SharedCollectionRoute
import io.github.wilymanhoward.reelvault.ui.navigation.TagsRoute

private data class TopLevelDestination(val route: Any, val label: String, val icon: ImageVector)

private val topLevelDestinations = listOf(
    TopLevelDestination(HomeRoute, "Saves", Icons.Outlined.GridView),
    TopLevelDestination(CollectionsRoute, "Collections", Icons.Outlined.Collections),
    TopLevelDestination(SearchRoute, "Search", Icons.Outlined.Search),
    TopLevelDestination(SettingsRoute, "Settings", Icons.Outlined.Settings),
)

/** Root UI: bottom navigation plus the navigation graph for every screen. */
@Composable
fun AppShell() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = topLevelDestinations.any { currentDestination?.hasRoute(it.route::class) == true }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any { it.hasRoute(dest.route::class) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable<OnboardingRoute> { PlaceholderScreen("Onboarding") }
            composable<HomeRoute> { PlaceholderScreen("All saves") }
            composable<CollectionsRoute> { PlaceholderScreen("Collections") }
            composable<SearchRoute> { PlaceholderScreen("Search") }
            composable<TagsRoute> { PlaceholderScreen("Tags") }
            composable<SettingsRoute> { PlaceholderScreen("Settings") }
            composable<CollectionDetailRoute> { PlaceholderScreen("Collection ${it.toRoute<CollectionDetailRoute>().collectionId}") }
            composable<ItemDetailRoute> { PlaceholderScreen("Item ${it.toRoute<ItemDetailRoute>().itemId}") }
            composable<SharedCollectionRoute> { PlaceholderScreen("Shared ${it.toRoute<SharedCollectionRoute>().shareId}") }
        }
    }
}

// shortcut: stand-in screens for the shell; each phase replaces its own.
@Composable
private fun PlaceholderScreen(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}
