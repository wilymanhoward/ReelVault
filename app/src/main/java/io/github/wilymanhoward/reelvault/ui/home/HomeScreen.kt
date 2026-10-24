package io.github.wilymanhoward.reelvault.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wilymanhoward.reelvault.domain.model.SortOrder
import io.github.wilymanhoward.reelvault.domain.model.ViewMode
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import io.github.wilymanhoward.reelvault.domain.link.UrlExtractor
import io.github.wilymanhoward.reelvault.ui.common.AddFab
import io.github.wilymanhoward.reelvault.ui.common.ClipboardTextEffect
import io.github.wilymanhoward.reelvault.ui.common.EmptyState
import io.github.wilymanhoward.reelvault.ui.common.SavesScreen
import io.github.wilymanhoward.reelvault.ui.common.readClipboardText

/** "All saves": every save in a grid or list. Tapping opens the video; ⋯ opens details. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenItem: (String) -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val saves by viewModel.saves.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val clipboardLink by viewModel.clipboardLink.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pasteText by rememberSaveable { mutableStateOf<String?>(null) }

    ClipboardTextEffect(viewModel::onClipboardText)

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    SavesScreen(
        viewModel = viewModel,
        items = saves,
        viewMode = settings.viewMode,
        onOpenItem = onOpenItem,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("ReelVault") },
                scrollBehavior = scrollBehavior,
                actions = {
                    var sortMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Outlined.Sort, "Sort") }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.label) },
                                leadingIcon = { if (order == settings.sortOrder) Icon(Icons.Filled.Check, null) },
                                onClick = { viewModel.setSortOrder(order); sortMenu = false },
                            )
                        }
                    }
                    val list = settings.viewMode == ViewMode.LIST
                    IconButton(onClick = { viewModel.setViewMode(if (list) ViewMode.GRID else ViewMode.LIST) }) {
                        Icon(
                            if (list) Icons.Outlined.GridView else Icons.AutoMirrored.Outlined.ViewList,
                            contentDescription = if (list) "Grid view" else "List view",
                        )
                    }
                },
            )
        },
        header = {
            // Keep showing the last link while the chip's exit animation runs.
            var shownLink by remember { mutableStateOf<String?>(null) }
            if (clipboardLink != null) shownLink = clipboardLink
            AnimatedVisibility(
                visible = clipboardLink != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                val link = shownLink ?: return@AnimatedVisibility
                ClipboardSuggestion(link, onSave = { viewModel.save(link) }, onDismiss = viewModel::dismissClipboardLink)
            }
        },
        emptyContent = {
            EmptyState(
                title = "Your vault is empty",
                body = "In TikTok, Instagram, YouTube or any app, tap Share and pick ReelVault. Or add a link with the + button.",
            )
        },
        floatingActionButton = {
            AddFab(
                onPaste = {
                    val clip = readClipboardText(context).orEmpty()
                    // A link on the clipboard saves straight away; anything else opens the dialog to fix up.
                    if (UrlExtractor.extract(clip) != null) viewModel.save(clip) else pasteText = clip
                },
                onType = { pasteText = "" },
            )
        },
    )

    pasteText?.let { initial ->
        PasteLinkDialog(
            initial = initial,
            onSave = { viewModel.save(it); pasteText = null },
            onDismiss = { pasteText = null },
        )
    }
}

@Composable
private fun ClipboardSuggestion(link: String, onSave: () -> Unit, onDismiss: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Link, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("Save this link?", style = MaterialTheme.typography.titleSmall)
                Text(link, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FilledTonalButton(onClick = onSave) { Text("Save") }
            IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Dismiss") }
        }
    }
}

@Composable
private fun PasteLinkDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.isEmpty()) "Add a link" else "Save a link") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Video link") },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
