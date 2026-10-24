package io.github.wilymanhoward.reelvault.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import io.github.wilymanhoward.reelvault.ui.common.GroupFooter
import io.github.wilymanhoward.reelvault.ui.common.VaultSeparator

import io.github.wilymanhoward.reelvault.ui.common.VaultIcons
import io.github.wilymanhoward.reelvault.ui.common.VaultChoices
import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import io.github.wilymanhoward.reelvault.ui.common.VaultSnackbarHost as SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.wilymanhoward.reelvault.BuildConfig
import io.github.wilymanhoward.reelvault.data.backup.BackupRepository
import io.github.wilymanhoward.reelvault.domain.repository.PreviewScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import io.github.wilymanhoward.reelvault.ui.common.VaultListItem as ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import io.github.wilymanhoward.reelvault.ui.common.VaultSwitch as Switch
import androidx.compose.material3.Text
import io.github.wilymanhoward.reelvault.ui.common.VaultLargeTopAppBar as LargeTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.wilymanhoward.reelvault.data.settings.SettingsRepository
import io.github.wilymanhoward.reelvault.domain.model.AppSettings
import io.github.wilymanhoward.reelvault.domain.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** App preferences, plus backup export/import. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val backup: BackupRepository,
    private val previews: PreviewScheduler,
) : ViewModel() {
    val state: StateFlow<AppSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }
    fun setImageLabeling(enabled: Boolean) = viewModelScope.launch { settings.setImageLabeling(enabled) }

    fun exportTo(uri: Uri, resolver: ContentResolver) = viewModelScope.launch {
        val message = runCatching {
            val text = backup.export()
            withContext(Dispatchers.IO) {
                resolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } ?: error("Couldn't open the file")
            }
            "Exported your vault"
        }.getOrElse { "Export failed: ${it.localizedMessage ?: it.javaClass.simpleName}" }
        _messages.send(message)
    }

    fun importFrom(uri: Uri, resolver: ContentResolver) = viewModelScope.launch {
        val message = runCatching {
            val text = withContext(Dispatchers.IO) {
                resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("Couldn't open the file")
            }
            val result = backup.import(text)
            result.addedIds.forEach(previews::schedule)
            buildString {
                append("Imported ${result.addedIds.size} saves")
                if (result.skipped > 0) append(", ${result.skipped} already here")
                if (result.collections > 0) append(", ${result.collections} collections")
            }
        }.getOrElse { it.localizedMessage ?: "Import failed" }
        _messages.send(message)
    }
}

/** The Settings tab. [accountSection] is the sign-in / sync block. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onEditRules: () -> Unit,
    onOpenAbout: () -> Unit,
    accountSection: @Composable () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.exportTo(it, context.contentResolver) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importFrom(it, context.contentResolver) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { LargeTopAppBar(title = { Text("Settings") }) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            accountSection()

            SectionTitle("Appearance")
            SettingsGroup {
                VaultChoices(
                    options = listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark"),
                    selected = settings.themeMode,
                    onSelect = viewModel::setThemeMode,
                )
            }

            SectionTitle("Auto-tagging")
            SettingsGroup {
                SwitchRow(
                    title = "Image labeling",
                    subtitle = "Recognize what's in thumbnails on this device to suggest categories",
                    checked = settings.imageLabeling,
                    onChange = viewModel::setImageLabeling,
                )
                VaultSeparator()
                ListItem(
                    modifier = Modifier.clickable(onClick = onEditRules),
                    headlineContent = { Text("Tag rules") },
                    trailingContent = { Icon(VaultIcons.ChevronRight, null) },
                )
            }
            GroupFooter("Rules turn keywords in titles, captions and hashtags into categories.")

            SectionTitle("Your data")
            SettingsGroup {
                ListItem(
                    modifier = Modifier.clickable { exportLauncher.launch("reelvault-backup-${LocalDate.now()}.json") },
                    headlineContent = { Text("Export to a file") },
                    trailingContent = { Icon(VaultIcons.ChevronRight, null) },
                )
                VaultSeparator()
                ListItem(
                    modifier = Modifier.clickable { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                    headlineContent = { Text("Import from a file") },
                    trailingContent = { Icon(VaultIcons.ChevronRight, null) },
                )
            }
            GroupFooter("Exports saves, notes, tags, collections and rules as JSON. Importing adds what's new and keeps saves you already have.")

            SectionTitle("About")
            SettingsGroup {
                ListItem(
                    modifier = Modifier.clickable(onClick = onOpenAbout),
                    headlineContent = { Text("About ReelVault") },
                    trailingContent = {
                        Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
                        Icon(VaultIcons.ChevronRight, null)
                    },
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** An inset rounded group of rows, iOS grouped-list style; separate rows with [VaultSeparator]. */
@Composable
internal fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) { Column(content = content) }
}

/** A group header: footnote size, sentence case, aligned with the row text inside the group. */
@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 28.dp, bottom = 8.dp),
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onChange(!checked) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
    )
}
