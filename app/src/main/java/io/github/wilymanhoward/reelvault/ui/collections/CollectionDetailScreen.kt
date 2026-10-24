package io.github.wilymanhoward.reelvault.ui.collections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.wilymanhoward.reelvault.data.settings.SettingsRepository
import io.github.wilymanhoward.reelvault.domain.model.SavedItem
import io.github.wilymanhoward.reelvault.domain.model.VaultCollection
import io.github.wilymanhoward.reelvault.domain.model.ViewMode
import io.github.wilymanhoward.reelvault.domain.repository.CollectionRepository
import io.github.wilymanhoward.reelvault.ui.common.BackButton
import io.github.wilymanhoward.reelvault.ui.common.BatchActions
import io.github.wilymanhoward.reelvault.ui.common.CollectionEditDialog
import io.github.wilymanhoward.reelvault.ui.common.ConfirmDialog
import io.github.wilymanhoward.reelvault.ui.common.SavesEvent
import io.github.wilymanhoward.reelvault.ui.common.SavesScreen
import io.github.wilymanhoward.reelvault.ui.common.SavesViewModel
import io.github.wilymanhoward.reelvault.ui.navigation.CollectionDetailRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One collection's saves, plus editing the collection itself. */
@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CollectionRepository,
    settings: SettingsRepository,
    batch: BatchActions,
) : SavesViewModel(batch, repo) {
    private val collectionId = savedStateHandle.toRoute<CollectionDetailRoute>().collectionId

    val collection: StateFlow<VaultCollection?> = repo.observeCollection(collectionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val items: StateFlow<List<SavedItem>> = repo.observeItems(collectionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val viewMode: StateFlow<ViewMode> = settings.settings.map { it.viewMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ViewMode.GRID)

    fun update(name: String, emoji: String?, color: Long?) = viewModelScope.launch { repo.update(collectionId, name, emoji, color) }
    fun deleteCollection() = viewModelScope.launch { repo.delete(collectionId) }

    fun setCover(itemId: String) = viewModelScope.launch {
        repo.setCover(collectionId, itemId)
        send(SavesEvent.Message("Cover updated"))
    }

    fun removeFromCollection(ids: Set<String>) = viewModelScope.launch {
        ids.forEach { repo.setMembership(it, collectionId, member = false) }
        send(SavesEvent.Message("Removed ${ids.size} from this collection"))
    }
}

/** A collection's saves. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    viewModel: CollectionDetailViewModel = hiltViewModel(),
) {
    val collection by viewModel.collection.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    SavesScreen(
        viewModel = viewModel,
        items = items,
        viewMode = viewMode,
        onOpenItem = onOpenItem,
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onBack) },
                title = { Text(listOfNotNull(collection?.emoji, collection?.name).joinToString(" ")) },
                actions = {
                    IconButton(onClick = { editing = true }) { Icon(Icons.Outlined.Edit, "Edit collection") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Delete collection") }
                },
            )
        },
        emptyContent = {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "This collection is empty.\nAdd saves from the share sheet, or select saves and tap “Add to collection”.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        extraSelectionActions = { selected, clear ->
            if (selected.size == 1) {
                IconButton(onClick = { viewModel.setCover(selected.first()); clear() }) { Icon(Icons.Outlined.Image, "Set as cover") }
            }
            IconButton(onClick = { viewModel.removeFromCollection(selected); clear() }) {
                Icon(Icons.Outlined.RemoveCircleOutline, "Remove from collection")
            }
        },
    )

    if (editing) {
        CollectionEditDialog(
            title = "Edit collection",
            initial = collection,
            onSave = { name, emoji, color -> viewModel.update(name, emoji, color); editing = false },
            onDismiss = { editing = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete “${collection?.name.orEmpty()}”?",
            body = "The saves stay in your vault; only the collection goes.",
            confirm = "Delete",
            onConfirm = { viewModel.deleteCollection(); confirmDelete = false; onBack() },
            onDismiss = { confirmDelete = false },
        )
    }
}
