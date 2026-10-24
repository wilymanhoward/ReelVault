package io.github.wilymanhoward.reelvault.ui.quicksave

import io.github.wilymanhoward.reelvault.ui.common.VaultIcons
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import io.github.wilymanhoward.reelvault.ui.common.VaultButton as Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import io.github.wilymanhoward.reelvault.ui.common.VaultFilterChip as FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import io.github.wilymanhoward.reelvault.ui.common.VaultTextField as OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wilymanhoward.reelvault.domain.model.SavedItem
import io.github.wilymanhoward.reelvault.domain.model.VaultCollection
import io.github.wilymanhoward.reelvault.ui.common.AnimatedCheck
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Long enough to read "Saved" and tap a collection; any interaction cancels the auto-close.
private const val AUTO_CLOSE_MS = 2_800L

/** Bottom sheet shown over the source app after sharing a link to ReelVault. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSaveSheet(onClose: () -> Unit, viewModel: QuickSaveViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val collections by viewModel.allCollections.collectAsStateWithLifecycle()
    val memberOf by viewModel.memberOf.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var interacted by rememberSaveable { mutableStateOf(false) }
    val close: () -> Unit = { scope.launch { sheetState.hide() }.invokeOnCompletion { onClose() } }

    LaunchedEffect(state, interacted) {
        if (state is QuickSaveState.Done && !interacted) {
            delay(AUTO_CLOSE_MS)
            close()
        }
    }

    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentKey = { it::class },
            label = "quickSave",
        ) { s ->
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (s) {
                    QuickSaveState.Saving -> {
                        CircularProgressIndicator(Modifier.padding(24.dp))
                        Text("Saving…", style = MaterialTheme.typography.titleMedium)
                    }
                    is QuickSaveState.Done -> DoneContent(
                        item = s.item,
                        alreadySaved = s.alreadySaved,
                        collections = collections,
                        memberOf = memberOf,
                        onInteract = { interacted = true },
                        onToggle = viewModel::toggle,
                        onCreate = viewModel::createAndAdd,
                        onDone = close,
                    )
                    QuickSaveState.NoLink -> Problem(
                        icon = { Icon(VaultIcons.LinkOff, null, Modifier.size(40.dp)) },
                        title = "No link found",
                        body = "ReelVault saves video links. What you shared didn't contain one.",
                        onClose = close,
                    )
                    QuickSaveState.Failed -> Problem(
                        icon = { Icon(VaultIcons.Info, null, Modifier.size(40.dp)) },
                        title = "Couldn't save",
                        body = "Something went wrong while saving. Try sharing again.",
                        onClose = close,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DoneContent(
    item: SavedItem,
    alreadySaved: Boolean,
    collections: List<VaultCollection>,
    memberOf: Set<String>,
    onInteract: () -> Unit,
    onToggle: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDone: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    if (alreadySaved) {
        Icon(VaultIcons.Info, null, Modifier.size(48.dp), tint = colors.secondary)
    } else {
        AnimatedCheck(color = colors.primary, checkColor = colors.onPrimary, burstColor = colors.tertiary)
    }
    Spacer(Modifier.height(12.dp))
    Text(if (alreadySaved) "Already in your vault" else "Saved", style = MaterialTheme.typography.titleLarge)
    Text(
        "${item.platform.displayName} · ${item.url}",
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )

    Spacer(Modifier.height(20.dp))
    Text("Add to collection", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))

    var creating by rememberSaveable { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        collections.forEach { c ->
            val selected = c.id in memberOf
            FilterChip(
                selected = selected,
                onClick = { onInteract(); onToggle(c.id) },
                label = { Text(listOfNotNull(c.emoji, c.name).joinToString(" ")) },
                leadingIcon = if (selected) ({ Icon(VaultIcons.Check, null, Modifier.size(18.dp)) }) else null,
            )
        }
        FilterChip(
            selected = false,
            onClick = { onInteract(); creating = true },
            label = { Text("New") },
            leadingIcon = { Icon(VaultIcons.Plus, null, Modifier.size(18.dp)) },
        )
    }
    if (creating) {
        val submit = {
            onCreate(newName)
            newName = ""
            creating = false
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Collection name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.weight(1f),
            )
        }
    }

    Spacer(Modifier.height(20.dp))
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
}

@Composable
private fun Problem(icon: @Composable () -> Unit, title: String, body: String, onClose: () -> Unit) {
    icon()
    Spacer(Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.titleLarge)
    Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(20.dp))
    Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
}
