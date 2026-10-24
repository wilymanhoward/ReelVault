package io.github.wilymanhoward.reelvault.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.wilymanhoward.reelvault.domain.link.PlatformDetector
import io.github.wilymanhoward.reelvault.domain.link.UrlExtractor
import io.github.wilymanhoward.reelvault.domain.link.UrlNormalizer
import io.github.wilymanhoward.reelvault.domain.model.Platform
import io.github.wilymanhoward.reelvault.domain.model.SavedItem
import io.github.wilymanhoward.reelvault.domain.repository.ItemRepository
import io.github.wilymanhoward.reelvault.domain.usecase.SaveLinkUseCase
import io.github.wilymanhoward.reelvault.domain.usecase.SaveResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** All saves, plus saving from the paste dialog and the clipboard suggestion. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val items: ItemRepository,
    private val saveLink: SaveLinkUseCase,
) : ViewModel() {

    val saves: StateFlow<List<SavedItem>> = items.observeItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _clipboardLink = MutableStateFlow<String?>(null)
    /** A supported, not-yet-saved link found on the clipboard. */
    val clipboardLink: StateFlow<String?> = _clipboardLink.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    /** One-off snackbar messages. */
    val messages: Flow<String> = _messages.receiveAsFlow()

    private val dismissedLinks = mutableSetOf<String>()

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
            _messages.send(message)
        }
    }
}
