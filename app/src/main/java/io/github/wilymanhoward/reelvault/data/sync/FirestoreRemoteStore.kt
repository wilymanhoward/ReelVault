package io.github.wilymanhoward.reelvault.data.sync

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import io.github.wilymanhoward.reelvault.data.firebase.FirebaseProvider
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

/**
 * [RemoteStore] on Cloud Firestore. Every write also sets `serverUpdatedAt` to the server's clock,
 * which pulls use as their cursor, so a device with a wrong clock can't make others miss changes.
 */
class FirestoreRemoteStore @Inject constructor(private val firebase: FirebaseProvider) : RemoteStore {

    private val db: FirebaseFirestore get() = firebase.firestore ?: error("Firebase isn't configured")

    override suspend fun pullItems(uid: String, sinceServerMs: Long) = pull(uid, ITEMS, sinceServerMs, ::toItem)
    override suspend fun pullCollections(uid: String, sinceServerMs: Long) = pull(uid, COLLECTIONS, sinceServerMs, ::toCollection)

    override suspend fun pushItems(uid: String, items: List<RemoteItem>) = push(uid, ITEMS, items, { it.id }) { item ->
        mapOf(
            "url" to item.url,
            "normalizedUrl" to item.normalizedUrl,
            "platform" to item.platform,
            "title" to item.title,
            "caption" to item.caption,
            "authorName" to item.authorName,
            "authorUrl" to item.authorUrl,
            "thumbnailUrl" to item.thumbnailUrl,
            "notes" to item.notes,
            "imageLabels" to item.imageLabels,
            "isFavorite" to item.isFavorite,
            "isPinned" to item.isPinned,
            "createdAt" to item.createdAt,
            "updatedAt" to item.updatedAt,
            "deleted" to item.deleted,
            "collectionIds" to item.collectionIds,
            "tags" to item.tags.map { mapOf("name" to it.name, "source" to it.source) },
        )
    }

    override suspend fun pushCollections(uid: String, collections: List<RemoteCollection>) = push(uid, COLLECTIONS, collections, { it.id }) { c ->
        mapOf(
            "name" to c.name,
            "emoji" to c.emoji,
            "colorArgb" to c.colorArgb,
            "coverItemId" to c.coverItemId,
            "sortOrder" to c.sortOrder,
            "shareId" to c.shareId,
            "createdAt" to c.createdAt,
            "updatedAt" to c.updatedAt,
            "deleted" to c.deleted,
        )
    }

    private suspend fun <T> pull(uid: String, collection: String, since: Long, parse: (DocumentSnapshot) -> T?): List<Pulled<T>> {
        val snapshot = db.collection("users").document(uid).collection(collection)
            .whereGreaterThan(SERVER_UPDATED_AT, Timestamp(Date(since)))
            .orderBy(SERVER_UPDATED_AT, Query.Direction.ASCENDING)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            val serverTime = doc.getTimestamp(SERVER_UPDATED_AT)?.toDate()?.time ?: return@mapNotNull null
            parse(doc)?.let { Pulled(it, serverTime) }
        }
    }

    private suspend fun <T> push(uid: String, collection: String, values: List<T>, id: (T) -> String, toMap: (T) -> Map<String, Any?>) {
        val ref = db.collection("users").document(uid).collection(collection)
        values.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.set(ref.document(id(it)), toMap(it) + (SERVER_UPDATED_AT to FieldValue.serverTimestamp())) }
            batch.commit().await()
        }
    }

    // Unknown or malformed docs (e.g. written by a newer app version) are skipped, not fatal.
    private fun toItem(doc: DocumentSnapshot): RemoteItem? = runCatching {
        @Suppress("UNCHECKED_CAST")
        val tags = (doc.get("tags") as? List<Map<String, Any?>>).orEmpty().mapNotNull { t ->
            val name = t["name"] as? String ?: return@mapNotNull null
            RemoteTag(name, t["source"] as? String ?: "MANUAL")
        }
        RemoteItem(
            id = doc.id,
            url = doc.getString("url")!!,
            normalizedUrl = doc.getString("normalizedUrl")!!,
            platform = doc.getString("platform") ?: "OTHER",
            title = doc.getString("title"),
            caption = doc.getString("caption"),
            authorName = doc.getString("authorName"),
            authorUrl = doc.getString("authorUrl"),
            thumbnailUrl = doc.getString("thumbnailUrl"),
            notes = doc.getString("notes").orEmpty(),
            imageLabels = doc.getString("imageLabels").orEmpty(),
            isFavorite = doc.getBoolean("isFavorite") ?: false,
            isPinned = doc.getBoolean("isPinned") ?: false,
            createdAt = doc.getLong("createdAt") ?: 0,
            updatedAt = doc.getLong("updatedAt") ?: 0,
            deleted = doc.getBoolean("deleted") ?: false,
            collectionIds = (doc.get("collectionIds") as? List<*>).orEmpty().filterIsInstance<String>(),
            tags = tags,
        )
    }.getOrNull()

    private fun toCollection(doc: DocumentSnapshot): RemoteCollection? = runCatching {
        RemoteCollection(
            id = doc.id,
            name = doc.getString("name")!!,
            emoji = doc.getString("emoji"),
            colorArgb = doc.getLong("colorArgb"),
            coverItemId = doc.getString("coverItemId"),
            sortOrder = (doc.getLong("sortOrder") ?: 0).toInt(),
            shareId = doc.getString("shareId"),
            createdAt = doc.getLong("createdAt") ?: 0,
            updatedAt = doc.getLong("updatedAt") ?: 0,
            deleted = doc.getBoolean("deleted") ?: false,
        )
    }.getOrNull()

    private companion object {
        const val ITEMS = "items"
        const val COLLECTIONS = "collections"
        const val SERVER_UPDATED_AT = "serverUpdatedAt"
        // Firestore allows 500 writes per batch; stay below it.
        const val BATCH_LIMIT = 400
    }
}
