package io.github.wilymanhoward.reelvault.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A filled circle that springs in, then a checkmark that draws itself. Plays once on first composition. */
@Composable
fun AnimatedCheck(color: Color, checkColor: Color, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    val scale = remember { Animatable(0f) }
    val stroke = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        stroke.animateTo(1f, tween(durationMillis = 260))
    }
    Canvas(
        modifier
            .size(size)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
    ) {
        drawCircle(color)
        val path = Path().apply {
            moveTo(this@Canvas.size.width * 0.28f, this@Canvas.size.height * 0.52f)
            lineTo(this@Canvas.size.width * 0.44f, this@Canvas.size.height * 0.67f)
            lineTo(this@Canvas.size.width * 0.73f, this@Canvas.size.height * 0.36f)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * stroke.value, partial, true)
        drawPath(
            partial,
            checkColor,
            style = Stroke(width = this.size.minDimension * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
