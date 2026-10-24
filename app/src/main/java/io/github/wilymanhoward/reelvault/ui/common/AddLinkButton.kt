package io.github.wilymanhoward.reelvault.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLinkButton(onPaste: () -> Unit, onType: () -> Unit, modifier: Modifier = Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    VaultButton(onClick = { open = true }, modifier = modifier) {
        Icon(VaultIcons.Plus, null, Modifier.size(20.dp))
        Text("Save a video", Modifier.padding(start = 10.dp))
    }
    if (open) {
        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Add to your vault", style = MaterialTheme.typography.headlineMedium)
                Text("A good video deserves to be found again.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                VaultOutlinedButton(onClick = { open = false; onPaste() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(VaultIcons.Paste, null, Modifier.size(20.dp))
                    Text("Paste a link", Modifier.padding(start = 10.dp))
                }
                VaultOutlinedButton(onClick = { open = false; onType() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(VaultIcons.Keyboard, null, Modifier.size(20.dp))
                    Text("Type a link", Modifier.padding(start = 10.dp))
                }
            }
        }
    }
}
