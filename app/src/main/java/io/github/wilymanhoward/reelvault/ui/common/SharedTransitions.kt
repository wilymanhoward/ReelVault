package io.github.wilymanhoward.reelvault.ui.common

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/** The app-wide shared transition scope, set by AppShell's SharedTransitionLayout. */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** The current navigation destination's animation scope, set per destination in AppShell. */
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Marks this element as the same object across screens under [key] (e.g. a card's thumbnail and
 * the detail screen's hero image), so navigation morphs one into the other. A no-op outside AppShell.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedAcrossScreens(key: String): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animated = LocalNavAnimatedScope.current ?: return this
    return with(shared) { this@sharedAcrossScreens.sharedElement(rememberSharedContentState(key), animated) }
}

/** Shared-element key for a save's thumbnail. */
fun thumbnailKey(itemId: String) = "thumb-$itemId"
