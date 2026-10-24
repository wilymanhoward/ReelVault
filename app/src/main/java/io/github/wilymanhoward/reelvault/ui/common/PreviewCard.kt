package io.github.wilymanhoward.reelvault.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.network.HttpException
import androidx.compose.ui.graphics.graphicsLayer
import io.github.wilymanhoward.reelvault.domain.model.Platform
import io.github.wilymanhoward.reelvault.domain.model.PreviewState
import io.github.wilymanhoward.reelvault.domain.model.SavedItem
import io.github.wilymanhoward.reelvault.ui.theme.style
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File

private enum class ImageState { LOADING, SHOWN, FAILED }

/** Callbacks shared by grid cards and list rows. */
class SaveActions(
    val onClick: (SavedItem) -> Unit,
    val onLongClick: (SavedItem) -> Unit,
    val onDetails: (SavedItem) -> Unit,
    val onThumbnailGone: (SavedItem) -> Unit,
)

/**
 * A saved item as a grid card: thumbnail with a gradient scrim, platform badge, title and author.
 * Without a usable image it shows the platform's gradient art instead, never a broken image.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreviewCard(
    item: SavedItem,
    selected: Boolean,
    actions: SaveActions,
    modifier: Modifier = Modifier,
    showDetails: Boolean = true,
) {
    val text = item.title ?: item.caption
    val shape = RoundedCornerShape(20.dp)
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val borderWidth by animateDpAsState(if (selected) 3.dp else 0.dp, label = "selectionBorder")
    Card(
        modifier = modifier
            .pressScale(interaction)
            .clip(shape)
            // Compose draws a 0.dp border as a hairline, so only add it while it's visible.
            .then(if (borderWidth > 0.dp) Modifier.border(borderWidth, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    actions.onClick(item)
                },
                onLongClick = { actions.onLongClick(item) },
            ),
        shape = shape,
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(item.platform.style.aspectRatio)) {
            Thumbnail(
                item,
                Modifier.fillMaxSize().sharedAcrossScreens(thumbnailKey(item.id)),
                onGone = { actions.onThumbnailGone(item) },
            ) { shown ->
                if (shown) {
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.72f))))
                    Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                        text?.let { Text(it, color = Color.White, style = MaterialTheme.typography.titleSmall, maxLines = 3, overflow = TextOverflow.Ellipsis) }
                        item.authorName?.let { Text(it, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium, maxLines = 1) }
                    }
                } else {
                    FallbackText(item, Modifier.align(Alignment.BottomStart))
                }
            }
            PlatformBadge(item, Modifier.align(Alignment.TopStart).padding(10.dp))
            StatusIcons(item, Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 40.dp))
            if (showDetails) {
                IconButton(
                    onClick = { actions.onDetails(item) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                ) {
                    Icon(
                        Icons.Outlined.MoreHoriz,
                        contentDescription = "Details",
                        tint = Color.White,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.35f), CircleShape).padding(4.dp),
                    )
                }
            }
            SelectedMark(selected, Modifier.align(Alignment.Center))
        }
    }
}

/** The check that pops in over a selected card. */
@Composable
private fun SelectedMark(selected: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = selected,
        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier,
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = "Selected",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp).background(Color.White, CircleShape),
        )
    }
}

/**
 * The item's thumbnail with loading shimmer and platform fallback art. [overlay] is drawn on top and
 * told whether a real image is showing.
 *
 * @param onGone called when the remote thumbnail answers 403/404 (expired signed URL).
 */
@Composable
fun Thumbnail(
    item: SavedItem,
    modifier: Modifier = Modifier,
    onGone: () -> Unit = {},
    overlay: @Composable androidx.compose.foundation.layout.BoxScope.(shown: Boolean) -> Unit = {},
) {
    val local = remember(item.localThumbnailPath) { item.localThumbnailPath?.let(::File)?.takeIf { it.exists() } }
    val model: Any? = local ?: item.thumbnailUrl
    var state by remember(model) { mutableStateOf(if (model == null) ImageState.FAILED else ImageState.LOADING) }
    val pending = item.previewState == PreviewState.PENDING && model == null
    Box(modifier) {
        when {
            pending || state == ImageState.LOADING -> Box(Modifier.fillMaxSize().shimmer())
            state == ImageState.FAILED -> FallbackArt(item)
            else -> Unit
        }
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = item.title ?: item.caption,
                contentScale = ContentScale.Crop,
                // YouTube's hqdefault.jpg is 4:3 with black bars baked in above and below; zoom past them.
                modifier = Modifier.fillMaxSize().then(
                    if (item.platform == Platform.YOUTUBE) Modifier.graphicsLayer { scaleX = 1.34f; scaleY = 1.34f } else Modifier,
                ),
                onSuccess = { state = ImageState.SHOWN },
                onError = { error ->
                    state = ImageState.FAILED
                    val code = (error.result.throwable as? HttpException)?.response?.code
                    if (local == null && (code == 403 || code == 404)) onGone()
                },
            )
        }
        overlay(state == ImageState.SHOWN)
    }
}

/** A small pill with the platform name in its colors. */
@Composable
fun PlatformBadge(item: SavedItem, modifier: Modifier = Modifier) {
    Text(
        item.platform.displayName,
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(item.platform.style.colors))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun StatusIcons(item: SavedItem, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Box(modifier) {
        when {
            item.isPinned -> Icon(Icons.Filled.PushPin, "Pinned", tint = Color.White, modifier = Modifier.size(size))
            item.isFavorite -> Icon(Icons.Filled.Star, "Favorite", tint = Color(0xFFFFC94D), modifier = Modifier.size(size))
        }
    }
}

/** Platform gradient and monogram, for items without a usable thumbnail. */
@Composable
private fun FallbackArt(item: SavedItem) {
    val style = item.platform.style
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(style.colors))) {
        Text(
            style.monogram,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun FallbackText(item: SavedItem, modifier: Modifier = Modifier) {
    Column(modifier.padding(12.dp)) {
        (item.title ?: item.caption)?.let {
            Text(it, color = Color.White, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text(
            displayUrl(item.url),
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** "tiktok.com/@a/video/1" instead of the full URL. */
fun displayUrl(url: String): String = url.toHttpUrlOrNull()?.let { it.host.removePrefix("www.") + it.encodedPath } ?: url
