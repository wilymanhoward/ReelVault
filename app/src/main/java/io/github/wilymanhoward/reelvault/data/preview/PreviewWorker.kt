package io.github.wilymanhoward.reelvault.data.preview

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.wilymanhoward.reelvault.data.local.ItemDao
import io.github.wilymanhoward.reelvault.domain.model.PreviewState
import io.github.wilymanhoward.reelvault.domain.repository.PreviewResolver
import io.github.wilymanhoward.reelvault.domain.repository.PreviewScheduler
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Fetches an item's preview metadata and caches its thumbnail. Also used to refresh a preview
 * whose thumbnail URL has expired (403/404).
 */
@HiltWorker
class PreviewWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val items: ItemDao,
    private val resolver: PreviewResolver,
    private val thumbnails: ThumbnailCache,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_ITEM_ID) ?: return Result.failure()
        val item = items.getById(id)?.takeUnless { it.deleted } ?: return Result.success()
        return try {
            val meta = resolver.resolve(item.url, item.normalizedUrl, item.platform)
            if (meta == null) {
                // Keep an earlier good preview; otherwise the card shows the platform fallback.
                if (item.previewState != PreviewState.READY) items.setPreviewState(id, PreviewState.FAILED)
                return Result.success()
            }
            val localPath = meta.thumbnailUrl?.let { thumbnails.store(id, it) } ?: item.localThumbnailPath
            items.updatePreview(
                id = id,
                title = meta.title,
                caption = meta.caption,
                authorName = meta.authorName,
                authorUrl = meta.authorUrl,
                thumbnailUrl = meta.thumbnailUrl,
                localThumbnailPath = localPath,
                state = PreviewState.READY,
                now = System.currentTimeMillis(),
            )
            Result.success()
        } catch (e: IOException) {
            if (runAttemptCount < MAX_ATTEMPTS) return Result.retry()
            if (item.previewState == PreviewState.PENDING) items.setPreviewState(id, PreviewState.FAILED)
            Result.success()
        }
    }

    companion object {
        const val KEY_ITEM_ID = "itemId"
        private const val MAX_ATTEMPTS = 4
    }
}

/** [PreviewScheduler] backed by WorkManager: needs a network, retries with exponential backoff. */
class WorkManagerPreviewScheduler @Inject constructor(@ApplicationContext private val context: Context) : PreviewScheduler {
    override fun schedule(itemId: String) {
        val request = OneTimeWorkRequestBuilder<PreviewWorker>()
            .setInputData(workDataOf(PreviewWorker.KEY_ITEM_ID to itemId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // KEEP: a fetch already queued for this item will do; finished work doesn't block a new one.
        WorkManager.getInstance(context).enqueueUniqueWork("preview-$itemId", ExistingWorkPolicy.KEEP, request)
    }
}
