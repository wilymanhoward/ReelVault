package io.github.wilymanhoward.reelvault.ui.home

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.wilymanhoward.reelvault.data.settings.SettingsRepository
import io.github.wilymanhoward.reelvault.domain.link.PlatformDetector
import io.github.wilymanhoward.reelvault.domain.link.UrlExtractor
import io.github.wilymanhoward.reelvault.domain.link.UrlNormalizer
import io.github.wilymanhoward.reelvault.domain.model.AppSettings
import io.github.wilymanhoward.reelvault.domain.model.Platform
import io.github.wilymanhoward.reelvault.domain.model.SavedItem
import io.github.wilymanhoward.reelvault.domain.model.SortOrder
import io.github.wilymanhoward.reelvault.domain.model.ViewMode
import io.github.wilymanhoward.reelvault.domain.model.sortedFor
import io.github.wilymanhoward.reelvault.domain.repository.CollectionRepository
import io.github.wilymanhoward.reelvault.domain.repository.ItemRepository
import io.github.wilymanhoward.reelvault.domain.usecase.SaveLinkUseCase
import io.github.wilymanhoward.reelvault.domain.usecase.SaveResult
import io.github.wilymanhoward.reelvault.ui.common.BatchActions
import io.github.wilymanhoward.reelvault.ui.common.SavesEvent
import io.github.wilymanhoward.reelvault.ui.common.SavesViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** All saves (sorted as chosen), saving from the paste dialog, and the clipboard suggestion. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val items: ItemRepository,
    private val saveLink: SaveLinkUseCase,
    private val settingsRepository: SettingsRepository,
    batch: BatchActions,
    collections: CollectionRepository,
) : SavesViewModel(batch, collections) {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val saves: StateFlow<List<SavedItem>> = combine(items.observeItems(), settingsRepository.settings) { list, s ->
        list.sortedFor(s.sortOrder)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _clipboardLink = MutableStateFlow<String?>(null)
    /** A supported, not-yet-saved link found on the clipboard. */
    val clipboardLink: StateFlow<String?> = _clipboardLink.asStateFlow()

    private val dismissedLinks = mutableSetOf<String>()

    fun setSortOrder(order: SortOrder) = viewModelScope.launch { settingsRepository.setSortOrder(order) }
    fun setViewMode(mode: ViewMode) = viewModelScope.launch { settingsRepository.setViewMode(mode) }

    fun onClipboardText(text: String) {
        viewModelScope.launch {
            val url = UrlExtractor.extract(text) ?: return@launch
            if (PlatformDetector.detect(url) == Platform.OTHER || url in dismissedLinks) return@launch
            if (items.findByNormalizedUrl(UrlNormalizer.normalize(url)) != null) return@launch
            _clipboardLink.value = url
        }
    }

    fun dismissClipboardLink() {
        _clipboardLink.value?.let(dismissedLinks::add)
        _clipboardLink.value = null
    }

    fun save(text: String) {
        if (text == _clipboardLink.value) dismissClipboardLink()
        viewModelScope.launch {
            val message = runCatching { saveLink(text) }.fold(
                onSuccess = {
                    when (it) {
                        is SaveResult.Saved -> "Saved to your vault"
                        is SaveResult.AlreadySaved -> "Already in your vault"
                        SaveResult.NoLink -> "That doesn't look like a link"
                    }
                },
                onFailure = { "Couldn't save that link" },
            )
            send(SavesEvent.Message(message))
        }
    }
}
