package io.github.wilymanhoward.reelvault.ui.search

import io.github.wilymanhoward.reelvault.ui.common.VaultIcons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.statusBarsPadding
import io.github.wilymanhoward.reelvault.ui.common.pressScale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import io.github.wilymanhoward.reelvault.ui.common.VaultDropdownMenu as DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import io.github.wilymanhoward.reelvault.ui.common.VaultFilterChip as FilterChip
import androidx.compose.material3.Icon
import io.github.wilymanhoward.reelvault.ui.common.VaultIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import io.github.wilymanhoward.reelvault.ui.common.VaultLargeTopAppBar as LargeTopAppBar
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import io.github.wilymanhoward.reelvault.domain.model.Platform
import io.github.wilymanhoward.reelvault.domain.model.SavedItem
import io.github.wilymanhoward.reelvault.domain.model.SearchFilter
import io.github.wilymanhoward.reelvault.domain.model.TagSummary
import io.github.wilymanhoward.reelvault.domain.model.ViewMode
import io.github.wilymanhoward.reelvault.domain.repository.CollectionRepository
import io.github.wilymanhoward.reelvault.domain.repository.ItemRepository
import io.github.wilymanhoward.reelvault.domain.repository.TagRepository
import io.github.wilymanhoward.reelvault.ui.common.BatchActions
import io.github.wilymanhoward.reelvault.ui.common.EmptyState
import io.github.wilymanhoward.reelvault.ui.common.SavesScreen
import io.github.wilymanhoward.reelvault.ui.common.SavesViewModel
import io.github.wilymanhoward.reelvault.ui.navigation.SearchRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Search as you type, with platform / collection / tag filters. */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    items: ItemRepository,
    tags: TagRepository,
    settings: SettingsRepository,
    batch: BatchActions,
    collections: CollectionRepository,
) : SavesViewModel(batch, collections) {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(SearchFilter(tag = savedStateHandle.toRoute<SearchRoute>().tag))
    val filter: StateFlow<SearchFilter> = _filter.asStateFlow()

    val allTags: StateFlow<List<TagSummary>> = tags.observeAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val viewMode: StateFlow<ViewMode> = settings.settings.map { it.viewMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ViewMode.GRID)

    // Debounced so FTS runs once per pause in typing, not on every keystroke.
    val results: StateFlow<List<SavedItem>> = combine(_query.debounce(150), _filter) { q, f -> q to f }
        .flatMapLatest { (q, f) -> items.search(q, f) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(text: String) {
        _query.value = text
    }

    fun togglePlatform(platform: Platform) = _filter.update { f ->
        f.copy(platforms = if (platform in f.platforms) f.platforms - platform else f.platforms + platform)
    }

    fun setCollection(id: String?) = _filter.update { it.copy(collectionId = id) }
    fun setTag(name: String?) = _filter.update { it.copy(tag = name) }
}

/** The Search tab. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onOpenItem: (String) -> Unit, onOpenTags: () -> Unit, viewModel: SearchViewModel = hiltViewModel()) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val tags by viewModel.allTags.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val fieldInteraction = remember { MutableInteractionSource() }
    val focused by fieldInteraction.collectIsFocusedAsState()

    SavesScreen(
        viewModel = viewModel,
        items = results,
        viewMode = viewMode,
        onOpenItem = onOpenItem,
        topBar = {
            // While typing, the title folds away so the search field takes over the header.
            AnimatedVisibility(visible = !focused, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                LargeTopAppBar(
                    title = { Text("Search") },
                    actions = { IconButton(onClick = onOpenTags) { Icon(VaultIcons.Tag, "Tags") } },
                )
            }
        },
        header = {
            Column(Modifier.animateContentSize().then(if (focused) Modifier.statusBarsPadding() else Modifier)) {
                TextField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    placeholder = { Text("Search your saves") },
                    leadingIcon = { Icon(VaultIcons.Search, null) },
                    trailingIcon = {
                        AnimatedVisibility(visible = query.isNotEmpty(), enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                            IconButton(onClick = { viewModel.setQuery("") }) { Icon(VaultIcons.Close, "Clear") }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    ),
                    interactionSource = fieldInteraction,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        PickerChip(
                            label = collections.firstOrNull { it.id == filter.collectionId }?.name ?: "Collection",
                            selected = filter.collectionId != null,
                            options = collections.map { it.id to it.name },
                            onPick = viewModel::setCollection,
                        )
                    }
                    item {
                        PickerChip(
                            label = filter.tag ?: "Tag",
                            selected = filter.tag != null,
                            options = tags.map { it.name to "${it.name} (${it.count})" },
                            onPick = viewModel::setTag,
                        )
                    }
                    items(Platform.entries.filter { it != Platform.OTHER }) { platform ->
                        val selected = platform in filter.platforms
                        val interaction = remember { MutableInteractionSource() }
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.togglePlatform(platform) },
                            label = { Text(platform.displayName) },
                            leadingIcon = if (selected) ({ Icon(VaultIcons.Check, null, Modifier.size(16.dp)) }) else null,
                            interactionSource = interaction,
                            modifier = Modifier.animateItem().pressScale(interaction, 0.9f).animateContentSize(),
                        )
                    }
                }
            }
        },
        emptyContent = {
            if (query.isBlank() && filter.isEmpty) {
                EmptyState("Nothing to search yet", "Saves you add show up here, searchable by title, caption, author, tag and note.")
            } else {
                EmptyState("Nothing matches", "Try fewer words, or clear a filter.")
            }
        },
    )
}

/** A filter chip that opens a menu of [options] (id to label); picking the selected one clears it. */
@Composable
private fun PickerChip(label: String, selected: Boolean, options: List<Pair<String, String>>, onPick: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected,
            onClick = { if (selected) onPick(null) else open = true },
            label = { Text(label) },
            trailingIcon = if (selected) ({ Icon(VaultIcons.Close, "Clear $label", Modifier.size(16.dp)) }) else null,
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (options.isEmpty()) DropdownMenuItem(text = { Text("None yet") }, onClick = { open = false }, enabled = false)
            options.forEach { (id, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onPick(id); open = false })
            }
        }
    }
}
