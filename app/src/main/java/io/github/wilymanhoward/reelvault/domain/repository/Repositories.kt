package io.github.wilymanhoward.reelvault.domain.repository

import io.github.wilymanhoward.reelvault.domain.model.Platform
import io.github.wilymanhoward.reelvault.domain.model.SavedItem
import io.github.wilymanhoward.reelvault.domain.model.VaultCollection
import kotlinx.coroutines.flow.Flow

/** Saved items, backed by Room. */
interface ItemRepository {
    fun observeItems(): Flow<List<SavedItem>>

    /** The saved (not deleted) item with this canonical key, if any. */
    suspend fun findByNormalizedUrl(normalizedUrl: String): SavedItem?

    /** Stores a new item, or revives the soft-deleted one with the same [normalizedUrl]. */
    suspend fun save(url: String, normalizedUrl: String, platform: Platform): SavedItem
}

/** Collections and which items they hold. */
interface CollectionRepository {
    fun observeCollections(): Flow<List<VaultCollection>>
    fun observeCollectionIdsFor(itemId: String): Flow<Set<String>>
    suspend fun create(name: String): VaultCollection
    suspend fun setMembership(itemId: String, collectionId: String, member: Boolean)
}

/** Follows a short link's redirects to the real video URL. */
fun interface ShortLinkResolver {
    /** The final URL, or null when the network request fails. */
    suspend fun resolve(url: String): String?
}
