package io.github.wilymanhoward.reelvault.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.wilymanhoward.reelvault.ui.theme.LocalReducedMotion
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * The "Saved" moment: a filled circle springs in, a checkmark draws itself and a ring of dots
 * bursts outward, with a confirm haptic. Plays once; shows the finished state when animations are off.
 *
 * @param burstColor dots of the burst; defaults to [color].
 */
@Composable
fun AnimatedCheck(
    color: Color,
    checkColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    burstColor: Color = color,
) {
    val reduced = LocalReducedMotion.current
    val haptics = LocalHapticFeedback.current
    val scale = remember { Animatable(if (reduced) 1f else 0f) }
    val stroke = remember { Animatable(if (reduced) 1f else 0f) }
    val burst = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        if (reduced) return@LaunchedEffect
        launch { burst.animateTo(1f, tween(durationMillis = 650, delayMillis = 120, easing = FastOutSlowInEasing)) }
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        stroke.animateTo(1f, tween(durationMillis = 260))
    }
    // The canvas is twice the circle so the burst has room to travel.
    Canvas(modifier.size(size * 2)) {
        val c = center
        val r = this.size.minDimension / 4f
        // Burst: 10 dots move from the circle's edge outward and fade.
        val b = burst.value
        if (b in 0.001f..0.999f) {
            repeat(10) { i ->
                val a = Math.toRadians(i * 36.0 + 18)
                val distance = r * (1.05f + 0.85f * b)
                drawCircle(
                    burstColor.copy(alpha = 1f - b),
                    radius = r * 0.09f * (1f - b * 0.5f),
                    center = Offset(c.x + distance * cos(a).toFloat(), c.y + distance * sin(a).toFloat()),
                )
            }
        }
        drawCircle(color, radius = r * scale.value, center = c)
        val path = Path().apply {
            moveTo(c.x - r * 0.44f, c.y + r * 0.04f)
            lineTo(c.x - r * 0.12f, c.y + r * 0.34f)
            lineTo(c.x + r * 0.46f, c.y - r * 0.28f)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * stroke.value, partial, true)
        drawPath(partial, checkColor, style = Stroke(width = r * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
