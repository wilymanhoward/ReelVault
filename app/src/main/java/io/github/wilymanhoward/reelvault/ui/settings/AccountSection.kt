package io.github.wilymanhoward.reelvault.ui.settings

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.wilymanhoward.reelvault.data.sync.AccountRepository
import io.github.wilymanhoward.reelvault.data.sync.AccountState
import io.github.wilymanhoward.reelvault.data.sync.SyncScheduler
import io.github.wilymanhoward.reelvault.data.sync.SyncStateStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Account and sync status for Settings. */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accounts: AccountRepository,
    private val scheduler: SyncScheduler,
    syncState: SyncStateStore,
) : ViewModel() {
    val state: StateFlow<AccountState?> = accounts.state.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val lastSyncedAt: StateFlow<Long?> = syncState.lastSyncedAt.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val googleAvailable get() = accounts.googleSignInAvailable

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun signInWithGoogle(activity: Context) = run("Google sign-in failed") { accounts.signInWithGoogle(activity) }
    fun continueAnonymously() = run("Couldn't start sync") { accounts.continueAnonymously() }
    fun signOut(removeLocalData: Boolean) = run("Couldn't sign out") { accounts.signOut(removeLocalData) }
    fun syncNow() = scheduler.requestSync(immediate = true)
    fun clearError() { _error.value = null }

    private fun run(failure: String, block: suspend () -> Unit) = viewModelScope.launch {
        _busy.value = true
        _error.value = null
        try {
            block()
        } catch (_: GetCredentialCancellationException) {
            // The user closed the account picker; nothing to report.
        } catch (e: Exception) {
            // Our own messages (IllegalStateException) are already user-facing; others get context.
            _error.value = if (e is IllegalStateException) e.message else "$failure: ${e.localizedMessage ?: e.javaClass.simpleName}"
        } finally {
            _busy.value = false
        }
    }
}

/** Sign-in and sync controls at the top of Settings. */
@Composable
fun AccountSection(viewModel: AccountViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lastSynced by viewModel.lastSyncedAt.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }

    SectionTitle("Account & sync")
    when (val s = state) {
        null -> Unit
        AccountState.NotConfigured -> ListItem(
            leadingContent = { Icon(Icons.Outlined.CloudOff, null) },
            headlineContent = { Text("Sync isn't set up in this build") },
            supportingContent = { Text("Everything works offline. Add a Firebase config to enable sign-in, sync and sharing (see README).") },
        )
        AccountState.SignedOut -> Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Your saves live on this device. Sign in to back them up, sync between devices and share collections.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (viewModel.googleAvailable) {
                Button(onClick = { viewModel.signInWithGoogle(context) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign in with Google")
                }
            }
            OutlinedButton(onClick = viewModel::continueAnonymously, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Sync without an account")
            }
        }
        is AccountState.SignedIn -> Column {
            ListItem(
                leadingContent = { Icon(Icons.Outlined.AccountCircle, null) },
                headlineContent = { Text(if (s.isAnonymous) "Anonymous account" else s.name ?: s.email ?: "Signed in") },
                supportingContent = {
                    Text(
                        listOfNotNull(
                            s.email.takeUnless { s.isAnonymous },
                            lastSynced?.let { "Synced ${DateUtils.getRelativeTimeSpanString(it)}" } ?: "Not synced yet",
                        ).joinToString(" · "),
                    )
                },
                trailingContent = {
                    TextButton(onClick = viewModel::syncNow) {
                        Icon(Icons.Outlined.Sync, null)
                        Text(" Sync now")
                    }
                },
            )
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (s.isAnonymous && viewModel.googleAvailable) {
                    Text(
                        "Link a Google account to keep your saves if you lose this phone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { viewModel.signInWithGoogle(context) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Upgrade to Google account")
                    }
                }
                OutlinedButton(onClick = { confirmSignOut = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign out")
                }
            }
            if (confirmSignOut) {
                SignOutDialog(
                    anonymous = s.isAnonymous,
                    onSignOut = { remove -> viewModel.signOut(remove); confirmSignOut = false },
                    onDismiss = { confirmSignOut = false },
                )
            }
        }
    }
    error?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun SignOutDialog(anonymous: Boolean, onSignOut: (removeLocalData: Boolean) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sign out?") },
        text = {
            Text(
                buildString {
                    append("Sync stops. You can keep your saves on this device or remove them.")
                    if (anonymous) append("\n\nThis is an anonymous account: once signed out, its cloud copy can't be reached again.")
                },
            )
        },
        confirmButton = { TextButton(onClick = { onSignOut(false) }) { Text("Keep saves here") } },
        dismissButton = {
            TextButton(onClick = { onSignOut(true) }) { Text("Remove from device", color = MaterialTheme.colorScheme.error) }
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
