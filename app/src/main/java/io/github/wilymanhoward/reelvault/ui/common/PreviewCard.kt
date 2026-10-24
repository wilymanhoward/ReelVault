package io.github.wilymanhoward.reelvault.ui.common

import io.github.wilymanhoward.reelvault.ui.common.VaultIcons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import io.github.wilymanhoward.reelvault.ui.common.VaultIconButton as IconButton
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
 * A saved item as a grid card: thumbnail with a separate title, platform and author.
 * Without a usable image it shows the platform monogram instead, never a broken image.
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
    val text = item.headline()
    val shape = RoundedCornerShape(16.dp)
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
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    actions.onClick(item)
                },
                onLongClick = { actions.onLongClick(item) },
            ),
        shape = shape,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(16.dp))) {
            Thumbnail(
                item,
                Modifier.fillMaxSize().sharedAcrossScreens(thumbnailKey(item.id)),
                onGone = { actions.onThumbnailGone(item) },
            )
            StatusIcons(item, Modifier.align(Alignment.TopStart).padding(10.dp))
            if (showDetails) {
                IconButton(onClick = { actions.onDetails(item) }, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(VaultIcons.More, "Details", tint = Color.White,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.65f), CircleShape).padding(4.dp))
                }
            }
            SelectedMark(selected, Modifier.align(Alignment.Center))
        }
        Column(Modifier.padding(horizontal = 4.dp, vertical = 10.dp)) {
            Text(text, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                item.metaLine(),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
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
        // Mark on the action color, so it reads in both themes (17.6:1 dark, 15.7:1 light).
        Icon(
            VaultIcons.Check,
            contentDescription = "Selected",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary, CircleShape).padding(10.dp),
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
                // Some thumbnails carry bars baked in: YouTube's hqdefault.jpg is 4:3 with black bars above and
                // below, and TikTok's "share card" is a wide 1200x630 image with the portrait video in the middle
                // and blurred edges. The zoom crops both to the video itself, in 3:4 cards and the wider detail hero.
                modifier = Modifier.fillMaxSize().then(
                    item.barZoom()?.let { zoom -> Modifier.graphicsLayer { scaleX = zoom; scaleY = zoom } } ?: Modifier,
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
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/**
 * Pin and favorite marks over the thumbnail. White on a dark disc so they hold 4.5:1 or better over
 * any photo (worst case, a white image, still leaves #666 behind the glyph); shape, not color, tells them apart.
 */
@Composable
private fun StatusIcons(item: SavedItem, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    val marks = listOfNotNull(
        (VaultIcons.Pin to "Pinned").takeIf { item.isPinned },
        (VaultIcons.Star to "Favorite").takeIf { item.isFavorite },
    )
    if (marks.isEmpty()) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        marks.forEach { (icon, label) ->
            Icon(
                icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape).padding(6.dp).size(size),
            )
        }
    }
}

/** Neutral surface and platform monogram, for items without a usable thumbnail. */
@Composable
private fun FallbackArt(item: SavedItem) {
    val style = item.platform.style
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        Text(
            style.monogram,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/**
 * What a save is called on cards and rows: its title, else its caption, else its author (TikTok often
 * gives no caption), and only as a last resort its link.
 */
fun SavedItem.headline(): String = title ?: caption ?: authorName ?: displayUrl(url)

/** The line under the headline: the platform, plus the author unless the author already is the headline. */
fun SavedItem.metaLine(): String =
    listOfNotNull(platform.displayName, authorName.takeUnless { title == null && caption == null }).joinToString(" · ")

/** How far to zoom a thumbnail to crop bars baked into it, or null when it has none. */
private fun SavedItem.barZoom(): Float? = when {
    platform == Platform.YOUTUBE -> 1.34f
    // The share card's video is narrower than a YouTube frame, and the detail hero is wider than a card.
    platform == Platform.TIKTOK && thumbnailUrl?.contains("video-share-card") == true -> 1.45f
    else -> null
}

/** "tiktok.com/@a/video/1" instead of the full URL. */
fun displayUrl(url: String): String = url.toHttpUrlOrNull()?.let { it.host.removePrefix("www.") + it.encodedPath } ?: url
