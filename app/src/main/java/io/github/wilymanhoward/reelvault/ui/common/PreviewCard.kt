package io.github.wilymanhoward.reelvault.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.network.HttpException
import io.github.wilymanhoward.reelvault.domain.model.PreviewState
import io.github.wilymanhoward.reelvault.domain.model.SavedItem
import io.github.wilymanhoward.reelvault.ui.theme.style
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File

private enum class ImageState { LOADING, SHOWN, FAILED }

/**
 * A saved item as a grid card: thumbnail with a gradient scrim, platform badge, title and author.
 * Without a usable image it shows the platform's gradient art instead, never a broken image.
 *
 * @param onThumbnailGone called when the remote thumbnail answers 403/404 (expired signed URL).
 */
@Composable
fun PreviewCard(item: SavedItem, onClick: () -> Unit, onThumbnailGone: () -> Unit, modifier: Modifier = Modifier) {
    val style = item.platform.style
    val local = remember(item.localThumbnailPath) { item.localThumbnailPath?.let(::File)?.takeIf { it.exists() } }
    val model: Any? = local ?: item.thumbnailUrl
    var imageState by remember(model) { mutableStateOf(if (model == null) ImageState.FAILED else ImageState.LOADING) }
    val pending = item.previewState == PreviewState.PENDING && model == null
    val text = item.title ?: item.caption

    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(style.aspectRatio)) {
            when {
                pending || imageState == ImageState.LOADING -> Box(Modifier.fillMaxSize().shimmer())
                imageState == ImageState.FAILED -> FallbackArt(item)
                else -> Unit
            }
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = text,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onSuccess = { imageState = ImageState.SHOWN },
                    onError = { state ->
                        imageState = ImageState.FAILED
                        val code = (state.result.throwable as? HttpException)?.response?.code
                        if (local == null && (code == 403 || code == 404)) onThumbnailGone()
                    },
                )
            }
            if (imageState == ImageState.SHOWN) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.72f)),
                    ),
                )
            }
            PlatformBadge(item, Modifier.align(Alignment.TopStart).padding(10.dp))
            if (imageState == ImageState.SHOWN && (text != null || item.authorName != null)) {
                Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                    text?.let {
                        Text(it, color = Color.White, style = MaterialTheme.typography.titleSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                    item.authorName?.let {
                        Text(it, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }
        }
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

/** Platform gradient, monogram and whatever text we have, for items without a usable thumbnail. */
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
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            (item.title ?: item.caption)?.let {
                Text(it, color = Color.White, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(
                item.url.toHttpUrlOrNull()?.let { it.host.removePrefix("www.") + it.encodedPath } ?: item.url,
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
