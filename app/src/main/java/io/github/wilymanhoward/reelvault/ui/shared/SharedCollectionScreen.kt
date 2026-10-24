package io.github.wilymanhoward.reelvault.ui.shared

import io.github.wilymanhoward.reelvault.ui.common.VaultIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import io.github.wilymanhoward.reelvault.ui.common.VaultButton as Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import io.github.wilymanhoward.reelvault.ui.common.VaultSnackbarHost as SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import io.github.wilymanhoward.reelvault.ui.common.VaultTextButton as TextButton
import io.github.wilymanhoward.reelvault.ui.common.VaultTopAppBar as TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.wilymanhoward.reelvault.data.share.ShareRepository
import io.github.wilymanhoward.reelvault.data.share.SharedCollection
import io.github.wilymanhoward.reelvault.data.share.SharedItem
import io.github.wilymanhoward.reelvault.domain.model.PreviewState
import io.github.wilymanhoward.reelvault.domain.model.SavedItem
import io.github.wilymanhoward.reelvault.domain.repository.CollectionRepository
import io.github.wilymanhoward.reelvault.domain.usecase.SaveLinkUseCase
import io.github.wilymanhoward.reelvault.domain.usecase.SaveResult
import io.github.wilymanhoward.reelvault.ui.common.BackButton
import io.github.wilymanhoward.reelvault.ui.common.COLLECTION_COLORS
import io.github.wilymanhoward.reelvault.ui.common.PreviewCard
import io.github.wilymanhoward.reelvault.ui.common.SaveActions
import io.github.wilymanhoward.reelvault.ui.common.openVideo
import io.github.wilymanhoward.reelvault.ui.navigation.SharedCollectionRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the shared-collection screen shows. */
sealed interface SharedState {
    data object Loading : SharedState
    data object NotFound : SharedState
    data class Failed(val message: String) : SharedState
    data class Loaded(val collection: SharedCollection, val adding: Boolean = false, val added: Boolean = false) : SharedState
}

/** Loads a shared snapshot from its link and copies it into this vault on request. */
@HiltViewModel
class SharedCollectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sharing: ShareRepository,
    private val saveLink: SaveLinkUseCase,
    private val collections: CollectionRepository,
) : ViewModel() {
    private val shareId = savedStateHandle.toRoute<SharedCollectionRoute>().shareId

    private val _state = MutableStateFlow<SharedState>(SharedState.Loading)
    val state: StateFlow<SharedState> = _state.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        load()
    }

    fun load() = viewModelScope.launch {
        _state.value = SharedState.Loading
        _state.value = try {
            if (!sharing.isAvailable) SharedState.Failed("Shared collections need a build with Firebase set up.")
            else sharing.fetch(shareId)?.let { SharedState.Loaded(it) } ?: SharedState.NotFound
        } catch (e: Exception) {
            SharedState.Failed("Couldn't load this collection. Check your connection and try again.")
        }
    }

    /** Saves every video (skipping ones already saved) into a new collection with the same name. */
    fun addToVault() = viewModelScope.launch {
        val loaded = _state.value as? SharedState.Loaded ?: return@launch
        if (loaded.adding || loaded.added) return@launch
        _state.value = loaded.copy(adding = true)
        try {
            val shared = loaded.collection
            var newCount = 0
            val ids = shared.items.mapNotNull { item ->
                when (val result = saveLink(item.url)) {
                    is SaveResult.Saved -> { newCount++; result.item.id }
                    is SaveResult.AlreadySaved -> result.item.id
                    SaveResult.NoLink -> null
                }
            }
            val collection = collections.create(shared.name, shared.emoji, shared.colorArgb)
            collections.addItems(ids, collection.id)
            _state.value = loaded.copy(adding = false, added = true)
            _messages.send(
                if (newCount == ids.size) "Added ${ids.size} videos to “${shared.name}”"
                else "Added “${shared.name}”: $newCount new, ${ids.size - newCount} you already had",
            )
        } catch (e: Exception) {
            _state.value = loaded.copy(adding = false)
            _messages.send("Couldn't add the collection: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }
}

/** A collection someone shared, opened from its link. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedCollectionScreen(onBack: () -> Unit, viewModel: SharedCollectionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text("Shared collection") }, navigationIcon = { BackButton(onBack) }) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                SharedState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                SharedState.NotFound -> Message("This collection isn't shared any more.", Modifier.align(Alignment.Center))
                is SharedState.Failed -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Message(s.message)
                    TextButton(onClick = { viewModel.load() }) { Text("Try again") }
                }
                is SharedState.Loaded -> {
                    val actions = remember {
                        SaveActions(
                            onClick = { openVideo(context, it.url, it.platform) },
                            onLongClick = {},
                            onDetails = {},
                            onThumbnailGone = {},
                        )
                    }
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(160.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing = 12.dp,
                    ) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Header(s, onAdd = viewModel::addToVault)
                        }
                        items(s.collection.items, key = { it.url }) { item ->
                            PreviewCard(item.asSavedItem(), selected = false, actions = actions, showDetails = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(state: SharedState.Loaded, onAdd: () -> Unit) {
    val c = state.collection
    val color = Color(c.colorArgb ?: COLLECTION_COLORS.first())
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(color).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        c.emoji?.let { Text(it, style = MaterialTheme.typography.displaySmall) }
        Text(c.name, style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Text(
            if (c.items.size == 1) "1 video" else "${c.items.size} videos",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
        )
        Button(onClick = onAdd, enabled = !state.adding && !state.added) {
            if (state.adding) CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            else Icon(VaultIcons.Import, null)
            Text(if (state.added) "  In your vault" else "  Add to my vault")
        }
    }
}

@Composable
private fun Message(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(32.dp),
    )
}

/** Shows a shared video with the regular card. Its URL doubles as id; it isn't stored. */
private fun SharedItem.asSavedItem() = SavedItem(
    id = url,
    url = url,
    normalizedUrl = url,
    platform = platform,
    title = title,
    caption = caption,
    authorName = authorName,
    thumbnailUrl = thumbnailUrl,
    localThumbnailPath = null,
    notes = "",
    previewState = PreviewState.READY,
    isFavorite = false,
    isPinned = false,
    createdAt = 0,
)
