package io.github.wilymanhoward.reelvault.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Access to saved items. Queries skip soft-deleted rows unless they say otherwise. */
@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE deleted = 0 ORDER BY isPinned DESC, createdAt DESC")
    fun observeItems(): Flow<List<SavedItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id AND deleted = 0")
    fun observeItem(id: String): Flow<SavedItemEntity?>

    /** Includes soft-deleted rows, so a re-saved link can revive its old row. */
    @Query("SELECT * FROM items WHERE normalizedUrl = :normalizedUrl")
    suspend fun findByNormalizedUrl(normalizedUrl: String): SavedItemEntity?

    @Upsert
    suspend fun upsert(item: SavedItemEntity)

    /** Bumps `updatedAt` so sync picks up changes stored in other tables (collections, tags). */
    @Query("UPDATE items SET updatedAt = :now WHERE id = :id")
    suspend fun touch(id: String, now: Long)

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

/** Access to collections and their membership. */
@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections WHERE deleted = 0 ORDER BY sortOrder, createdAt")
    fun observeCollections(): Flow<List<CollectionEntity>>

    @Upsert
    suspend fun upsert(collection: CollectionEntity)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM collections")
    suspend fun maxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addItem(ref: CollectionItemCrossRef)

    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId AND itemId = :itemId")
    suspend fun removeItem(collectionId: String, itemId: String)

    @Query("SELECT collectionId FROM collection_items WHERE itemId = :itemId")
    fun observeCollectionIdsFor(itemId: String): Flow<List<String>>

    @Query(
        """
        SELECT items.* FROM items
        JOIN collection_items ON collection_items.itemId = items.id
        WHERE collection_items.collectionId = :collectionId AND items.deleted = 0
        ORDER BY collection_items.addedAt DESC
        """,
    )
    fun observeItems(collectionId: String): Flow<List<SavedItemEntity>>
}

/** Access to item tags and auto-tagging rules. */
@Dao
interface TagDao {
    @Query("SELECT * FROM item_tags WHERE itemId = :itemId ORDER BY name")
    fun observeTags(itemId: String): Flow<List<ItemTagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<ItemTagEntity>)

    @Query("SELECT * FROM tag_rules ORDER BY category")
    fun observeRules(): Flow<List<TagRuleEntity>>
}
