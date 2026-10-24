package io.github.wilymanhoward.reelvault.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import io.github.wilymanhoward.reelvault.domain.model.PreviewState
import kotlinx.coroutines.flow.Flow

/** Access to saved items. Queries skip soft-deleted rows unless they say otherwise. */
@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE deleted = 0 ORDER BY isPinned DESC, createdAt DESC")
    fun observeItems(): Flow<List<SavedItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id AND deleted = 0")
    fun observeItem(id: String): Flow<SavedItemEntity?>

    /** Includes soft-deleted rows. */
    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: String): SavedItemEntity?

    /** Includes soft-deleted rows, so a re-saved link can revive its old row. */
    @Query("SELECT * FROM items WHERE normalizedUrl = :normalizedUrl")
    suspend fun findByNormalizedUrl(normalizedUrl: String): SavedItemEntity?

    @Query("SELECT * FROM items WHERE deleted = 0")
    suspend fun getAllActive(): List<SavedItemEntity>

    /** Rows changed after [since], soft-deleted ones included (they sync as tombstones). */
    @Query("SELECT * FROM items WHERE updatedAt > :since")
    suspend fun getChangedSince(since: Long): List<SavedItemEntity>

    @Upsert
    suspend fun upsert(item: SavedItemEntity)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun hardDelete(id: String)

    /** Bumps `updatedAt` so sync picks up changes stored in other tables (collections, tags). */
    @Query("UPDATE items SET updatedAt = :now WHERE id = :id")
    suspend fun touch(id: String, now: Long)

    /** Writes only preview columns, so edits made while a preview was fetching (notes, tags) survive. */
    @Query(
        """
        UPDATE items SET title = :title, caption = :caption, authorName = :authorName, authorUrl = :authorUrl,
            thumbnailUrl = :thumbnailUrl, localThumbnailPath = :localThumbnailPath, previewState = :state, updatedAt = :now
        WHERE id = :id
        """,
    )
    suspend fun updatePreview(
        id: String,
        title: String?,
        caption: String?,
        authorName: String?,
        authorUrl: String?,
        thumbnailUrl: String?,
        localThumbnailPath: String?,
        state: PreviewState,
        now: Long,
    )

    @Query("UPDATE items SET previewState = :state WHERE id = :id")
    suspend fun setPreviewState(id: String, state: PreviewState)

    @Query("UPDATE items SET imageLabels = :labels WHERE id = :id")
    suspend fun setImageLabels(id: String, labels: String)

    @Query("UPDATE items SET tagsText = :tagsText, updatedAt = :now WHERE id = :id")
    suspend fun setTagsText(id: String, tagsText: String, now: Long)

    @Query("UPDATE items SET notes = :notes, updatedAt = :now WHERE id = :id")
    suspend fun setNotes(id: String, notes: String, now: Long)

    @Query("UPDATE items SET isFavorite = :favorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, now: Long)

    @Query("UPDATE items SET isPinned = :pinned, updatedAt = :now WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, now: Long)

    @Query("UPDATE items SET deleted = :deleted, updatedAt = :now WHERE id IN (:ids)")
    suspend fun setDeleted(ids: List<String>, deleted: Boolean, now: Long)

    /** @param match an FTS4 MATCH expression, already escaped by the caller. */
    @Query(
        """
        SELECT items.* FROM items
        JOIN items_fts ON items.rowid = items_fts.rowid
        WHERE items_fts MATCH :match AND items.deleted = 0
        ORDER BY items.isPinned DESC, items.createdAt DESC
        """,
    )
    fun search(match: String): Flow<List<SavedItemEntity>>
}

/** A collection with what its card needs: item count and cover image. */
data class CollectionWithStats(
    @Embedded val collection: CollectionEntity,
    val itemCount: Int,
    val coverLocalPath: String?,
    val coverThumbnailUrl: String?,
)

/** Access to collections and their membership. */
@Dao
interface CollectionDao {
    // The cover is the chosen item if it still exists, else the first item added.
    @Query(
        """
        SELECT c.*,
            (SELECT COUNT(*) FROM collection_items ci JOIN items i ON i.id = ci.itemId
                WHERE ci.collectionId = c.id AND i.deleted = 0) AS itemCount,
            cov.localThumbnailPath AS coverLocalPath,
            cov.thumbnailUrl AS coverThumbnailUrl
        FROM collections c
        LEFT JOIN items cov ON cov.id = COALESCE(
            (SELECT i.id FROM items i WHERE i.id = c.coverItemId AND i.deleted = 0),
            (SELECT i.id FROM collection_items ci JOIN items i ON i.id = ci.itemId
                WHERE ci.collectionId = c.id AND i.deleted = 0 ORDER BY ci.addedAt LIMIT 1))
        WHERE c.deleted = 0
        ORDER BY c.sortOrder, c.createdAt
        """,
    )
    fun observeCollectionsWithStats(): Flow<List<CollectionWithStats>>

    @Query("SELECT * FROM collections WHERE id = :id AND deleted = 0")
    fun observeCollection(id: String): Flow<CollectionEntity?>

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getById(id: String): CollectionEntity?

    @Query("SELECT * FROM collections WHERE deleted = 0 ORDER BY sortOrder, createdAt")
    suspend fun getAllActive(): List<CollectionEntity>

    @Query("SELECT * FROM collections WHERE updatedAt > :since")
    suspend fun getChangedSince(since: Long): List<CollectionEntity>

    @Upsert
    suspend fun upsert(collection: CollectionEntity)

    @Upsert
    suspend fun upsertAll(collections: List<CollectionEntity>)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM collections")
    suspend fun maxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addItem(ref: CollectionItemCrossRef)

    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId AND itemId = :itemId")
    suspend fun removeItem(collectionId: String, itemId: String)

    @Query("DELETE FROM collection_items WHERE itemId = :itemId")
    suspend fun removeAllFor(itemId: String)

    @Query("SELECT collectionId FROM collection_items WHERE itemId = :itemId")
    fun observeCollectionIdsFor(itemId: String): Flow<List<String>>

    @Query("SELECT collectionId FROM collection_items WHERE itemId = :itemId")
    suspend fun getCollectionIdsFor(itemId: String): List<String>

    @Query("SELECT itemId FROM collection_items WHERE collectionId = :collectionId")
    fun observeItemIds(collectionId: String): Flow<List<String>>

    @Query(
        """
        SELECT items.* FROM items
        JOIN collection_items ON collection_items.itemId = items.id
        WHERE collection_items.collectionId = :collectionId AND items.deleted = 0
        ORDER BY items.isPinned DESC, collection_items.addedAt DESC
        """,
    )
    fun observeItems(collectionId: String): Flow<List<SavedItemEntity>>
}

/** A tag name and how many saved items carry it. */
data class TagCount(val name: String, val count: Int)

/** Access to item tags and auto-tagging rules. */
@Dao
interface TagDao {
    @Query("SELECT * FROM item_tags WHERE itemId = :itemId ORDER BY source DESC, name COLLATE NOCASE")
    fun observeTags(itemId: String): Flow<List<ItemTagEntity>>

    @Query("SELECT * FROM item_tags WHERE itemId = :itemId")
    suspend fun getTags(itemId: String): List<ItemTagEntity>

    @Query(
        """
        SELECT t.name AS name, COUNT(*) AS count FROM item_tags t JOIN items i ON i.id = t.itemId
        WHERE i.deleted = 0 GROUP BY t.name ORDER BY t.name COLLATE NOCASE
        """,
    )
    fun observeTagCounts(): Flow<List<TagCount>>

    @Query("SELECT itemId FROM item_tags WHERE name = :name")
    fun observeItemIdsWithTag(name: String): Flow<List<String>>

    @Query("SELECT DISTINCT itemId FROM item_tags WHERE name = :name")
    suspend fun getItemIdsWithTag(name: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(tags: List<ItemTagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(tags: List<ItemTagEntity>)

    @Query("DELETE FROM item_tags WHERE itemId = :itemId AND source = 'AUTO'")
    suspend fun deleteAutoTags(itemId: String)

    @Query("DELETE FROM item_tags WHERE itemId = :itemId")
    suspend fun deleteAllFor(itemId: String)

    @Query("DELETE FROM item_tags WHERE itemId = :itemId AND name = :name")
    suspend fun delete(itemId: String, name: String)

    /**
     * Copies [old] to [new] on every item (merging with items that already have [new]). Pair with
     * [deleteEverywhere]. The copy is MANUAL, so re-running auto-tagging doesn't undo a rename.
     */
    @Query("INSERT OR IGNORE INTO item_tags (itemId, name, source) SELECT itemId, :new, 'MANUAL' FROM item_tags WHERE name = :old")
    suspend fun copyTag(old: String, new: String)

    @Query("DELETE FROM item_tags WHERE name = :name")
    suspend fun deleteEverywhere(name: String)

    @Query("SELECT * FROM tag_rules ORDER BY category COLLATE NOCASE")
    fun observeRules(): Flow<List<TagRuleEntity>>

    @Query("SELECT * FROM tag_rules")
    suspend fun getRules(): List<TagRuleEntity>

    @Upsert
    suspend fun upsertRule(rule: TagRuleEntity)

    @Insert
    suspend fun insertRules(rules: List<TagRuleEntity>)

    @Query("DELETE FROM tag_rules WHERE id = :id")
    suspend fun deleteRule(id: Long)

    @Query("DELETE FROM tag_rules")
    suspend fun deleteAllRules()
}
