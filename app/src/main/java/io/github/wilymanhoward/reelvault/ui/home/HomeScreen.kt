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
import io.github.wilymanhoward.reelvault.ui.common.ClipboardTextEffect
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

    SavesScreen(
        viewModel = viewModel,
        items = saves,
        viewMode = settings.viewMode,
        onOpenItem = onOpenItem,
        topBar = {
            TopAppBar(
                title = { Text("ReelVault") },
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
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Nothing saved yet.\nShare a video to ReelVault from any app, or paste a link.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { pasteText = readClipboardText(context).orEmpty() },
                icon = { Icon(Icons.Outlined.ContentPaste, null) },
                text = { Text("Paste link") },
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
        title = { Text("Save a link") },
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
