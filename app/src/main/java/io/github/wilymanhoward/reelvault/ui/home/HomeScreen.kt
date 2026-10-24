package io.github.wilymanhoward.reelvault.ui.home

import io.github.wilymanhoward.reelvault.ui.common.VaultIcons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import io.github.wilymanhoward.reelvault.ui.common.VaultAlertDialog as AlertDialog
import io.github.wilymanhoward.reelvault.ui.common.VaultDropdownMenu as DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import io.github.wilymanhoward.reelvault.ui.common.VaultFilledTonalButton as FilledTonalButton
import androidx.compose.material3.Icon
import io.github.wilymanhoward.reelvault.ui.common.VaultIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import io.github.wilymanhoward.reelvault.ui.common.VaultTextField as OutlinedTextField
import androidx.compose.material3.Text
import io.github.wilymanhoward.reelvault.ui.common.VaultTextButton as TextButton
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
import io.github.wilymanhoward.reelvault.ui.common.VaultLargeTopAppBar as LargeTopAppBar
import io.github.wilymanhoward.reelvault.domain.link.UrlExtractor
import io.github.wilymanhoward.reelvault.ui.common.AddLinkButton
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

    SavesScreen(
        viewModel = viewModel,
        items = saves,
        viewMode = settings.viewMode,
        onOpenItem = onOpenItem,
        topBar = {
            LargeTopAppBar(
                title = { Text("ReelVault") },
                actions = actions@{
                    // Nothing to sort or re-lay out in an empty library.
                    if (saves.isEmpty()) return@actions
                    var sortMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { sortMenu = true }) { Icon(VaultIcons.Sort, "Sort") }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.label) },
                                leadingIcon = { if (order == settings.sortOrder) Icon(VaultIcons.Check, null) },
                                onClick = { viewModel.setSortOrder(order); sortMenu = false },
                            )
                        }
                    }
                    val list = settings.viewMode == ViewMode.LIST
                    IconButton(onClick = { viewModel.setViewMode(if (list) ViewMode.GRID else ViewMode.LIST) }) {
                        Icon(
                            if (list) VaultIcons.Library else VaultIcons.List,
                            contentDescription = if (list) "Grid view" else "List view",
                        )
                    }
                },
            )
        },
        header = {
            // The empty state already says the library is empty; the count only appears once it means something.
            if (saves.isNotEmpty()) {
                Text(
                    if (saves.size == 1) "1 saved video" else "${saves.size} saved videos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            AddLinkButton(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
                onPaste = {
                    val clip = readClipboardText(context).orEmpty()
                    // A link on the clipboard saves straight away; anything else opens the dialog to fix up.
                    if (UrlExtractor.extract(clip) != null) viewModel.save(clip) else pasteText = clip
                },
                onType = { pasteText = "" },
            )

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
                title = "A home for your next find",
                body = "Keep the videos you want to return to. Share one to ReelVault, or tap Save a video above.",
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
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(VaultIcons.Link, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("Save this link?", style = MaterialTheme.typography.titleSmall)
                Text(link, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FilledTonalButton(onClick = onSave) { Text("Save") }
            IconButton(onClick = onDismiss) { Icon(VaultIcons.Close, contentDescription = "Dismiss") }
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
