package io.github.wilymanhoward.reelvault.data.repository

import androidx.room.withTransaction
import io.github.wilymanhoward.reelvault.data.local.CollectionEntity
import io.github.wilymanhoward.reelvault.data.local.CollectionItemCrossRef
import io.github.wilymanhoward.reelvault.data.local.ReelVaultDatabase
import io.github.wilymanhoward.reelvault.data.local.SavedItemEntity
import io.github.wilymanhoward.reelvault.domain.model.Platform
import io.github.wilymanhoward.reelvault.domain.model.SavedItem
import io.github.wilymanhoward.reelvault.domain.model.VaultCollection
import io.github.wilymanhoward.reelvault.domain.repository.CollectionRepository
import io.github.wilymanhoward.reelvault.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

/** [ItemRepository] on top of Room. */
class RoomItemRepository @Inject constructor(private val db: ReelVaultDatabase) : ItemRepository {
    private val dao = db.itemDao()

    override fun observeItems(): Flow<List<SavedItem>> = dao.observeItems().map { list -> list.map { it.toDomain() } }

    override suspend fun findByNormalizedUrl(normalizedUrl: String): SavedItem? =
        dao.findByNormalizedUrl(normalizedUrl)?.takeUnless { it.deleted }?.toDomain()

    override suspend fun save(url: String, normalizedUrl: String, platform: Platform): SavedItem = db.withTransaction {
        val now = System.currentTimeMillis()
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
}

/** [CollectionRepository] on top of Room. */
class RoomCollectionRepository @Inject constructor(private val db: ReelVaultDatabase) : CollectionRepository {
    private val dao = db.collectionDao()

    override fun observeCollections(): Flow<List<VaultCollection>> =
        dao.observeCollections().map { list -> list.map { it.toDomain() } }

    override fun observeCollectionIdsFor(itemId: String): Flow<Set<String>> =
        dao.observeCollectionIdsFor(itemId).map { it.toSet() }

    override suspend fun create(name: String): VaultCollection = db.withTransaction {
        val now = System.currentTimeMillis()
        val entity = CollectionEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            sortOrder = dao.maxSortOrder() + 1,
            createdAt = now,
            updatedAt = now,
        )
        dao.upsert(entity)
        entity.toDomain()
    }

    override suspend fun setMembership(itemId: String, collectionId: String, member: Boolean) = db.withTransaction {
        val now = System.currentTimeMillis()
        if (member) dao.addItem(CollectionItemCrossRef(collectionId, itemId, addedAt = now))
        else dao.removeItem(collectionId, itemId)
        db.itemDao().touch(itemId, now)
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

internal fun CollectionEntity.toDomain() = VaultCollection(id = id, name = name, emoji = emoji, colorArgb = colorArgb)
