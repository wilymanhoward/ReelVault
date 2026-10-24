package io.github.wilymanhoward.reelvault.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.room.InvalidationTracker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.wilymanhoward.reelvault.data.firebase.FirebaseProvider
import io.github.wilymanhoward.reelvault.data.local.ReelVaultDatabase
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Runs [SyncEngine] for the signed-in account. Retries with backoff on failure. */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val firebase: FirebaseProvider,
    private val engine: SyncEngine,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = firebase.auth?.currentUser?.uid ?: return Result.success()
        return try {
            // Edits made while syncing are picked up by another round, a few at most.
            var rounds = 0
            while (engine.sync(uid) && ++rounds < MAX_ROUNDS) Unit
            Result.success()
        } catch (e: Exception) {
            Log.w("ReelVault", "Sync failed (attempt ${runAttemptCount + 1})", e)
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private companion object {
        const val MAX_ROUNDS = 3
        const val MAX_ATTEMPTS = 5
    }
}

/** Queues sync work: soon after local changes, and every few hours as a backstop. */
@Singleton
class SyncScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    private val workManager get() = WorkManager.getInstance(context)
    private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** @param immediate skip the short delay that batches a burst of edits into one sync. */
    fun requestSync(immediate: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInitialDelay(if (immediate) 0 else DEBOUNCE_SECONDS, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // KEEP: a queued or running sync already covers these changes (the worker loops while changes keep coming).
        workManager.enqueueUniqueWork(ONE_TIME, ExistingWorkPolicy.KEEP, request)
    }

    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS).setConstraints(constraints).build()
        workManager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel() {
        workManager.cancelUniqueWork(ONE_TIME)
        workManager.cancelUniqueWork(PERIODIC)
    }

    private companion object {
        const val ONE_TIME = "sync"
        const val PERIODIC = "sync-periodic"
        const val DEBOUNCE_SECONDS = 5L
    }
}

/** Watches the synced tables and requests a sync whenever they change while someone is signed in. */
@Singleton
class SyncTrigger @Inject constructor(
    private val db: ReelVaultDatabase,
    private val firebase: FirebaseProvider,
    private val scheduler: SyncScheduler,
) {
    private val observer = object : InvalidationTracker.Observer(arrayOf("items", "collections", "collection_items", "item_tags")) {
        override fun onInvalidated(tables: Set<String>) {
            if (firebase.auth?.currentUser != null) scheduler.requestSync()
        }
    }

    /** Call once from Application.onCreate. */
    fun start() {
        if (!firebase.isConfigured) return
        db.invalidationTracker.addObserver(observer)
        if (firebase.auth?.currentUser != null) {
            scheduler.requestSync(immediate = true)
            scheduler.schedulePeriodic()
        }
    }
}
