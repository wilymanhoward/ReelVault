package io.github.wilymanhoward.reelvault.ui.item

import io.github.wilymanhoward.reelvault.ui.common.VaultIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import io.github.wilymanhoward.reelvault.ui.common.VaultAssistChip as AssistChip
import io.github.wilymanhoward.reelvault.ui.common.VaultButton as Button
import androidx.compose.material3.ExperimentalMaterial3Api
import io.github.wilymanhoward.reelvault.ui.common.VaultFilterChip as FilterChip
import androidx.compose.material3.Icon
import io.github.wilymanhoward.reelvault.ui.common.VaultIconButton as IconButton
import io.github.wilymanhoward.reelvault.ui.common.VaultInputChip as InputChip
import androidx.compose.material3.MaterialTheme
import io.github.wilymanhoward.reelvault.ui.common.VaultTextField as OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import io.github.wilymanhoward.reelvault.ui.common.VaultTextButton as TextButton
import io.github.wilymanhoward.reelvault.ui.common.VaultTopAppBar as TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.wilymanhoward.reelvault.domain.model.SavedItem
import io.github.wilymanhoward.reelvault.domain.model.Tag
import io.github.wilymanhoward.reelvault.domain.model.TagSource
import io.github.wilymanhoward.reelvault.domain.model.VaultCollection
import io.github.wilymanhoward.reelvault.domain.repository.CollectionRepository
import io.github.wilymanhoward.reelvault.domain.repository.ItemRepository
import io.github.wilymanhoward.reelvault.domain.repository.PreviewScheduler
import io.github.wilymanhoward.reelvault.domain.repository.TagRepository
import io.github.wilymanhoward.reelvault.ui.common.BackButton
import io.github.wilymanhoward.reelvault.ui.common.ConfirmDialog
import io.github.wilymanhoward.reelvault.ui.common.PlatformBadge
import io.github.wilymanhoward.reelvault.ui.common.TextInputDialog
import io.github.wilymanhoward.reelvault.ui.common.Thumbnail
import io.github.wilymanhoward.reelvault.ui.common.displayUrl
import io.github.wilymanhoward.reelvault.ui.common.openVideo
import io.github.wilymanhoward.reelvault.ui.common.shareVideo
import io.github.wilymanhoward.reelvault.ui.common.sharedAcrossScreens
import io.github.wilymanhoward.reelvault.ui.common.thumbnailKey
import io.github.wilymanhoward.reelvault.ui.navigation.ItemDetailRoute
import io.github.wilymanhoward.reelvault.ui.theme.style
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** One save: its metadata, notes, tags, collections, favorite/pin. */
@OptIn(FlowPreview::class)
@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val items: ItemRepository,
    private val tags: TagRepository,
    private val collections: CollectionRepository,
    private val previews: PreviewScheduler,
) : ViewModel() {
    private val itemId = savedStateHandle.toRoute<ItemDetailRoute>().itemId

    val item: StateFlow<SavedItem?> = items.observeItem(itemId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val itemTags: StateFlow<List<Tag>> = tags.observeTags(itemId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allCollections: StateFlow<List<VaultCollection>> = collections.observeCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val memberOf: StateFlow<Set<String>> = collections.observeCollectionIdsFor(itemId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val notesDraft = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch { notesDraft.filterNotNull().debounce(400).collect { items.setNotes(itemId, it) } }
    }

    fun onNotesChange(text: String) {
        notesDraft.value = text
    }

    /** Saves a pending notes edit right away (the screen is closing). */
    fun flushNotes() {
        val pending = notesDraft.value ?: return
        if (pending != item.value?.notes) viewModelScope.launch { items.setNotes(itemId, pending) }
    }

    fun toggleFavorite() = item.value?.let { viewModelScope.launch { items.setFavorite(itemId, !it.isFavorite) } }
    fun togglePinned() = item.value?.let { viewModelScope.launch { items.setPinned(itemId, !it.isPinned) } }
    fun delete() = viewModelScope.launch { items.delete(listOf(itemId)) }
    fun addTag(name: String) = viewModelScope.launch { tags.addTag(listOf(itemId), name) }
    fun removeTag(name: String) = viewModelScope.launch { tags.removeTag(itemId, name) }
    fun refreshPreview() = previews.schedule(itemId)

    fun toggleCollection(collectionId: String) = viewModelScope.launch {
        collections.setMembership(itemId, collectionId, collectionId !in memberOf.value)
    }

    fun createCollection(name: String) = viewModelScope.launch {
        collections.setMembership(itemId, collections.create(name).id, member = true)
    }
}

private enum class DetailDialog { NONE, TAG, COLLECTION, DELETE }

/** Details of one save. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemDetailScreen(onBack: () -> Unit, viewModel: ItemDetailViewModel = hiltViewModel()) {
    val item by viewModel.item.collectAsStateWithLifecycle()
    val tags by viewModel.itemTags.collectAsStateWithLifecycle()
    val collections by viewModel.allCollections.collectAsStateWithLifecycle()
    val memberOf by viewModel.memberOf.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var dialog by rememberSaveable { mutableStateOf(DetailDialog.NONE) }
    var notes by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(item?.notes) { if (notes == null) item?.let { notes = it.notes } }
    DisposableEffect(Unit) { onDispose { viewModel.flushNotes() } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onBack) },
                title = {},
                actions = {
                    val current = item ?: return@TopAppBar
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(if (current.isFavorite) VaultIcons.Star else VaultIcons.Star, if (current.isFavorite) "Unfavorite" else "Favorite")
                    }
                    IconButton(onClick = { viewModel.togglePinned() }) {
                        Icon(if (current.isPinned) VaultIcons.Pin else VaultIcons.Pin, if (current.isPinned) "Unpin" else "Pin")
                    }
                    IconButton(onClick = { shareVideo(context, current.url, current.platform) }) { Icon(VaultIcons.Share, "Share") }
                    IconButton(onClick = { dialog = DetailDialog.DELETE }) { Icon(VaultIcons.Trash, "Delete") }
                },
            )
        },
    ) { padding ->
        val current = item ?: return@Scaffold
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Thumbnail(
                current,
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(current.platform.style.aspectRatio.coerceAtLeast(0.8f))
                    .sharedAcrossScreens(thumbnailKey(current.id))
                    .clip(RoundedCornerShape(24.dp)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PlatformBadge(current)
                (current.title ?: current.caption)?.let { Text(it, style = MaterialTheme.typography.titleLarge) }
                if (current.title != null && current.caption != null && current.caption != current.title) {
                    Text(current.caption, style = MaterialTheme.typography.bodyMedium)
                }
                current.authorName?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text(displayUrl(current.url), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { openVideo(context, current.url, current.platform) }, modifier = Modifier.fillMaxWidth()) {
                Icon(VaultIcons.External, null, Modifier.size(18.dp))
                Text("Open in ${current.platform.displayName}", Modifier.padding(start = 8.dp))
            }

            OutlinedTextField(
                value = notes.orEmpty(),
                onValueChange = { notes = it; viewModel.onNotesChange(it) },
                label = { Text("Notes") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Section("Tags") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.forEach { tag ->
                        // Auto tags: outlined with a sparkle. Manual tags: filled input chips.
                        if (tag.source == TagSource.AUTO) {
                            AssistChip(
                                onClick = { viewModel.removeTag(tag.name) },
                                label = { Text(tag.name) },
                                leadingIcon = { Icon(VaultIcons.AutoTag, "Auto tag", Modifier.size(16.dp)) },
                                trailingIcon = { Icon(VaultIcons.Close, "Remove ${tag.name}", Modifier.size(16.dp)) },
                            )
                        } else {
                            InputChip(
                                selected = true,
                                onClick = { viewModel.removeTag(tag.name) },
                                label = { Text(tag.name) },
                                trailingIcon = { Icon(VaultIcons.Close, "Remove ${tag.name}", Modifier.size(16.dp)) },
                            )
                        }
                    }
                    AssistChip(
                        onClick = { dialog = DetailDialog.TAG },
                        label = { Text("Add tag") },
                        leadingIcon = { Icon(VaultIcons.Plus, null, Modifier.size(16.dp)) },
                    )
                }
            }

            Section("Collections") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    collections.forEach { c ->
                        val selected = c.id in memberOf
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.toggleCollection(c.id) },
                            label = { Text(listOfNotNull(c.emoji, c.name).joinToString(" ")) },
                            leadingIcon = if (selected) ({ Icon(VaultIcons.Check, null, Modifier.size(16.dp)) }) else null,
                        )
                    }
                    AssistChip(
                        onClick = { dialog = DetailDialog.COLLECTION },
                        label = { Text("New") },
                        leadingIcon = { Icon(VaultIcons.Plus, null, Modifier.size(16.dp)) },
                    )
                }
            }

            TextButton(onClick = { viewModel.refreshPreview() }) {
                Icon(VaultIcons.Refresh, null, Modifier.size(18.dp))
                Text("Refresh preview", Modifier.padding(start = 8.dp))
            }
        }
    }

    when (dialog) {
        DetailDialog.TAG -> TextInputDialog(
            title = "Add tag",
            label = "Tag",
            confirm = "Add",
            onConfirm = { viewModel.addTag(it); dialog = DetailDialog.NONE },
            onDismiss = { dialog = DetailDialog.NONE },
        )
        DetailDialog.COLLECTION -> TextInputDialog(
            title = "New collection",
            label = "Name",
            confirm = "Create",
            onConfirm = { viewModel.createCollection(it); dialog = DetailDialog.NONE },
            onDismiss = { dialog = DetailDialog.NONE },
        )
        DetailDialog.DELETE -> ConfirmDialog(
            title = "Delete this save?",
            body = "It's removed from your vault and all collections.",
            confirm = "Delete",
            onConfirm = { viewModel.delete(); dialog = DetailDialog.NONE; onBack() },
            onDismiss = { dialog = DetailDialog.NONE },
        )
        DetailDialog.NONE -> Unit
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        content()
    }
}
