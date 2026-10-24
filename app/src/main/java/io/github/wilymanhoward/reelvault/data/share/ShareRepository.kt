package io.github.wilymanhoward.reelvault.data.share

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import io.github.wilymanhoward.reelvault.BuildConfig
import io.github.wilymanhoward.reelvault.data.firebase.FirebaseProvider
import io.github.wilymanhoward.reelvault.data.local.ReelVaultDatabase
import io.github.wilymanhoward.reelvault.domain.model.Platform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/** One video in a shared collection snapshot. */
data class SharedItem(
    val url: String,
    val platform: Platform,
    val title: String?,
    val caption: String?,
    val authorName: String?,
    val thumbnailUrl: String?,
)

/** The public snapshot at `shared/{shareId}`. */
data class SharedCollection(
    val shareId: String,
    val name: String,
    val emoji: String?,
    val colorArgb: Long?,
    val items: List<SharedItem>,
)

/** Thrown when sharing needs an account (anonymous is enough) and nobody is signed in. */
class NotSignedInException : IllegalStateException("Sign in (or turn on sync) in Settings to share collections")

/**
 * Publishes collections as public snapshots in `shared/{shareId}`, readable by anyone with the link
 * and writable only by the owner (see firestore.rules). The snapshot is a copy: publishing again
 * refreshes it, stopping deletes it.
 */
@Singleton
class ShareRepository @Inject constructor(
    private val firebase: FirebaseProvider,
    private val db: ReelVaultDatabase,
) {
    private val collections = db.collectionDao()

    val isAvailable: Boolean get() = firebase.isConfigured

    /** The public link for [shareId]. */
    fun linkFor(shareId: String) = "https://${BuildConfig.SHARE_HOST}/c/$shareId"

    /**
     * Publishes (or refreshes) the snapshot of [collectionId] and returns its link.
     * The share id is kept on the collection, so the link stays the same across refreshes and devices.
     */
    suspend fun publish(collectionId: String): String {
        val firestore = firebase.firestore ?: error("Firebase isn't configured in this build")
        val uid = firebase.auth?.currentUser?.uid ?: throw NotSignedInException()
        val collection = collections.getById(collectionId)?.takeUnless { it.deleted } ?: error("Collection not found")
        val items = collections.observeItems(collectionId).first().take(MAX_ITEMS)
        val snapshot = mapOf(
            "ownerUid" to uid,
            "name" to collection.name,
            "emoji" to collection.emoji,
            "colorArgb" to collection.colorArgb,
            "itemCount" to items.size,
            "updatedAt" to FieldValue.serverTimestamp(),
            "items" to items.map {
                mapOf(
                    "url" to it.url,
                    "platform" to it.platform.name,
                    "title" to it.title,
                    "caption" to it.caption?.take(MAX_CAPTION),
                    "authorName" to it.authorName,
                    "thumbnailUrl" to it.thumbnailUrl,
                )
            },
        )

        suspend fun write(shareId: String) = firestore.collection(SHARED).document(shareId).set(snapshot).await()
        var shareId = collection.shareId ?: newShareId()
        try {
            write(shareId)
        } catch (e: FirebaseFirestoreException) {
            // The id came from another account's share (e.g. signed into a different account since): start a new link.
            if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED || collection.shareId == null) throw e
            shareId = newShareId()
            write(shareId)
        }
        if (shareId != collection.shareId) {
            collections.upsert(collection.copy(shareId = shareId, updatedAt = System.currentTimeMillis()))
        }
        return linkFor(shareId)
    }

    /** Deletes the public snapshot; the old link then shows "no longer shared". */
    suspend fun stopSharing(collectionId: String) {
        val collection = collections.getById(collectionId) ?: return
        val shareId = collection.shareId ?: return
        firebase.firestore?.collection(SHARED)?.document(shareId)?.delete()?.await()
        collections.upsert(collection.copy(shareId = null, updatedAt = System.currentTimeMillis()))
    }

    /** Reads a shared snapshot (no sign-in needed). Null when it doesn't exist (any more). */
    suspend fun fetch(shareId: String): SharedCollection? {
        val firestore = firebase.firestore ?: error("Firebase isn't configured in this build")
        val doc = firestore.collection(SHARED).document(shareId).get().await()
        if (!doc.exists()) return null
        @Suppress("UNCHECKED_CAST")
        val items = (doc.get("items") as? List<Map<String, Any?>>).orEmpty().mapNotNull { m ->
            val url = (m["url"] as? String)?.takeIf { it.startsWith("https://") || it.startsWith("http://") } ?: return@mapNotNull null
            SharedItem(
                url = url,
                platform = Platform.entries.firstOrNull { it.name == m["platform"] } ?: Platform.OTHER,
                title = m["title"] as? String,
                caption = m["caption"] as? String,
                authorName = m["authorName"] as? String,
                thumbnailUrl = m["thumbnailUrl"] as? String,
            )
        }
        return SharedCollection(
            shareId = shareId,
            name = doc.getString("name") ?: "Shared collection",
            emoji = doc.getString("emoji"),
            colorArgb = doc.getLong("colorArgb"),
            items = items,
        )
    }

    private fun newShareId(): String {
        val random = SecureRandom()
        return (1..SHARE_ID_LENGTH).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
    }

    private companion object {
        const val SHARED = "shared"
        // 12 random base-62 chars ≈ 71 bits: unguessable, still short enough to read out.
        const val SHARE_ID_LENGTH = 12
        const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        // Keeps the snapshot well under Firestore's 1 MiB document limit.
        const val MAX_ITEMS = 500
        const val MAX_CAPTION = 500
    }
}
