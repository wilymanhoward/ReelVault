package io.github.wilymanhoward.reelvault.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.wilymanhoward.reelvault.ui.collections.CollectionDetailScreen
import io.github.wilymanhoward.reelvault.ui.collections.CollectionsScreen
import io.github.wilymanhoward.reelvault.ui.common.LocalNavAnimatedScope
import io.github.wilymanhoward.reelvault.ui.common.LocalSharedTransitionScope
import io.github.wilymanhoward.reelvault.ui.home.HomeScreen
import io.github.wilymanhoward.reelvault.ui.item.ItemDetailScreen
import io.github.wilymanhoward.reelvault.ui.navigation.AboutRoute
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
import io.github.wilymanhoward.reelvault.ui.onboarding.OnboardingScreen
import io.github.wilymanhoward.reelvault.ui.search.SearchScreen
import io.github.wilymanhoward.reelvault.ui.settings.AboutScreen
import io.github.wilymanhoward.reelvault.ui.settings.AccountSection
import io.github.wilymanhoward.reelvault.ui.settings.SettingsScreen
import io.github.wilymanhoward.reelvault.ui.shared.SharedCollectionScreen
import io.github.wilymanhoward.reelvault.ui.tags.TagRulesScreen
import io.github.wilymanhoward.reelvault.ui.tags.TagsScreen

private data class TopLevelDestination(val route: Any, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val topLevelDestinations = listOf(
    TopLevelDestination(HomeRoute, "Saves", Icons.Outlined.GridView, Icons.Filled.GridView),
    TopLevelDestination(CollectionsRoute, "Collections", Icons.Outlined.Collections, Icons.Filled.Collections),
    TopLevelDestination(SearchRoute(), "Search", Icons.Outlined.Search, Icons.Filled.Search),
    TopLevelDestination(SettingsRoute, "Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
)

/**
 * Root UI: animated bottom navigation plus the navigation graph. Everything sits in one
 * SharedTransitionLayout so a card's thumbnail can morph into the detail screen's hero image.
 *
 * @param onboardingDone false on first launch, which starts at onboarding.
 * @param pendingShareId a collection link to open (from MainActivity), consumed via [onShareIdHandled].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppShell(
    onboardingDone: Boolean,
    onFinishOnboarding: () -> Unit,
    pendingShareId: String? = null,
    onShareIdHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = topLevelDestinations.any { currentDestination?.hasRoute(it.route::class) == true }
    val startDestination: Any = remember { if (onboardingDone) HomeRoute else OnboardingRoute }

    // A collection link (https://<project>.web.app/c/<id>) opened the app or arrived while it was open.
    LaunchedEffect(pendingShareId) {
        pendingShareId ?: return@LaunchedEffect
        navController.navigate(SharedCollectionRoute(pendingShareId)) { launchSingleTop = true }
        onShareIdHandled()
    }
    val openItem: (String) -> Unit = { navController.navigate(ItemDetailRoute(it)) }
    val back: () -> Unit = { navController.popBackStack() }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                NavigationBar {
                    topLevelDestinations.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any { it.hasRoute(dest.route::class) } == true
                        val scale by animateFloatAsState(
                            if (selected) 1.15f else 1f,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                            label = "tabScale",
                        )
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) dest.selectedIcon else dest.icon,
                                    contentDescription = null,
                                    modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
                                )
                            },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    // Screens draw their own top bars (with status-bar insets), so only the bottom is padded here.
                    modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
                    enterTransition = { fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 20 } },
                    exitTransition = { fadeOut(tween(180)) },
                    popEnterTransition = { fadeIn(tween(220)) },
                    popExitTransition = { fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 20 } },
                ) {
                    screen<OnboardingRoute> {
                        OnboardingScreen(onDone = {
                            onFinishOnboarding()
                            navController.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
                        })
                    }
                    screen<HomeRoute> { HomeScreen(onOpenItem = openItem) }
                    screen<CollectionsRoute> { CollectionsScreen(onOpenCollection = { navController.navigate(CollectionDetailRoute(it)) }) }
                    screen<SearchRoute> { SearchScreen(onOpenItem = openItem, onOpenTags = { navController.navigate(TagsRoute) }) }
                    screen<TagsRoute> { TagsScreen(onBack = back, onTagClick = { navController.navigate(SearchRoute(tag = it)) }) }
                    screen<SettingsRoute> {
                        SettingsScreen(
                            onEditRules = { navController.navigate(TagRulesRoute) },
                            onOpenAbout = { navController.navigate(AboutRoute) },
                            accountSection = { AccountSection() },
                        )
                    }
                    screen<TagRulesRoute> { TagRulesScreen(onBack = back) }
                    screen<AboutRoute> { AboutScreen(onBack = back) }
                    screen<CollectionDetailRoute> { CollectionDetailScreen(onBack = back, onOpenItem = openItem) }
                    screen<ItemDetailRoute> { ItemDetailScreen(onBack = back) }
                    screen<SharedCollectionRoute> { SharedCollectionScreen(onBack = back) }
                }
            }
        }
    }
}

/** A destination that also exposes its animation scope for shared elements. */
private inline fun <reified T : Any> NavGraphBuilder.screen(noinline content: @Composable (NavBackStackEntry) -> Unit) {
    composable<T> { entry ->
        CompositionLocalProvider(LocalNavAnimatedScope provides this) { content(entry) }
    }
}
