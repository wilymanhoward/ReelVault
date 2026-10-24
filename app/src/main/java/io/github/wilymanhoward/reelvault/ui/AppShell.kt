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
import io.github.wilymanhoward.reelvault.ui.collections.CollectionDetailScreen
import io.github.wilymanhoward.reelvault.ui.collections.CollectionsScreen
import io.github.wilymanhoward.reelvault.ui.home.HomeScreen
import io.github.wilymanhoward.reelvault.ui.item.ItemDetailScreen
import io.github.wilymanhoward.reelvault.ui.navigation.CollectionDetailRoute
import io.github.wilymanhoward.reelvault.ui.navigation.CollectionsRoute
import io.github.wilymanhoward.reelvault.ui.navigation.HomeRoute
import io.github.wilymanhoward.reelvault.ui.navigation.ItemDetailRoute
import io.github.wilymanhoward.reelvault.ui.navigation.OnboardingRoute
import io.github.wilymanhoward.reelvault.ui.navigation.SearchRoute
import io.github.wilymanhoward.reelvault.ui.navigation.SettingsRoute
import io.github.wilymanhoward.reelvault.ui.navigation.SharedCollectionRoute
import io.github.wilymanhoward.reelvault.ui.navigation.TagRulesRoute
import io.github.wilymanhoward.reelvault.ui.navigation.TagsRoute
import io.github.wilymanhoward.reelvault.ui.search.SearchScreen
import io.github.wilymanhoward.reelvault.ui.settings.SettingsScreen
import io.github.wilymanhoward.reelvault.ui.tags.TagRulesScreen
import io.github.wilymanhoward.reelvault.ui.tags.TagsScreen

private data class TopLevelDestination(val route: Any, val label: String, val icon: ImageVector)

private val topLevelDestinations = listOf(
    TopLevelDestination(HomeRoute, "Saves", Icons.Outlined.GridView),
    TopLevelDestination(CollectionsRoute, "Collections", Icons.Outlined.Collections),
    TopLevelDestination(SearchRoute(), "Search", Icons.Outlined.Search),
    TopLevelDestination(SettingsRoute, "Settings", Icons.Outlined.Settings),
)

/** Root UI: bottom navigation plus the navigation graph for every screen. */
@Composable
fun AppShell() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = topLevelDestinations.any { currentDestination?.hasRoute(it.route::class) == true }
    val openItem: (String) -> Unit = { navController.navigate(ItemDetailRoute(it)) }
    val back: () -> Unit = { navController.popBackStack() }

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
            // Screens draw their own top bars (with status-bar insets), so only the bottom is padded here.
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable<OnboardingRoute> { PlaceholderScreen("Onboarding") }
            composable<HomeRoute> { HomeScreen(onOpenItem = openItem) }
            composable<CollectionsRoute> { CollectionsScreen(onOpenCollection = { navController.navigate(CollectionDetailRoute(it)) }) }
            composable<SearchRoute> { SearchScreen(onOpenItem = openItem, onOpenTags = { navController.navigate(TagsRoute) }) }
            composable<TagsRoute> { TagsScreen(onBack = back, onTagClick = { navController.navigate(SearchRoute(tag = it)) }) }
            composable<SettingsRoute> { SettingsScreen(onEditRules = { navController.navigate(TagRulesRoute) }, accountSection = {}) }
            composable<TagRulesRoute> { TagRulesScreen(onBack = back) }
            composable<CollectionDetailRoute> { CollectionDetailScreen(onBack = back, onOpenItem = openItem) }
            composable<ItemDetailRoute> { ItemDetailScreen(onBack = back) }
            composable<SharedCollectionRoute> { PlaceholderScreen("Shared ${it.toRoute<SharedCollectionRoute>().shareId}") }
        }
    }
}

// shortcut: stand-in screens for onboarding (Phase 7) and the shared viewer (Phase 6).
@Composable
private fun PlaceholderScreen(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}
