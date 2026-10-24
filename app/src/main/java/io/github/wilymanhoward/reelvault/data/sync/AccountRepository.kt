package io.github.wilymanhoward.reelvault.data.sync

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.wilymanhoward.reelvault.data.firebase.FirebaseProvider
import io.github.wilymanhoward.reelvault.data.local.ReelVaultDatabase
import io.github.wilymanhoward.reelvault.domain.repository.TagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Who is signed in, as far as the app cares. */
sealed interface AccountState {
    /** This build has no Firebase config; the app is local-only. */
    data object NotConfigured : AccountState
    data object SignedOut : AccountState
    data class SignedIn(val uid: String, val isAnonymous: Boolean, val email: String?, val name: String?) : AccountState
}

/**
 * Firebase Auth: anonymous accounts, Google sign-in through Credential Manager, and upgrading an
 * anonymous account to Google by linking (same uid, so its synced data carries over).
 */
@Singleton
class AccountRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebase: FirebaseProvider,
    private val db: ReelVaultDatabase,
    private val tags: TagRepository,
    private val syncState: SyncStateStore,
    private val syncScheduler: SyncScheduler,
) {
    val state: Flow<AccountState> = firebase.auth?.let { auth ->
        callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { trySend(it.toState()) }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }.distinctUntilChanged()
    } ?: flowOf(AccountState.NotConfigured)

    /** The signed-in uid right now, or null. */
    val currentUid: String? get() = firebase.auth?.currentUser?.uid

    val googleSignInAvailable: Boolean get() = firebase.webClientId != null

    /** Syncs without an account picker; can be upgraded to Google later. */
    suspend fun continueAnonymously() {
        val auth = firebase.auth ?: error("Firebase isn't configured in this build")
        auth.signInAnonymously().await()
        afterSignIn()
    }

    /**
     * Shows the Google account picker and signs in. An anonymous user is upgraded in place; if that
     * Google account already has ReelVault data, we switch to it and this device's saves merge in.
     *
     * @param activityContext an Activity, which Credential Manager needs to show its UI.
     */
    suspend fun signInWithGoogle(activityContext: Context) {
        val auth = firebase.auth ?: error("Firebase isn't configured in this build")
        val clientId = firebase.webClientId ?: error("Google sign-in isn't enabled for this Firebase project")
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
            .build()
        val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Unexpected credential type"
        }
        val firebaseCredential = GoogleAuthProvider.getCredential(GoogleIdTokenCredential.createFrom(credential.data).idToken, null)
        val current = auth.currentUser
        if (current?.isAnonymous == true) {
            try {
                current.linkWithCredential(firebaseCredential).await()
            } catch (_: FirebaseAuthUserCollisionException) {
                auth.signInWithCredential(firebaseCredential).await()
            }
        } else {
            auth.signInWithCredential(firebaseCredential).await()
        }
        afterSignIn()
    }

    /**
     * Signs out. Local saves stay unless [removeLocalData], which wipes the database and cached
     * thumbnails (for a shared device). An anonymous account can't be signed back into.
     */
    suspend fun signOut(removeLocalData: Boolean) {
        syncScheduler.cancel()
        firebase.auth?.signOut()
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
        syncState.reset()
        if (removeLocalData) {
            withContext(Dispatchers.IO) {
                db.clearAllTables()
                File(context.filesDir, "thumbnails").deleteRecursively()
            }
            tags.resetRules()
        }
    }

    private suspend fun afterSignIn() {
        // New account or switched account: start from scratch so everything local gets pushed.
        syncState.reset()
        syncScheduler.requestSync(immediate = true)
        syncScheduler.schedulePeriodic()
    }

    private fun FirebaseAuth.toState(): AccountState = currentUser?.let {
        AccountState.SignedIn(it.uid, it.isAnonymous, it.email, it.displayName)
    } ?: AccountState.SignedOut
}
