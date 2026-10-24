package io.github.wilymanhoward.reelvault.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import io.github.wilymanhoward.reelvault.domain.model.Platform
import io.github.wilymanhoward.reelvault.ui.common.VaultDial
import io.github.wilymanhoward.reelvault.ui.theme.LocalReducedMotion
import io.github.wilymanhoward.reelvault.ui.theme.style
import kotlinx.coroutines.launch

private data class Page(val title: String, val body: String)

private val pages = listOf(
    Page("Share to ReelVault", "In TikTok, Instagram, YouTube or any app, tap Share and pick ReelVault. It saves without leaving the app you're in."),
    Page("Every save in one grid", "Previews, tags and search for videos from every platform. Tap one to watch it in its own app."),
    Page("Collect and share", "Sort saves into collections and send one to friends as a link. Sign in to sync between devices."),
)

/** First-launch introduction: three short pages, each with a small animation of what it explains. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDone) { Text(if (last) "" else "Skip") }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
            val active = pager.currentPage == index
            Column(Modifier.fillMaxSize().padding(horizontal = 28.dp), verticalArrangement = Arrangement.Center) {
                Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
                    when (index) {
                        0 -> ShareSheetArt(active)
                        1 -> GridArt(active)
                        else -> CollectArt(active)
                    }
                }
                Text(pages[index].title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 32.dp))
                Text(
                    pages[index].body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PageDots(count = pages.size, current = pager.currentPage)
            Button(onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) {
                AnimatedContent(targetState = last, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "cta") { isLast ->
                    Text(if (isLast) "Get started" else "Next")
                }
            }
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val width by animateDpAsState(if (i == current) 24.dp else 8.dp, spring(stiffness = Spring.StiffnessMediumLow), label = "dot")
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(RoundedCornerShape(50))
                    .background(if (i == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

/** 0 → 1 once the page becomes [active]; already 1 when animations are off. */
@Composable
private fun rememberEntrance(active: Boolean, durationMillis: Int = 900): Animatable<Float, *> {
    val reduced = LocalReducedMotion.current
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(active, reduced) {
        if (!active || reduced) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis))
    }
    return progress
}

/** A phone with a share sheet sliding up and the ReelVault target lighting up. */
@Composable
private fun ShareSheetArt(active: Boolean) {
    val p = rememberEntrance(active, 1_100)
    val colors = MaterialTheme.colorScheme
    val apps = listOf(Color(0xFF4CB782), Color(0xFF3B8BFF), Color(0xFFF2A93B))
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(width = 170.dp, height = 270.dp)) {
            val phone = CornerRadius(28.dp.toPx())
            drawRoundRect(colors.surfaceContainerHigh, cornerRadius = phone)
            // The video playing behind the sheet.
            drawRoundRect(Brush.verticalGradient(Platform.TIKTOK.style.colors), Offset(10.dp.toPx(), 10.dp.toPx()), Size(size.width - 20.dp.toPx(), size.height - 20.dp.toPx()), CornerRadius(20.dp.toPx()))
            val sheetHeight = size.height * 0.42f
            val slide = (1f - easeOut(p.value.coerceIn(0f, 0.6f) / 0.6f)) * sheetHeight
            translate(top = size.height - sheetHeight + slide) {
                drawRoundRect(colors.surface, size = Size(size.width, sheetHeight), cornerRadius = CornerRadius(22.dp.toPx()))
                val dot = 30.dp.toPx()
                val gap = (size.width - dot * 4) / 5
                apps.forEachIndexed { i, c -> drawCircle(c, dot / 2, Offset(gap + dot / 2 + i * (dot + gap), sheetHeight * 0.4f)) }
                // ReelVault's slot, pulsing once the sheet is up.
                val pulse = ((p.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
                val center = Offset(gap + dot / 2 + 3 * (dot + gap), sheetHeight * 0.4f)
                drawCircle(colors.primary.copy(alpha = 0.25f * (1f - pulse)), dot / 2 * (1f + pulse * 0.9f), center)
                drawCircle(colors.primary, dot / 2, center)
                drawCircle(colors.tertiary, dot / 6, center)
            }
        }
    }
}

/** Cards from different platforms dropping into a grid. */
@Composable
private fun GridArt(active: Boolean) {
    val p = rememberEntrance(active, 1_000)
    val cards = listOf(Platform.TIKTOK, Platform.YOUTUBE, Platform.INSTAGRAM, Platform.XIAOHONGSHU, Platform.PINTEREST, Platform.X)
    Canvas(Modifier.size(width = 240.dp, height = 270.dp)) {
        val cols = 3
        val gap = 10.dp.toPx()
        val w = (size.width - gap * (cols - 1)) / cols
        val h = w * 1.5f
        cards.forEachIndexed { i, platform ->
            val local = ((p.value - i * 0.09f) / 0.5f).coerceIn(0f, 1f)
            val drop = (1f - easeOutBack(local)) * -60.dp.toPx()
            val x = (i % cols) * (w + gap)
            val y = (i / cols) * (h + gap) + (size.height - (h * 2 + gap)) / 2
            translate(x, y + drop) {
                drawCard(platform.style.colors, Size(w, h), alpha = local)
            }
        }
    }
}

/** Cards fanning into one stack with the vault dial on top: a collection. */
@Composable
private fun CollectArt(active: Boolean) {
    val p = rememberEntrance(active, 1_000)
    val cards = listOf(Platform.INSTAGRAM, Platform.TIKTOK, Platform.YOUTUBE)
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(240.dp)) {
            val w = size.width * 0.42f
            val h = w * 1.5f
            cards.forEachIndexed { i, platform ->
                val spread = (1f - easeOut(p.value.coerceIn(0f, 0.7f) / 0.7f))
                val angle = (i - 1) * (8f + 22f * spread)
                val dx = (i - 1) * size.width * 0.25f * spread
                translate(size.width / 2 - w / 2 + dx, size.height / 2 - h / 2) {
                    rotate(angle, pivot = Offset(w / 2, h)) { drawCard(platform.style.colors, Size(w, h)) }
                }
            }
        }
        val appear = ((p.value - 0.65f) / 0.35f).coerceIn(0f, 1f)
        VaultDial(
            size = 72.dp,
            ringColor = Color.White,
            playColor = MaterialTheme.colorScheme.tertiary,
            animate = false,
            modifier = Modifier.padding(top = 8.dp).then(Modifier.size(72.dp * appear.coerceAtLeast(0.01f))),
        )
    }
}

private fun DrawScope.drawCard(colors: List<Color>, size: Size, alpha: Float = 1f) {
    drawRoundRect(Brush.linearGradient(colors, end = Offset(size.width, size.height)), size = size, cornerRadius = CornerRadius(14.dp.toPx()), alpha = alpha)
    drawRoundRect(Color.White.copy(alpha = 0.35f * alpha), Offset(8.dp.toPx(), size.height - 22.dp.toPx()), Size(size.width * 0.6f, 6.dp.toPx()), CornerRadius(3.dp.toPx()))
    drawRoundRect(Color.White.copy(alpha = 0.6f * alpha), Offset(8.dp.toPx(), 8.dp.toPx()), Size(size.width * 0.35f, 8.dp.toPx()), CornerRadius(4.dp.toPx()), style = Stroke(1.dp.toPx()))
}

private fun easeOut(t: Float) = 1f - (1f - t) * (1f - t) * (1f - t)

private fun easeOutBack(t: Float): Float {
    val c1 = 1.4f
    val c3 = c1 + 1
    val x = t - 1
    return 1 + c3 * x * x * x + c1 * x * x
}
