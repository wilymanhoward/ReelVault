package io.github.wilymanhoward.reelvault.ui

import io.github.wilymanhoward.reelvault.ui.common.VaultIcons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import io.github.wilymanhoward.reelvault.ui.common.VaultTab
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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

private data class TopLevelDestination(val route: Any, val label: String, val icon: ImageVector)

private val topLevelDestinations = listOf(
    TopLevelDestination(HomeRoute, "Library", VaultIcons.Library),
    TopLevelDestination(CollectionsRoute, "Collections", VaultIcons.Collections),
    TopLevelDestination(SearchRoute(), "Search", VaultIcons.Search),
    TopLevelDestination(SettingsRoute, "Settings", VaultIcons.Settings),
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
                Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            topLevelDestinations.forEach { dest ->
                                val selected = currentDestination?.hierarchy?.any { it.hasRoute(dest.route::class) } == true
                                VaultTab(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(dest.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(dest.icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
                                    label = dest.label,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
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
