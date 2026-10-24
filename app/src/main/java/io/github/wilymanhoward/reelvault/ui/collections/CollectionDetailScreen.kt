package io.github.wilymanhoward.reelvault.ui.collections

import io.github.wilymanhoward.reelvault.ui.common.VaultIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import io.github.wilymanhoward.reelvault.ui.common.VaultAlertDialog as AlertDialog
import io.github.wilymanhoward.reelvault.ui.common.VaultButton as Button
import androidx.compose.material3.LinearProgressIndicator
import io.github.wilymanhoward.reelvault.ui.common.VaultOutlinedButton as OutlinedButton
import io.github.wilymanhoward.reelvault.ui.common.VaultTextButton as TextButton
import androidx.compose.ui.platform.LocalContext
import io.github.wilymanhoward.reelvault.data.share.NotSignedInException
import io.github.wilymanhoward.reelvault.data.share.ShareRepository
import io.github.wilymanhoward.reelvault.ui.common.copyText
import io.github.wilymanhoward.reelvault.ui.common.shareText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import io.github.wilymanhoward.reelvault.ui.common.VaultIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import io.github.wilymanhoward.reelvault.ui.common.VaultTopAppBar as TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import io.github.wilymanhoward.reelvault.ui.common.EmptyState
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

/** Where publishing the collection's share link stands. */
sealed interface ShareState {
    data object Idle : ShareState
    data object Working : ShareState
    data class Shared(val link: String) : ShareState
    data class Failed(val message: String) : ShareState
}

/** One collection's saves, plus editing and sharing the collection itself. */
@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CollectionRepository,
    private val sharing: ShareRepository,
    settings: SettingsRepository,
    batch: BatchActions,
) : SavesViewModel(batch, repo) {
    private val _share = MutableStateFlow<ShareState>(ShareState.Idle)
    val share: StateFlow<ShareState> = _share.asStateFlow()

    /** Publishes (or refreshes) the public snapshot and shows its link. */
    fun publishShare() = viewModelScope.launch {
        _share.value = ShareState.Working
        _share.value = try {
            ShareState.Shared(sharing.publish(collectionId))
        } catch (e: NotSignedInException) {
            ShareState.Failed(e.message.orEmpty())
        } catch (e: Exception) {
            ShareState.Failed("Couldn't publish the link: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    fun stopSharing() = viewModelScope.launch {
        _share.value = ShareState.Working
        _share.value = try {
            sharing.stopSharing(collectionId)
            send(SavesEvent.Message("Link turned off"))
            ShareState.Idle
        } catch (e: Exception) {
            ShareState.Failed("Couldn't turn off the link: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    fun dismissShare() {
        _share.value = ShareState.Idle
    }

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
    val share by viewModel.share.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
                title = { Text("Collection", style = MaterialTheme.typography.bodyMedium) },
                actions = {
                    IconButton(onClick = viewModel::publishShare) {
                        Icon(if (collection?.shareId != null) VaultIcons.Link else VaultIcons.Share, "Share collection")
                    }
                    IconButton(onClick = { editing = true }) { Icon(VaultIcons.Edit, "Edit collection") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(VaultIcons.Trash, "Delete collection") }
                },
            )
        },
        header = { collection?.let { CollectionHero(it) } },
        emptyContent = {
            EmptyState(
                title = "Nothing in here yet",
                body = "Pick this collection when you save from the share sheet, or long-press saves anywhere and tap Add to collection.",
            )
        },
        extraSelectionActions = { selected, clear ->
            if (selected.size == 1) {
                IconButton(onClick = { viewModel.setCover(selected.first()); clear() }) { Icon(VaultIcons.Image, "Set as cover") }
            }
            IconButton(onClick = { viewModel.removeFromCollection(selected); clear() }) {
                Icon(VaultIcons.MinusCircle, "Remove from collection")
            }
        },
    )

    when (val s = share) {
        ShareState.Idle -> Unit
        ShareState.Working -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Publishing link…") },
            text = { LinearProgressIndicator(Modifier.fillMaxWidth()) },
            confirmButton = {},
        )
        is ShareState.Shared -> AlertDialog(
            onDismissRequest = viewModel::dismissShare,
            title = { Text("Share “${collection?.name.orEmpty()}”") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Anyone with this link can see the videos in this collection. It shows them as they are now; share again after changes to update it.")
                    Text(s.link, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { copyText(context, "ReelVault link", s.link) }) { Text("Copy link") }
                        Button(onClick = { shareText(context, "${collection?.name.orEmpty()}, a ReelVault collection\n${s.link}") }) { Text("Share link") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::dismissShare) { Text("Done") } },
            dismissButton = {
                TextButton(onClick = viewModel::stopSharing) { Text("Stop sharing", color = MaterialTheme.colorScheme.error) }
            },
        )
        is ShareState.Failed -> AlertDialog(
            onDismissRequest = viewModel::dismissShare,
            title = { Text("Couldn't share") },
            text = { Text(s.message) },
            confirmButton = { TextButton(onClick = viewModel::dismissShare) { Text("OK") } },
        )
    }

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
