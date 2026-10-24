package io.github.wilymanhoward.reelvault.ui.settings

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
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
import io.github.wilymanhoward.reelvault.ui.common.GroupFooter
import io.github.wilymanhoward.reelvault.ui.common.VaultIcons
import io.github.wilymanhoward.reelvault.ui.common.VaultListItem
import io.github.wilymanhoward.reelvault.ui.common.VaultSeparator
import io.github.wilymanhoward.reelvault.ui.common.VaultAlertDialog as AlertDialog
import io.github.wilymanhoward.reelvault.ui.common.VaultTextButton as TextButton
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

/** "Just now" for the last minute, then "5 minutes ago", "Yesterday" and so on. */
private fun syncedLabel(at: Long?, now: Long = System.currentTimeMillis()): String = when {
    at == null -> "Not synced yet"
    now - at < DateUtils.MINUTE_IN_MILLIS -> "Just now"
    else -> DateUtils.getRelativeTimeSpanString(at, now, DateUtils.MINUTE_IN_MILLIS).toString()
}

/** Sign-in and sync, as the first grouped section of Settings. */
@Composable
fun AccountSection(viewModel: AccountViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lastSynced by viewModel.lastSyncedAt.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }

    SectionTitle("Account and sync")
    when (val s = state) {
        null -> Unit
        AccountState.NotConfigured -> {
            SettingsGroup {
                VaultListItem(
                    leadingContent = { Icon(VaultIcons.CloudOff, null) },
                    headlineContent = { Text("Sync is off in this copy") },
                    supportingContent = {
                        Text("This build of ReelVault was made without a Firebase project, so sign-in, sync and sharing aren't available. Everything else works on this device.")
                    },
                )
            }
        }
        AccountState.SignedOut -> {
            SettingsGroup {
                if (viewModel.googleAvailable) {
                    ActionRow("Sign in with Google", enabled = !busy) { viewModel.signInWithGoogle(context) }
                    VaultSeparator()
                }
                ActionRow("Sync without an account", enabled = !busy, onClick = viewModel::continueAnonymously)
            }
            GroupFooter("Your saves live on this device. Sign in to back them up, sync between devices and share collections.")
        }
        is AccountState.SignedIn -> {
            SettingsGroup {
                VaultListItem(
                    leadingContent = { Icon(VaultIcons.User, null) },
                    headlineContent = { Text(if (s.isAnonymous) "Anonymous account" else s.name ?: s.email ?: "Signed in") },
                    // The address gets a line of its own, so it never breaks mid-word beside an action.
                    supportingContent = s.email.takeUnless { s.isAnonymous }?.let { { Text(it) } },
                )
                VaultSeparator()
                VaultListItem(
                    modifier = Modifier.clickable(role = Role.Button, onClick = viewModel::syncNow),
                    leadingContent = { Icon(VaultIcons.Sync, null) },
                    headlineContent = { Text("Sync now") },
                    trailingContent = { Text(syncedLabel(lastSynced), style = MaterialTheme.typography.bodyMedium) },
                )
                if (s.isAnonymous && viewModel.googleAvailable) {
                    VaultSeparator()
                    ActionRow("Upgrade to Google account", enabled = !busy) { viewModel.signInWithGoogle(context) }
                }
                VaultSeparator()
                ActionRow("Sign out", enabled = !busy, color = MaterialTheme.colorScheme.error) { confirmSignOut = true }
            }
            if (s.isAnonymous && viewModel.googleAvailable) {
                GroupFooter("Link a Google account to keep your saves if you lose this phone.")
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
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 8.dp),
        )
    }
}

/** A tappable row whose title is the action, like iOS settings buttons. */
@Composable
private fun ActionRow(title: String, enabled: Boolean, color: Color = MaterialTheme.colorScheme.primary, onClick: () -> Unit) {
    VaultListItem(
        modifier = Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        headlineContent = { Text(title, color = if (enabled) color else color.copy(alpha = 0.4f)) },
    )
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
