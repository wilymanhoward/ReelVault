package io.github.wilymanhoward.reelvault.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.wilymanhoward.reelvault.data.settings.SettingsRepository
import io.github.wilymanhoward.reelvault.domain.model.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Activity-level state: the settings that shape the theme and the first screen. */
@HiltViewModel
class MainViewModel @Inject constructor(private val settingsRepository: SettingsRepository) : ViewModel() {
    /** Null until DataStore has been read once, so the UI doesn't flash onboarding for returning users. */
    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun finishOnboarding() = viewModelScope.launch { settingsRepository.setOnboardingDone() }
}
