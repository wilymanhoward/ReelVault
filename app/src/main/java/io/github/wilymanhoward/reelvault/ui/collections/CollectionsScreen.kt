package io.github.wilymanhoward.reelvault.ui.collections

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
            TopAppBar(
                title = { Text("Collections") },
                actions = {
                    if (collections.size > 1) {
                        IconButton(onClick = { reordering = !reordering }) {
                            Icon(if (reordering) Icons.Filled.Check else Icons.Outlined.SwapVert, if (reordering) "Done reordering" else "Reorder")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!reordering) {
                ExtendedFloatingActionButton(
                    onClick = { creating = true },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("New collection") },
                )
            }
        },
    ) { padding ->
        if (collections.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No collections yet.\nGroup saves by topic, trip or mood.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
    Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CollectionCover(collection, Modifier.size(88.dp))
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(collection.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (collection.itemCount == 1) "1 save" else "${collection.itemCount} saves",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (reordering) {
                IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) { Icon(Icons.Outlined.KeyboardArrowUp, "Move up") }
                IconButton(onClick = { onMove(1) }, enabled = canMoveDown) { Icon(Icons.Outlined.KeyboardArrowDown, "Move down") }
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
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
