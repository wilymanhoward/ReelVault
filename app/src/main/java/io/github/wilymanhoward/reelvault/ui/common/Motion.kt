package io.github.wilymanhoward.reelvault.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.wilymanhoward.reelvault.ui.theme.LocalReducedMotion
import kotlinx.coroutines.delay

/** Shrinks a little while [interactionSource] is pressed and springs back on release. */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressedScale: Float = 0.95f): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * Fades and lifts an item in once, delayed by its [index], for the first screenful of a list.
 * Items past [maxStaggered] (or when [enabled] is false, e.g. after the first load) appear at once.
 */
@Composable
fun Modifier.staggeredEntrance(index: Int, enabled: Boolean, maxStaggered: Int = 12): Modifier {
    val reduced = LocalReducedMotion.current
    val animate = enabled && !reduced && index < maxStaggered
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay(index * 45L)
            progress.animateTo(1f, tween(durationMillis = 380))
        }
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 24.dp.toPx()
    }
}
