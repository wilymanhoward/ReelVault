package io.github.wilymanhoward.reelvault.ui.collections

import io.github.wilymanhoward.reelvault.ui.common.VaultIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import io.github.wilymanhoward.reelvault.ui.common.VaultLargeTopAppBar as LargeTopAppBar
import io.github.wilymanhoward.reelvault.ui.common.VaultTextButton as TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import io.github.wilymanhoward.reelvault.ui.common.VaultIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.wilymanhoward.reelvault.domain.model.VaultCollection
import io.github.wilymanhoward.reelvault.domain.repository.CollectionRepository
import io.github.wilymanhoward.reelvault.ui.common.COLLECTION_COLORS
import io.github.wilymanhoward.reelvault.ui.common.CollectionEditDialog
import io.github.wilymanhoward.reelvault.ui.common.CollectionMark
import io.github.wilymanhoward.reelvault.ui.common.EmptyState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** Collections list: create and reorder. */
@HiltViewModel
class CollectionsViewModel @Inject constructor(private val repo: CollectionRepository) : ViewModel() {
    val collections: StateFlow<List<VaultCollection>> = repo.observeCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String, emoji: String?, color: Long?) = viewModelScope.launch { repo.create(name, emoji, color) }

    /** Moves [id] one step up (-1) or down (+1). */
    fun move(id: String, delta: Int) = viewModelScope.launch {
        val ids = collections.value.map { it.id }.toMutableList()
        val from = ids.indexOf(id)
        val to = from + delta
        if (from < 0 || to !in ids.indices) return@launch
        ids.add(to, ids.removeAt(from))
        repo.reorder(ids)
    }
}

/** The Collections tab. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(onOpenCollection: (String) -> Unit, viewModel: CollectionsViewModel = hiltViewModel()) {
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    var reordering by rememberSaveable { mutableStateOf(false) }
    var creating by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTopAppBar(
                title = { Text("Collections") },
                actions = {
                    TextButton(onClick = { creating = true }) { Text("New") }
                    if (collections.size > 1) {
                        IconButton(onClick = { reordering = !reordering }) {
                            Icon(if (reordering) VaultIcons.Check else VaultIcons.Swap, if (reordering) "Done reordering" else "Reorder")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (collections.isEmpty()) {
            EmptyState(
                title = "No collections yet",
                body = "Recipes, places, rabbit holes. Give your saves a collection of their own.",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(collections, key = { _, c -> c.id }) { index, c ->
                    CollectionRow(
                        collection = c,
                        reordering = reordering,
                        canMoveUp = index > 0,
                        canMoveDown = index < collections.lastIndex,
                        onClick = { if (!reordering) onOpenCollection(c.id) },
                        onMove = { viewModel.move(c.id, it) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    if (creating) {
        CollectionEditDialog(
            title = "New collection",
            initial = null,
            onSave = { name, emoji, color -> viewModel.create(name, emoji, color); creating = false },
            onDismiss = { creating = false },
        )
    }
}

@Composable
private fun CollectionRow(
    collection: VaultCollection,
    reordering: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f)) {
            CollectionCover(collection, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent, Color.Black.copy(alpha = 0.65f)))))
            Column(Modifier.padding(14.dp)) {
                Text(collection.name, color = Color.White, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    if (collection.itemCount == 1) "1 save" else "${collection.itemCount} saves",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
            if (!reordering) {
                Icon(VaultIcons.ChevronRight, null, tint = Color.White, modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp).size(22.dp))
            }
            if (reordering) {
                Row(Modifier.align(Alignment.BottomEnd).padding(4.dp)) {
                    IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) { Icon(VaultIcons.ChevronUp, "Move up") }
                    IconButton(onClick = { onMove(1) }, enabled = canMoveDown) { Icon(VaultIcons.ChevronDown, "Move down") }
                }
            }
        }
    }
}

/** The collection's cover thumbnail, or its emoji on its color when it has none. */
@Composable
fun CollectionCover(collection: VaultCollection, modifier: Modifier = Modifier) {
    val color = Color(collection.colorArgb ?: COLLECTION_COLORS.first())
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
        CollectionMark(collection, size = 44.dp)
        collection.coverImage?.let { image ->
            AsyncImage(
                model = if (image.startsWith("/")) File(image) else image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                // Same crop as the cards: zoom past bars baked into YouTube and TikTok share-card images.
                modifier = Modifier.fillMaxSize().then(
                    if (collection.coverHasBars) Modifier.graphicsLayer { scaleX = 1.34f; scaleY = 1.34f } else Modifier,
                ),
            )
        }
    }
}

@Composable
fun CollectionHero(collection: VaultCollection) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp).height(if (collection.coverImage == null) 156.dp else 220.dp).clip(RoundedCornerShape(24.dp))) {
        CollectionCover(collection, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Text(collection.name, color = Color.White, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(if (collection.itemCount == 1) "1 saved video" else "${collection.itemCount} saved videos", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
        }
    }
}
