package io.github.wilymanhoward.reelvault.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.wilymanhoward.reelvault.domain.model.AppSettings
import io.github.wilymanhoward.reelvault.domain.model.SortOrder
import io.github.wilymanhoward.reelvault.domain.model.ThemeMode
import io.github.wilymanhoward.reelvault.domain.model.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

internal val Context.dataStore by preferencesDataStore(name = "settings")

/** Reads and writes [AppSettings] in a Preferences DataStore. */
@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs.enum(THEME_MODE, ThemeMode.SYSTEM),
            dynamicColor = prefs[DYNAMIC_COLOR] ?: false,
            imageLabeling = prefs[IMAGE_LABELING] ?: true,
            sortOrder = prefs.enum(SORT_ORDER, SortOrder.NEWEST),
            viewMode = prefs.enum(VIEW_MODE, ViewMode.GRID),
            onboardingDone = prefs[ONBOARDING_DONE] ?: false,
        )
    }

    suspend fun setOnboardingDone() = set(ONBOARDING_DONE, true)

    suspend fun setThemeMode(mode: ThemeMode) = set(THEME_MODE, mode.name)
    suspend fun setDynamicColor(enabled: Boolean) = set(DYNAMIC_COLOR, enabled)
    suspend fun setImageLabeling(enabled: Boolean) = set(IMAGE_LABELING, enabled)
    suspend fun setSortOrder(order: SortOrder) = set(SORT_ORDER, order.name)
    suspend fun setViewMode(mode: ViewMode) = set(VIEW_MODE, mode.name)

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    private inline fun <reified E : Enum<E>> Preferences.enum(key: Preferences.Key<String>, default: E): E =
        this[key]?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val IMAGE_LABELING = booleanPreferencesKey("image_labeling")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }
}
