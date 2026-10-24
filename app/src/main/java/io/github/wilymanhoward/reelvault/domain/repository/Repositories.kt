package io.github.wilymanhoward.reelvault.domain.repository

import io.github.wilymanhoward.reelvault.domain.model.Platform
import io.github.wilymanhoward.reelvault.domain.model.SavedItem
import io.github.wilymanhoward.reelvault.domain.model.SearchFilter
import io.github.wilymanhoward.reelvault.domain.model.Tag
import io.github.wilymanhoward.reelvault.domain.model.TagRule
import io.github.wilymanhoward.reelvault.domain.model.TagSummary
import io.github.wilymanhoward.reelvault.domain.model.VaultCollection
import kotlinx.coroutines.flow.Flow

/** Saved items, backed by Room. */
interface ItemRepository {
    fun observeItems(): Flow<List<SavedItem>>
    fun observeItem(id: String): Flow<SavedItem?>

    /** Full-text search over title, caption, author, tags, notes and URL, narrowed by [filter]. */
    fun search(query: String, filter: SearchFilter): Flow<List<SavedItem>>

    /** The saved (not deleted) item with this canonical key, if any. */
    suspend fun findByNormalizedUrl(normalizedUrl: String): SavedItem?

    /** Stores a new item, or revives the soft-deleted one with the same [normalizedUrl]. */
    suspend fun save(url: String, normalizedUrl: String, platform: Platform): SavedItem

    suspend fun setNotes(id: String, notes: String)
    suspend fun setFavorite(id: String, favorite: Boolean)
    suspend fun setPinned(id: String, pinned: Boolean)

    /** Soft-deletes, so [restore] can undo it and sync can propagate it. */
    suspend fun delete(ids: Collection<String>)
    suspend fun restore(ids: Collection<String>)
}

/** Collections and which items they hold. */
interface CollectionRepository {
    /** Collections in the user's order, with item counts and covers. */
    fun observeCollections(): Flow<List<VaultCollection>>
    fun observeCollection(id: String): Flow<VaultCollection?>
    fun observeItems(collectionId: String): Flow<List<SavedItem>>
    fun observeCollectionIdsFor(itemId: String): Flow<Set<String>>

    suspend fun create(name: String, emoji: String? = null, colorArgb: Long? = null): VaultCollection
    suspend fun update(id: String, name: String, emoji: String?, colorArgb: Long?)
    /** @param itemId the cover item, or null to go back to "first item". */
    suspend fun setCover(id: String, itemId: String?)
    suspend fun delete(id: String)
    /** Stores the order of collections as given. */
    suspend fun reorder(orderedIds: List<String>)

    suspend fun setMembership(itemId: String, collectionId: String, member: Boolean)
    suspend fun addItems(itemIds: Collection<String>, collectionId: String)
}

/** Item tags, auto-tagging and its rules. */
interface TagRepository {
    fun observeTags(itemId: String): Flow<List<Tag>>
    fun observeAllTags(): Flow<List<TagSummary>>
    fun observeRules(): Flow<List<TagRule>>

    suspend fun addTag(itemIds: Collection<String>, name: String)
    suspend fun removeTag(itemId: String, name: String)
    suspend fun renameTag(old: String, new: String)
    suspend fun deleteTag(name: String)

    /** Recomputes [itemId]'s auto tags from its text, image labels and the current rules. Manual tags stay. */
    suspend fun applyAutoTags(itemId: String)
    /** [applyAutoTags] for every saved item, e.g. after editing rules. */
    suspend fun reapplyAutoTags()

    suspend fun saveRule(rule: TagRule)
    suspend fun deleteRule(id: Long)
    suspend fun resetRules()
}

/** Follows a short link's redirects to the real video URL. */
fun interface ShortLinkResolver {
    /** The final URL, or null when the network request fails. */
    suspend fun resolve(url: String): String?
}
