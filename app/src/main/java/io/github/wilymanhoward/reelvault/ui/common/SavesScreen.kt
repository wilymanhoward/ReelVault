package io.github.wilymanhoward.reelvault.ui.common

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.wilymanhoward.reelvault.domain.model.SavedItem
import io.github.wilymanhoward.reelvault.domain.model.VaultCollection
import io.github.wilymanhoward.reelvault.domain.model.ViewMode
import io.github.wilymanhoward.reelvault.domain.repository.CollectionRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One-off UI events from a saves screen. */
sealed interface SavesEvent {
    data class Deleted(val ids: Set<String>) : SavesEvent
    data class Message(val text: String) : SavesEvent
}

/** Base for screens that show saves with selection and batch actions. */
abstract class SavesViewModel(
    private val batch: BatchActions,
    collectionRepository: CollectionRepository,
) : ViewModel() {

    val collections: StateFlow<List<VaultCollection>> = collectionRepository.observeCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = Channel<SavesEvent>(Channel.BUFFERED)
    val events: Flow<SavesEvent> = _events.receiveAsFlow()

    protected fun send(event: SavesEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    fun delete(ids: Set<String>) = viewModelScope.launch {
        batch.delete(ids)
        _events.send(SavesEvent.Deleted(ids))
    }

    fun restore(ids: Set<String>) = viewModelScope.launch { batch.restore(ids) }

    fun addTag(ids: Set<String>, name: String) = viewModelScope.launch {
        batch.addTag(ids, name)
        _events.send(SavesEvent.Message("Tagged ${ids.size} with “$name”"))
    }

    fun addToCollection(ids: Set<String>, collection: VaultCollection) = viewModelScope.launch {
        batch.addToCollection(ids, collection.id)
        _events.send(SavesEvent.Message("Added ${ids.size} to ${collection.name}"))
    }

    fun addToNewCollection(ids: Set<String>, name: String) = viewModelScope.launch {
        batch.addToNewCollection(ids, name)
        _events.send(SavesEvent.Message("Added ${ids.size} to $name"))
    }

    fun onThumbnailGone(itemId: String) = batch.onThumbnailGone(itemId)
}

private enum class BatchDialog { NONE, COLLECTION, TAG }

/**
 * Scaffold for a list of saves: normal [topBar] or the selection bar, the saves themselves,
 * delete with an Undo snackbar, and the batch dialogs. Tapping a save opens the video, or toggles
 * it while selecting.
 *
 * @param extraSelectionActions screen-specific batch actions, given the selection and a way to clear it.
 */
@Composable
fun SavesScreen(
    viewModel: SavesViewModel,
    items: List<SavedItem>,
    viewMode: ViewMode,
    onOpenItem: (String) -> Unit,
    topBar: @Composable () -> Unit,
    emptyContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    header: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    extraSelectionActions: @Composable (selected: Set<String>, clear: () -> Unit) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var dialog by rememberSaveable { mutableStateOf(BatchDialog.NONE) }
    // Drop selected ids that disappeared (deleted elsewhere, filtered out).
    LaunchedEffect(items) {
        val visible = items.mapTo(HashSet()) { it.id }
        if (!visible.containsAll(selected)) selected = selected intersect visible
    }
    val clear = { selected = emptySet() }

    BackHandler(enabled = selected.isNotEmpty(), onBack = clear)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SavesEvent.Message -> snackbar.showSnackbar(event.text)
                is SavesEvent.Deleted -> {
                    val label = if (event.ids.size == 1) "Deleted" else "Deleted ${event.ids.size} saves"
                    val result = snackbar.showSnackbar(label, actionLabel = "Undo", duration = SnackbarDuration.Long)
                    if (result == SnackbarResult.ActionPerformed) viewModel.restore(event.ids)
                }
            }
        }
    }

    val actions = remember(viewModel, selected) {
        SaveActions(
            onClick = { item ->
                if (selected.isNotEmpty()) selected = selected.toggle(item.id)
                else openVideo(context, item.url, item.platform)
            },
            onLongClick = { item ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                selected = selected.toggle(item.id)
            },
            onDetails = { onOpenItem(it.id) },
            onThumbnailGone = { viewModel.onThumbnailGone(it.id) },
        )
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (selected.isEmpty()) topBar()
            else SelectionTopBar(selected.size, onClear = clear) {
                extraSelectionActions(selected, clear)
                IconButton(onClick = { dialog = BatchDialog.COLLECTION }) { Icon(Icons.Outlined.DriveFileMove, "Add to collection") }
                IconButton(onClick = { dialog = BatchDialog.TAG }) { Icon(Icons.Outlined.Sell, "Tag") }
                IconButton(onClick = { viewModel.delete(selected); clear() }) { Icon(Icons.Outlined.Delete, "Delete") }
            }
        },
        floatingActionButton = { if (selected.isEmpty()) floatingActionButton() },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            header()
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize()) { emptyContent() }
            } else {
                SavesList(
                    items = items,
                    viewMode = viewMode,
                    selected = selected,
                    actions = actions,
                    onDelete = { viewModel.delete(setOf(it.id)) },
                )
            }
        }
    }

    when (dialog) {
        BatchDialog.COLLECTION -> CollectionPickerDialog(
            collections = collections,
            onPick = { viewModel.addToCollection(selected, it); clear(); dialog = BatchDialog.NONE },
            onCreate = { viewModel.addToNewCollection(selected, it); clear(); dialog = BatchDialog.NONE },
            onDismiss = { dialog = BatchDialog.NONE },
        )
        BatchDialog.TAG -> TextInputDialog(
            title = "Tag ${selected.size} saves",
            label = "Tag",
            confirm = "Add tag",
            onConfirm = { viewModel.addTag(selected, it); clear(); dialog = BatchDialog.NONE },
            onDismiss = { dialog = BatchDialog.NONE },
        )
        BatchDialog.NONE -> Unit
    }
}

private fun Set<String>.toggle(id: String) = if (id in this) this - id else this + id
