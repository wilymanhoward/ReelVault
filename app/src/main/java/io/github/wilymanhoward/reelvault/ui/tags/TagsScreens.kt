package io.github.wilymanhoward.reelvault.ui.tags

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.wilymanhoward.reelvault.domain.model.TagRule
import io.github.wilymanhoward.reelvault.domain.model.TagSummary
import io.github.wilymanhoward.reelvault.domain.repository.TagRepository
import io.github.wilymanhoward.reelvault.ui.common.BackButton
import io.github.wilymanhoward.reelvault.ui.common.ConfirmDialog
import io.github.wilymanhoward.reelvault.ui.common.TextInputDialog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Tags and auto-tagging rules. */
@HiltViewModel
class TagsViewModel @Inject constructor(private val repo: TagRepository) : ViewModel() {
    val tags: StateFlow<List<TagSummary>> = repo.observeAllTags().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val rules: StateFlow<List<TagRule>> = repo.observeRules().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun rename(old: String, new: String) = viewModelScope.launch { repo.renameTag(old, new) }
    fun delete(name: String) = viewModelScope.launch { repo.deleteTag(name) }
    fun saveRule(rule: TagRule) = viewModelScope.launch { repo.saveRule(rule) }
    fun deleteRule(id: Long) = viewModelScope.launch { repo.deleteRule(id) }
    fun resetRules() = viewModelScope.launch { repo.resetRules() }
    suspend fun reapply() = repo.reapplyAutoTags()
}

/** Every tag with its count. Tap to search by it; rename or delete from the menu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(onBack: () -> Unit, onTagClick: (String) -> Unit, viewModel: TagsViewModel = hiltViewModel()) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("Tags") }, navigationIcon = { BackButton(onBack) }) },
    ) { padding ->
        if (tags.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No tags yet. Saves get tagged automatically from hashtags and your rules, or add tags on a save.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LazyColumn(Modifier.padding(padding)) {
            items(tags, key = { it.name }) { tag ->
                var menu by remember { mutableStateOf(false) }
                ListItem(
                    modifier = Modifier.clickable { onTagClick(tag.name) }.animateItem(),
                    headlineContent = { Text(tag.name) },
                    supportingContent = { Text(if (tag.count == 1) "1 save" else "${tag.count} saves") },
                    trailingContent = {
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More for ${tag.name}") }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { renaming = tag.name; menu = false })
                                DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { deleting = tag.name; menu = false })
                            }
                        }
                    },
                )
            }
        }
    }

    renaming?.let { old ->
        TextInputDialog(
            title = "Rename tag",
            label = "Name",
            initial = old,
            onConfirm = { viewModel.rename(old, it); renaming = null },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { name ->
        ConfirmDialog(
            title = "Delete “$name”?",
            body = "It's removed from every save. Auto tags can come back when rules are re-applied.",
            confirm = "Delete",
            onConfirm = { viewModel.delete(name); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

/** Edit the keyword → category rules used by auto-tagging. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagRulesScreen(onBack: () -> Unit, viewModel: TagsViewModel = hiltViewModel()) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<TagRule?>(null) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Auto-tag rules") },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            viewModel.reapply()
                            snackbar.showSnackbar("Rules re-applied to all saves")
                        }
                    }) { Icon(Icons.Outlined.Refresh, "Re-apply to all saves") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Reset to defaults") }, onClick = { confirmReset = true; menu = false })
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = TagRule(category = "", keywords = emptyList()) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("New rule") },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                Text(
                    "A save gets a category when any keyword appears in its title, caption, hashtags or image labels.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            items(rules, key = { it.id }) { rule ->
                ListItem(
                    modifier = Modifier.clickable { editing = rule }.animateItem(),
                    headlineContent = { Text(rule.category) },
                    supportingContent = { Text(rule.keywords.joinToString(", "), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    trailingContent = { Switch(checked = rule.enabled, onCheckedChange = { viewModel.saveRule(rule.copy(enabled = it)) }) },
                )
            }
        }
    }

    editing?.let { rule ->
        RuleDialog(
            rule = rule,
            onSave = { viewModel.saveRule(it); editing = null },
            onDelete = if (rule.id != 0L) ({ viewModel.deleteRule(rule.id); editing = null }) else null,
            onDismiss = { editing = null },
        )
    }
    if (confirmReset) {
        ConfirmDialog(
            title = "Reset rules?",
            body = "Your rule edits are replaced by the built-in rules. Tags already on saves stay until rules are re-applied.",
            confirm = "Reset",
            onConfirm = { viewModel.resetRules(); confirmReset = false },
            onDismiss = { confirmReset = false },
        )
    }
}

@Composable
private fun RuleDialog(rule: TagRule, onSave: (TagRule) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var category by rememberSaveable { mutableStateOf(rule.category) }
    var keywords by rememberSaveable { mutableStateOf(rule.keywords.joinToString(", ")) }
    val parsed = keywords.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (rule.id == 0L) "New rule" else "Edit rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category tag") }, singleLine = true)
                OutlinedTextField(value = keywords, onValueChange = { keywords = it }, label = { Text("Keywords, comma-separated") }, minLines = 3)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(rule.copy(category = category.trim(), keywords = parsed)) }, enabled = category.isNotBlank() && parsed.isNotEmpty()) {
                Text("Save")
            }
        },
        dismissButton = {
            onDelete?.let { TextButton(onClick = it) { Text("Delete", color = MaterialTheme.colorScheme.error) } }
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
