package io.github.wilymanhoward.reelvault.data.repository

import androidx.room.withTransaction
import io.github.wilymanhoward.reelvault.data.local.CollectionEntity
import io.github.wilymanhoward.reelvault.data.local.CollectionItemCrossRef
import io.github.wilymanhoward.reelvault.data.local.CollectionWithStats
import io.github.wilymanhoward.reelvault.data.local.ItemTagEntity
import io.github.wilymanhoward.reelvault.data.local.ReelVaultDatabase
import io.github.wilymanhoward.reelvault.data.local.SavedItemEntity
import io.github.wilymanhoward.reelvault.data.local.TagRuleEntity
import io.github.wilymanhoward.reelvault.domain.model.Platform
import io.github.wilymanhoward.reelvault.domain.model.SavedItem
import io.github.wilymanhoward.reelvault.domain.model.SearchFilter
import io.github.wilymanhoward.reelvault.domain.model.Tag
import io.github.wilymanhoward.reelvault.domain.model.TagRule
import io.github.wilymanhoward.reelvault.domain.model.TagSource
import io.github.wilymanhoward.reelvault.domain.model.TagSummary
import io.github.wilymanhoward.reelvault.domain.model.VaultCollection
import io.github.wilymanhoward.reelvault.domain.repository.CollectionRepository
import io.github.wilymanhoward.reelvault.domain.repository.ItemRepository
import io.github.wilymanhoward.reelvault.domain.repository.TagRepository
import io.github.wilymanhoward.reelvault.domain.tagging.AutoTagger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

private fun now() = System.currentTimeMillis()

/** [ItemRepository] on top of Room. */
class RoomItemRepository @Inject constructor(private val db: ReelVaultDatabase) : ItemRepository {
    private val dao = db.itemDao()

    override fun observeItems(): Flow<List<SavedItem>> = dao.observeItems().map { list -> list.map { it.toDomain() } }

    override fun observeItem(id: String): Flow<SavedItem?> = dao.observeItem(id).map { it?.toDomain() }

    override fun search(query: String, filter: SearchFilter): Flow<List<SavedItem>> {
        val base = ftsMatch(query)?.let(dao::search) ?: dao.observeItems()
        val inCollection = filter.collectionId?.let { id -> db.collectionDao().observeItemIds(id).map { it.toSet() } } ?: flowOf(null)
        val withTag = filter.tag?.let { name -> db.tagDao().observeItemIdsWithTag(name).map { it.toSet() } } ?: flowOf(null)
        return combine(base, inCollection, withTag) { items, collectionIds, tagIds ->
            items.filter {
                (filter.platforms.isEmpty() || it.platform in filter.platforms) &&
                    (collectionIds == null || it.id in collectionIds) &&
                    (tagIds == null || it.id in tagIds)
            }.map { it.toDomain() }
        }
    }

    override suspend fun findByNormalizedUrl(normalizedUrl: String): SavedItem? =
        dao.findByNormalizedUrl(normalizedUrl)?.takeUnless { it.deleted }?.toDomain()

    override suspend fun save(url: String, normalizedUrl: String, platform: Platform): SavedItem = db.withTransaction {
        val now = now()
        val entity = dao.findByNormalizedUrl(normalizedUrl)
            ?.copy(url = url, platform = platform, deleted = false, createdAt = now, updatedAt = now)
            ?: SavedItemEntity(
                id = UUID.randomUUID().toString(),
                url = url,
                normalizedUrl = normalizedUrl,
                platform = platform,
                createdAt = now,
                updatedAt = now,
            )
        dao.upsert(entity)
        entity.toDomain()
    }

    override suspend fun setNotes(id: String, notes: String) = dao.setNotes(id, notes, now())
    override suspend fun setFavorite(id: String, favorite: Boolean) = dao.setFavorite(id, favorite, now())
    override suspend fun setPinned(id: String, pinned: Boolean) = dao.setPinned(id, pinned, now())
    override suspend fun delete(ids: Collection<String>) = dao.setDeleted(ids.toList(), deleted = true, now())
    override suspend fun restore(ids: Collection<String>) = dao.setDeleted(ids.toList(), deleted = false, now())
}

/**
 * Turns what the user typed into a safe FTS4 MATCH expression: every word becomes a prefix term,
 * all terms must match. Punctuation and FTS operators are dropped. Null when nothing is left.
 */
internal fun ftsMatch(query: String): String? =
    Regex("""[\p{L}\p{N}]+""").findAll(query.lowercase()).map { "${it.value}*" }.toList().ifEmpty { null }?.joinToString(" ")

/** [CollectionRepository] on top of Room. */
class RoomCollectionRepository @Inject constructor(private val db: ReelVaultDatabase) : CollectionRepository {
    private val dao = db.collectionDao()

    override fun observeCollections(): Flow<List<VaultCollection>> =
        dao.observeCollectionsWithStats().map { list -> list.map { it.toDomain() } }

    override fun observeCollection(id: String): Flow<VaultCollection?> = dao.observeCollection(id).map { it?.toDomain() }

    override fun observeItems(collectionId: String): Flow<List<SavedItem>> =
        dao.observeItems(collectionId).map { list -> list.map { it.toDomain() } }

    override fun observeCollectionIdsFor(itemId: String): Flow<Set<String>> =
        dao.observeCollectionIdsFor(itemId).map { it.toSet() }

    override suspend fun create(name: String, emoji: String?, colorArgb: Long?): VaultCollection = db.withTransaction {
        val now = now()
        val entity = CollectionEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            emoji = emoji?.trim()?.ifEmpty { null },
            colorArgb = colorArgb,
            sortOrder = dao.maxSortOrder() + 1,
            createdAt = now,
            updatedAt = now,
        )
        dao.upsert(entity)
        entity.toDomain()
    }

    override suspend fun update(id: String, name: String, emoji: String?, colorArgb: Long?) = edit(id) {
        it.copy(name = name.trim(), emoji = emoji?.trim()?.ifEmpty { null }, colorArgb = colorArgb)
    }

    override suspend fun setCover(id: String, itemId: String?) = edit(id) { it.copy(coverItemId = itemId) }

    override suspend fun delete(id: String) = edit(id) { it.copy(deleted = true) }

    override suspend fun reorder(orderedIds: List<String>) = db.withTransaction {
        val now = now()
        val byId = dao.getAllActive().associateBy { it.id }
        dao.upsertAll(orderedIds.mapIndexedNotNull { index, id -> byId[id]?.takeIf { it.sortOrder != index }?.copy(sortOrder = index, updatedAt = now) })
    }

    override suspend fun setMembership(itemId: String, collectionId: String, member: Boolean) = db.withTransaction {
        val now = now()
        if (member) dao.addItem(CollectionItemCrossRef(collectionId, itemId, addedAt = now))
        else dao.removeItem(collectionId, itemId)
        db.itemDao().touch(itemId, now)
    }

    override suspend fun addItems(itemIds: Collection<String>, collectionId: String) = db.withTransaction {
        val now = now()
        itemIds.forEach {
            dao.addItem(CollectionItemCrossRef(collectionId, it, addedAt = now))
            db.itemDao().touch(it, now)
        }
    }

    private suspend fun edit(id: String, change: (CollectionEntity) -> CollectionEntity) = db.withTransaction {
        val current = dao.getById(id) ?: return@withTransaction
        dao.upsert(change(current).copy(updatedAt = now()))
    }
}

/** [TagRepository] on top of Room. */
class RoomTagRepository @Inject constructor(private val db: ReelVaultDatabase) : TagRepository {
    private val dao = db.tagDao()
    private val items = db.itemDao()

    override fun observeTags(itemId: String): Flow<List<Tag>> =
        dao.observeTags(itemId).map { list -> list.map { Tag(it.name, it.source) } }

    override fun observeAllTags(): Flow<List<TagSummary>> =
        dao.observeTagCounts().map { list -> list.map { TagSummary(it.name, it.count) } }

    override fun observeRules(): Flow<List<TagRule>> = dao.observeRules().map { list -> list.map { it.toDomain() } }

    override suspend fun addTag(itemIds: Collection<String>, name: String) {
        val tag = name.trim().removePrefix("#").trim()
        if (tag.isEmpty()) return
        db.withTransaction {
            // REPLACE turns an existing auto tag into a manual one, so auto-tagging won't remove it later.
            dao.insertReplace(itemIds.map { ItemTagEntity(it, tag, TagSource.MANUAL) })
            itemIds.forEach { refreshTagsText(it) }
        }
    }

    override suspend fun removeTag(itemId: String, name: String) = db.withTransaction {
        dao.delete(itemId, name)
        refreshTagsText(itemId)
    }

    override suspend fun renameTag(old: String, new: String) {
        val target = new.trim().removePrefix("#").trim()
        if (target.isEmpty() || target == old) return
        db.withTransaction {
            val affected = dao.getItemIdsWithTag(old)
            dao.copyTag(old, target)
            dao.deleteEverywhere(old)
            affected.forEach { refreshTagsText(it) }
        }
    }

    override suspend fun deleteTag(name: String) = db.withTransaction {
        val affected = dao.getItemIdsWithTag(name)
        dao.deleteEverywhere(name)
        affected.forEach { refreshTagsText(it) }
    }

    override suspend fun applyAutoTags(itemId: String) = db.withTransaction {
        applyAutoTags(itemId, dao.getRules().map { it.toDomain() })
    }

    override suspend fun reapplyAutoTags() = db.withTransaction {
        val rules = dao.getRules().map { it.toDomain() }
        items.getAllActive().forEach { applyAutoTags(it.id, rules) }
    }

    override suspend fun saveRule(rule: TagRule) = dao.upsertRule(rule.toEntity())
    override suspend fun deleteRule(id: Long) = dao.deleteRule(id)

    override suspend fun resetRules() = db.withTransaction {
        dao.deleteAllRules()
        dao.insertRules(AutoTagger.DEFAULT_RULES.map { it.toEntity() })
    }

    private suspend fun applyAutoTags(itemId: String, rules: List<TagRule>) {
        val item = items.getById(itemId) ?: return
        val labels = item.imageLabels.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val auto = AutoTagger.autoTags(item.title, item.caption, labels, rules)
        val before = dao.getTags(itemId).map { it.name }.toSet()
        dao.deleteAutoTags(itemId)
        // IGNORE: a manual tag with the same name wins.
        dao.insertIgnore(auto.map { ItemTagEntity(itemId, it, TagSource.AUTO) })
        if (dao.getTags(itemId).map { it.name }.toSet() != before) refreshTagsText(itemId)
    }

    /** Copies the item's tag names into its FTS-indexed column and marks it changed for sync. */
    private suspend fun refreshTagsText(itemId: String) {
        val names = dao.getTags(itemId).map { it.name }.sortedWith(String.CASE_INSENSITIVE_ORDER)
        items.setTagsText(itemId, names.joinToString(" "), now())
    }
}

internal fun SavedItemEntity.toDomain() = SavedItem(
    id = id,
    url = url,
    normalizedUrl = normalizedUrl,
    platform = platform,
    title = title,
    caption = caption,
    authorName = authorName,
    thumbnailUrl = thumbnailUrl,
    localThumbnailPath = localThumbnailPath,
    notes = notes,
    previewState = previewState,
    isFavorite = isFavorite,
    isPinned = isPinned,
    createdAt = createdAt,
)

internal fun CollectionEntity.toDomain() = VaultCollection(
    id = id,
    name = name,
    emoji = emoji,
    colorArgb = colorArgb,
    coverItemId = coverItemId,
    shareId = shareId,
)

internal fun CollectionWithStats.toDomain() = collection.toDomain().copy(
    itemCount = itemCount,
    coverImage = coverLocalPath ?: coverThumbnailUrl,
)

internal fun TagRuleEntity.toDomain() = TagRule(
    id = id,
    category = category,
    keywords = keywords.split(',').map { it.trim() }.filter { it.isNotEmpty() },
    enabled = enabled,
)

internal fun TagRule.toEntity() = TagRuleEntity(id = id, category = category.trim(), keywords = keywords.joinToString(", "), enabled = enabled)
