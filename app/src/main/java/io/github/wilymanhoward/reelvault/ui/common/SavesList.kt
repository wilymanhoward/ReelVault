package io.github.wilymanhoward.reelvault.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.wilymanhoward.reelvault.domain.model.SavedItem
import io.github.wilymanhoward.reelvault.domain.model.ViewMode

/**
 * Saves as a staggered grid of cards or a compact list. Swiping an item away deletes it
 * (the caller shows an Undo snackbar); long-press starts multi-select.
 */
@Composable
fun SavesList(
    items: List<SavedItem>,
    viewMode: ViewMode,
    selected: Set<String>,
    actions: SaveActions,
    onDelete: (SavedItem) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
) {
    // Swiping is off while selecting, so a stray swipe can't delete during a batch action.
    val swipeEnabled = selected.isEmpty()
    // The first screenful staggers in once per screen; later inserts use animateItem instead.
    var firstLoad by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1_200)
        firstLoad = false
    }
    AnimatedContent(targetState = viewMode, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "viewMode") { mode ->
        when (mode) {
            ViewMode.GRID -> LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(160.dp),
                modifier = modifier,
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalItemSpacing = 12.dp,
            ) {
                itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                    SwipeToDelete(
                        swipeEnabled,
                        onDelete = { onDelete(item) },
                        modifier = Modifier.animateItem().staggeredEntrance(index, firstLoad),
                    ) {
                        PreviewCard(item, selected = item.id in selected, actions = actions)
                    }
                }
            }
            ViewMode.LIST -> LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding())) {
                itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                    SwipeToDelete(
                        swipeEnabled,
                        onDelete = { onDelete(item) },
                        modifier = Modifier.animateItem().staggeredEntrance(index, firstLoad),
                    ) {
                        SaveRow(item, selected = item.id in selected, actions = actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeToDelete(enabled: Boolean, onDelete: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) onDelete()
            it == SwipeToDismissBoxValue.EndToStart
        },
    )
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = enabled,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
        content = { content() },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SaveRow(item: SavedItem, selected: Boolean, actions: SaveActions) {
    ListItem(
        modifier = Modifier.combinedClickable(onClick = { actions.onClick(item) }, onLongClick = { actions.onLongClick(item) }),
        colors = ListItemDefaults.colors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        leadingContent = {
            Box(Modifier.size(56.dp).sharedAcrossScreens(thumbnailKey(item.id)).clip(RoundedCornerShape(12.dp))) {
                Thumbnail(item, Modifier.fillMaxSize(), onGone = { actions.onThumbnailGone(item) })
                if (selected) Icon(Icons.Filled.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center))
            }
        },
        overlineContent = { Text(item.platform.displayName) },
        headlineContent = { Text(item.title ?: item.caption ?: displayUrl(item.url), maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = item.authorName?.let { { Text(it, maxLines = 1) } },
        trailingContent = {
            IconButton(onClick = { actions.onDetails(item) }) { Icon(Icons.Outlined.MoreHoriz, contentDescription = "Details") }
        },
    )
}
