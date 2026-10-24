package io.github.wilymanhoward.reelvault.data.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.wilymanhoward.reelvault.data.settings.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where sync left off, for one account.
 *
 * @property pulledItems / [pulledCollections] server timestamps (ms) of the newest pulled docs.
 * @property pushed local `updatedAt` up to which changes have been pushed.
 */
data class SyncCursors(val pulledItems: Long = 0, val pulledCollections: Long = 0, val pushed: Long = 0)

/** Persists [SyncCursors] keyed to the signed-in uid, so switching accounts starts a full sync. */
@Singleton
class SyncStateStore @Inject constructor(@ApplicationContext private val context: Context) {

    /** When the last successful sync finished (ms), or null. */
    val lastSyncedAt: Flow<Long?> = context.dataStore.data.map { it[LAST_SYNCED] }

    suspend fun load(uid: String): SyncCursors {
        val prefs = context.dataStore.data.first()
        if (prefs[UID] != uid) return SyncCursors()
        return SyncCursors(prefs[PULLED_ITEMS] ?: 0, prefs[PULLED_COLLECTIONS] ?: 0, prefs[PUSHED] ?: 0)
    }

    suspend fun save(uid: String, cursors: SyncCursors, syncedAt: Long) {
        context.dataStore.edit {
            it[UID] = uid
            it[PULLED_ITEMS] = cursors.pulledItems
            it[PULLED_COLLECTIONS] = cursors.pulledCollections
            it[PUSHED] = cursors.pushed
            it[LAST_SYNCED] = syncedAt
        }
    }

    /** Forget all progress: the next sync pulls and pushes everything. */
    suspend fun reset() {
        context.dataStore.edit {
            it.remove(UID)
            it.remove(PULLED_ITEMS)
            it.remove(PULLED_COLLECTIONS)
            it.remove(PUSHED)
            it.remove(LAST_SYNCED)
        }
    }

    private companion object {
        val UID = stringPreferencesKey("sync_uid")
        val PULLED_ITEMS = longPreferencesKey("sync_pulled_items")
        val PULLED_COLLECTIONS = longPreferencesKey("sync_pulled_collections")
        val PUSHED = longPreferencesKey("sync_pushed")
        val LAST_SYNCED = longPreferencesKey("sync_last_synced")
    }
}
