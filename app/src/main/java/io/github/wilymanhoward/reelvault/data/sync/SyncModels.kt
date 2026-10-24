package io.github.wilymanhoward.reelvault.data.sync

/** A tag as stored in Firestore. */
data class RemoteTag(val name: String, val source: String)

/**
 * An item document at `users/{uid}/items/{id}`. Memberships and tags live on the item, so one
 * write carries everything about it. Local-only fields (cached thumbnail path, FTS text) stay out.
 */
data class RemoteItem(
    val id: String,
    val url: String,
    val normalizedUrl: String,
    val platform: String,
    val title: String?,
    val caption: String?,
    val authorName: String?,
    val authorUrl: String?,
    val thumbnailUrl: String?,
    val notes: String,
    val imageLabels: String,
    val isFavorite: Boolean,
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean,
    val collectionIds: List<String>,
    val tags: List<RemoteTag>,
)

/** A collection document at `users/{uid}/collections/{id}`. */
data class RemoteCollection(
    val id: String,
    val name: String,
    val emoji: String?,
    val colorArgb: Long?,
    val coverItemId: String?,
    val sortOrder: Int,
    val shareId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean,
)

/** A pulled document with its server write time, which is the pull cursor. */
data class Pulled<T>(val value: T, val serverUpdatedAt: Long)

/** The cloud side of sync. Firestore in the app, a fake in tests. */
interface RemoteStore {
    /** Docs whose server write time is after [sinceServerMs], oldest first. */
    suspend fun pullItems(uid: String, sinceServerMs: Long): List<Pulled<RemoteItem>>
    suspend fun pullCollections(uid: String, sinceServerMs: Long): List<Pulled<RemoteCollection>>
    suspend fun pushItems(uid: String, items: List<RemoteItem>)
    suspend fun pushCollections(uid: String, collections: List<RemoteCollection>)
}
