package io.github.wilymanhoward.reelvault.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.wilymanhoward.reelvault.domain.model.Platform

/** Where a preview's metadata stands. */
enum class PreviewState { PENDING, READY, FAILED }

/** Whether a tag was added by the auto-tagger or by the user. */
enum class TagSource { AUTO, MANUAL }

/**
 * One saved video link.
 *
 * Ids are UUID strings so records created offline can sync to Firestore without collisions.
 * `updatedAt` and `deleted` (soft delete) drive last-write-wins sync. `normalizedUrl` is unique,
 * so saving a link that was soft-deleted revives that row instead of inserting a new one.
 *
 * @property tagsText space-separated tag names, copied from [ItemTagEntity] so FTS can search them.
 */
@Entity(tableName = "items", indices = [Index(value = ["normalizedUrl"], unique = true)])
data class SavedItemEntity(
    @PrimaryKey val id: String,
    val url: String,
    val normalizedUrl: String,
    val platform: Platform,
    val title: String? = null,
    val caption: String? = null,
    val authorName: String? = null,
    val authorUrl: String? = null,
    val thumbnailUrl: String? = null,
    val localThumbnailPath: String? = null,
    val notes: String = "",
    val tagsText: String = "",
    val previewState: PreviewState = PreviewState.PENDING,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

/** Full-text index over the searchable columns of [SavedItemEntity]; Room keeps it in sync with triggers. */
@Fts4(contentEntity = SavedItemEntity::class)
@Entity(tableName = "items_fts")
data class SavedItemFts(
    val title: String?,
    val caption: String?,
    val authorName: String?,
    val notes: String,
    val tagsText: String,
    val url: String,
)

/**
 * A user-made group of items.
 *
 * @property coverItemId item whose thumbnail is the cover; null means "use the first item".
 * @property shareId id of the public snapshot in `shared/{shareId}` while the collection is shared.
 */
@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String? = null,
    val colorArgb: Long? = null,
    val coverItemId: String? = null,
    val sortOrder: Int = 0,
    val shareId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

/** Many-to-many link between collections and items. */
@Entity(
    tableName = "collection_items",
    primaryKeys = ["collectionId", "itemId"],
    foreignKeys = [
        ForeignKey(CollectionEntity::class, ["id"], ["collectionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(SavedItemEntity::class, ["id"], ["itemId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("itemId")],
)
data class CollectionItemCrossRef(
    val collectionId: String,
    val itemId: String,
    val addedAt: Long,
)

/** A tag on an item. */
@Entity(
    tableName = "item_tags",
    primaryKeys = ["itemId", "name"],
    foreignKeys = [ForeignKey(SavedItemEntity::class, ["id"], ["itemId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("name")],
)
data class ItemTagEntity(
    val itemId: String,
    val name: String,
    val source: TagSource,
)

/**
 * An editable auto-tagging rule: if any keyword appears in an item's text, the item gets [category].
 *
 * @property keywords comma-separated, matched case-insensitively.
 */
@Entity(tableName = "tag_rules")
data class TagRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val keywords: String,
    val enabled: Boolean = true,
)
