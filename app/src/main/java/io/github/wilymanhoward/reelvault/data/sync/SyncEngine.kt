package io.github.wilymanhoward.reelvault.data.sync

import androidx.room.withTransaction
import io.github.wilymanhoward.reelvault.data.local.CollectionEntity
import io.github.wilymanhoward.reelvault.data.local.CollectionItemCrossRef
import io.github.wilymanhoward.reelvault.data.local.ItemTagEntity
import io.github.wilymanhoward.reelvault.data.local.ReelVaultDatabase
import io.github.wilymanhoward.reelvault.data.local.SavedItemEntity
import io.github.wilymanhoward.reelvault.domain.model.Platform
import io.github.wilymanhoward.reelvault.domain.model.PreviewState
import io.github.wilymanhoward.reelvault.domain.model.TagSource
import io.github.wilymanhoward.reelvault.domain.repository.PreviewScheduler
import javax.inject.Inject

/**
 * Two-way sync between Room (the source of truth for the UI) and the cloud.
 *
 * Pull first, then push. Conflicts are last-write-wins on the device-set `updatedAt`. Deletes are
 * soft (`deleted = true`) so they sync as tombstones. If two devices saved the same video offline
 * (two ids, one normalized URL), every device keeps the smaller id and tombstones the other, so
 * they all converge on the same record.
 */
class SyncEngine @Inject constructor(
    private val db: ReelVaultDatabase,
    private val remote: RemoteStore,
    private val state: SyncStateStore,
    private val previews: PreviewScheduler,
) {
    /** Replaceable in tests. */
    internal var clock: () -> Long = System::currentTimeMillis

    private val items = db.itemDao()
    private val collections = db.collectionDao()
    private val tags = db.tagDao()

    /** Runs one sync for [uid]. Returns true if local changes arrived while it ran (sync again). */
    suspend fun sync(uid: String): Boolean {
        var cursors = state.load(uid)
        val tombstones = mutableListOf<RemoteItem>()
        val needPreview = mutableListOf<String>()

        val pulledCollections = remote.pullCollections(uid, cursors.pulledCollections)
        db.withTransaction { pulledCollections.forEach { applyCollection(it.value) } }

        val pulledItems = remote.pullItems(uid, cursors.pulledItems)
        db.withTransaction { pulledItems.forEach { applyItem(it.value, tombstones, needPreview) } }

        cursors = cursors.copy(
            pulledCollections = pulledCollections.maxOfOrNull { it.serverUpdatedAt } ?: cursors.pulledCollections,
            pulledItems = pulledItems.maxOfOrNull { it.serverUpdatedAt } ?: cursors.pulledItems,
        )

        val changedCollections = collections.getChangedSince(cursors.pushed)
        val changedItems = items.getChangedSince(cursors.pushed)
        if (changedCollections.isNotEmpty()) remote.pushCollections(uid, changedCollections.map { it.toRemote() })
        val itemsToPush = changedItems.map { it.toRemote() } + tombstones
        if (itemsToPush.isNotEmpty()) remote.pushItems(uid, itemsToPush)

        val pushedUpTo = (changedCollections.map { it.updatedAt } + changedItems.map { it.updatedAt }).maxOrNull()
        cursors = cursors.copy(pushed = maxOf(cursors.pushed, pushedUpTo ?: 0))
        state.save(uid, cursors, syncedAt = clock())

        needPreview.forEach(previews::schedule)
        return items.getChangedSince(cursors.pushed).isNotEmpty() || collections.getChangedSince(cursors.pushed).isNotEmpty()
    }

    private suspend fun applyCollection(remote: RemoteCollection) {
        val local = collections.getById(remote.id)
        if (local != null && local.updatedAt >= remote.updatedAt) return
        collections.upsert(
            CollectionEntity(
                id = remote.id,
                name = remote.name,
                emoji = remote.emoji,
                colorArgb = remote.colorArgb,
                coverItemId = remote.coverItemId,
                sortOrder = remote.sortOrder,
                shareId = remote.shareId,
                createdAt = remote.createdAt,
                updatedAt = remote.updatedAt,
                deleted = remote.deleted,
            ),
        )
    }

    private suspend fun applyItem(remote: RemoteItem, tombstones: MutableList<RemoteItem>, needPreview: MutableList<String>) {
        val local = items.getById(remote.id)
        if (local != null && local.updatedAt >= remote.updatedAt) return
        // A deletion of something this device never had: nothing to do (and nothing to store).
        if (local == null && remote.deleted) return

        if (!remote.deleted) {
            val duplicate = items.findByNormalizedUrl(remote.normalizedUrl)?.takeIf { it.id != remote.id }
            if (duplicate != null) {
                if (duplicate.id < remote.id) {
                    tombstones += remote.copy(deleted = true, updatedAt = clock())
                    return
                }
                items.hardDelete(duplicate.id)
                tombstones += duplicate.toRemote(collectionIds = emptyList(), tags = emptyList()).copy(deleted = true, updatedAt = clock())
            }
        }

        val hasPreview = remote.title != null || remote.caption != null || remote.thumbnailUrl != null
        items.upsert(
            SavedItemEntity(
                id = remote.id,
                url = remote.url,
                normalizedUrl = remote.normalizedUrl,
                platform = Platform.entries.firstOrNull { it.name == remote.platform } ?: Platform.OTHER,
                title = remote.title,
                caption = remote.caption,
                authorName = remote.authorName,
                authorUrl = remote.authorUrl,
                thumbnailUrl = remote.thumbnailUrl,
                // The cached copy is still right if the thumbnail URL didn't change.
                localThumbnailPath = local?.localThumbnailPath?.takeIf { local.thumbnailUrl == remote.thumbnailUrl },
                notes = remote.notes,
                tagsText = remote.tags.map { it.name }.sortedWith(String.CASE_INSENSITIVE_ORDER).joinToString(" "),
                imageLabels = remote.imageLabels,
                previewState = if (hasPreview) PreviewState.READY else PreviewState.PENDING,
                isFavorite = remote.isFavorite,
                isPinned = remote.isPinned,
                createdAt = remote.createdAt,
                updatedAt = remote.updatedAt,
                deleted = remote.deleted,
            ),
        )
        collections.removeAllFor(remote.id)
        remote.collectionIds.filter { collections.getById(it) != null }.forEach {
            collections.addItem(CollectionItemCrossRef(it, remote.id, addedAt = remote.updatedAt))
        }
        tags.deleteAllFor(remote.id)
        tags.insertIgnore(remote.tags.map { ItemTagEntity(remote.id, it.name, if (it.source == "AUTO") TagSource.AUTO else TagSource.MANUAL) })

        // Fetch a thumbnail for this device: new here, or its image changed.
        val thumbnailMissing = remote.thumbnailUrl != null && (local?.localThumbnailPath == null || local.thumbnailUrl != remote.thumbnailUrl)
        if (!remote.deleted && (!hasPreview || thumbnailMissing)) needPreview += remote.id
    }

    private suspend fun SavedItemEntity.toRemote(): RemoteItem = toRemote(
        collectionIds = collections.getCollectionIdsFor(id),
        tags = tags.getTags(id).map { RemoteTag(it.name, it.source.name) },
    )

    private fun SavedItemEntity.toRemote(collectionIds: List<String>, tags: List<RemoteTag>) = RemoteItem(
        id = id,
        url = url,
        normalizedUrl = normalizedUrl,
        platform = platform.name,
        title = title,
        caption = caption,
        authorName = authorName,
        authorUrl = authorUrl,
        thumbnailUrl = thumbnailUrl,
        notes = notes,
        imageLabels = imageLabels,
        isFavorite = isFavorite,
        isPinned = isPinned,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deleted = deleted,
        collectionIds = collectionIds,
        tags = tags,
    )

    private fun CollectionEntity.toRemote() = RemoteCollection(
        id = id,
        name = name,
        emoji = emoji,
        colorArgb = colorArgb,
        coverItemId = coverItemId,
        sortOrder = sortOrder,
        shareId = shareId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deleted = deleted,
    )
}
