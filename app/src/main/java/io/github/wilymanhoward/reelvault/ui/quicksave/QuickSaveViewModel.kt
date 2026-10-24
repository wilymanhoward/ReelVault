package io.github.wilymanhoward.reelvault.ui.quicksave

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.wilymanhoward.reelvault.domain.model.SavedItem
import io.github.wilymanhoward.reelvault.domain.model.VaultCollection
import io.github.wilymanhoward.reelvault.domain.repository.CollectionRepository
import io.github.wilymanhoward.reelvault.domain.usecase.SaveLinkUseCase
import io.github.wilymanhoward.reelvault.domain.usecase.SaveResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the quick-save sheet is showing. */
sealed interface QuickSaveState {
    data object Saving : QuickSaveState
    data class Done(val item: SavedItem, val alreadySaved: Boolean) : QuickSaveState
    data object NoLink : QuickSaveState
    data object Failed : QuickSaveState
}

/** Saves the shared text as soon as the sheet opens, then lets the user file it into collections. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QuickSaveViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    saveLink: SaveLinkUseCase,
    private val collections: CollectionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<QuickSaveState>(QuickSaveState.Saving)
    val state: StateFlow<QuickSaveState> = _state.asStateFlow()

    val allCollections: StateFlow<List<VaultCollection>> = collections.observeCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val memberOf: StateFlow<Set<String>> = _state
        .flatMapLatest { s -> if (s is QuickSaveState.Done) collections.observeCollectionIdsFor(s.item.id) else flowOf(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        // The activity's intent extras arrive in the SavedStateHandle; a link may be in the text or the subject.
        val text = listOfNotNull(
            savedStateHandle.get<CharSequence>(Intent.EXTRA_TEXT),
            savedStateHandle.get<CharSequence>(Intent.EXTRA_SUBJECT),
        ).joinToString(" ")
        viewModelScope.launch {
            _state.value = runCatching { saveLink(text) }.fold(
                onSuccess = {
                    when (it) {
                        is SaveResult.Saved -> QuickSaveState.Done(it.item, alreadySaved = false)
                        is SaveResult.AlreadySaved -> QuickSaveState.Done(it.item, alreadySaved = true)
                        SaveResult.NoLink -> QuickSaveState.NoLink
                    }
                },
                onFailure = { QuickSaveState.Failed },
            )
        }
    }

    fun toggle(collectionId: String) {
        val item = (state.value as? QuickSaveState.Done)?.item ?: return
        viewModelScope.launch { collections.setMembership(item.id, collectionId, collectionId !in memberOf.value) }
    }

    fun createAndAdd(name: String) {
        val item = (state.value as? QuickSaveState.Done)?.item ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            val created = collections.create(name)
            collections.setMembership(item.id, created.id, member = true)
        }
    }
}
