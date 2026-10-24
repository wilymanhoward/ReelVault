package io.github.wilymanhoward.reelvault.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import io.github.wilymanhoward.reelvault.ui.theme.LocalReducedMotion
import kotlin.math.cos
import kotlin.math.sin

/**
 * The ReelVault mark drawn in Compose: a vault dial (ring + ticks) around a play triangle, the same
 * shape as the app icon. When [animate], the dial turns like a combination lock (left, right, settle)
 * and repeats after a pause; it holds still when the system's animations are off.
 */
@Composable
fun VaultDial(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    ringColor: Color = MaterialTheme.colorScheme.primary,
    playColor: Color = MaterialTheme.colorScheme.tertiary,
    animate: Boolean = true,
) {
    val reduced = LocalReducedMotion.current
    val angle = remember { Animatable(0f) }
    LaunchedEffect(animate, reduced) {
        if (!animate || reduced) return@LaunchedEffect
        val spin = tween<Float>(durationMillis = 700, easing = FastOutSlowInEasing)
        while (true) {
            angle.animateTo(-60f, spin)
            angle.animateTo(35f, spin)
            angle.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
            kotlinx.coroutines.delay(2_400)
        }
    }
    Canvas(modifier.size(size)) {
        val c = center
        val r = this.size.minDimension * 0.34f
        val stroke = this.size.minDimension * 0.045f
        rotate(angle.value, c) {
            drawCircle(ringColor, radius = r, center = c, style = Stroke(width = stroke))
            repeat(8) { i ->
                val a = Math.toRadians(i * 45.0)
                val inner = r * 1.23f
                val outer = r * 1.41f
                drawLine(
                    ringColor,
                    start = Offset(c.x + inner * cos(a).toFloat(), c.y + inner * sin(a).toFloat()),
                    end = Offset(c.x + outer * cos(a).toFloat(), c.y + outer * sin(a).toFloat()),
                    strokeWidth = stroke * 0.8f,
                    cap = StrokeCap.Round,
                )
            }
        }
        val s = r * 0.45f
        val play = Path().apply {
            moveTo(c.x - s * 0.55f, c.y - s * 0.85f)
            lineTo(c.x - s * 0.55f, c.y + s * 0.85f)
            lineTo(c.x + s * 0.95f, c.y)
            close()
        }
        drawPath(play, playColor)
    }
}

/** An empty screen: the animated dial, a title saying what's missing, and what to do about it. */
@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.widthIn(max = 360.dp),
        ) {
            VaultDial(size = 112.dp)
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            action()
        }
    }
}
