package io.github.wilymanhoward.reelvault.data.backup

import androidx.room.withTransaction
import io.github.wilymanhoward.reelvault.data.local.CollectionEntity
import io.github.wilymanhoward.reelvault.data.local.CollectionItemCrossRef
import io.github.wilymanhoward.reelvault.data.local.ItemTagEntity
import io.github.wilymanhoward.reelvault.data.local.ReelVaultDatabase
import io.github.wilymanhoward.reelvault.data.local.SavedItemEntity
import io.github.wilymanhoward.reelvault.data.local.TagRuleEntity
import io.github.wilymanhoward.reelvault.domain.model.Platform
import io.github.wilymanhoward.reelvault.domain.model.PreviewState
import io.github.wilymanhoward.reelvault.domain.model.TagSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** Backup file format, version [VERSION]. Plain JSON so people can read or script it. */
@Serializable
data class Backup(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: Long,
    val items: List<BackupItem>,
    val collections: List<BackupCollection>,
    val rules: List<BackupRule>,
) {
    companion object {
        const val FORMAT = "reelvault-backup"
        const val VERSION = 1
    }
}

@Serializable
data class BackupItem(
    val id: String,
    val url: String,
    val normalizedUrl: String,
    val platform: String,
    val title: String? = null,
    val caption: String? = null,
    val authorName: String? = null,
    val authorUrl: String? = null,
    val thumbnailUrl: String? = null,
    val notes: String = "",
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Long,
    val tags: List<BackupTag> = emptyList(),
    val collectionIds: List<String> = emptyList(),
)

@Serializable
data class BackupTag(val name: String, val auto: Boolean = false)

@Serializable
data class BackupCollection(
    val id: String,
    val name: String,
    val emoji: String? = null,
    val colorArgb: Long? = null,
    val sortOrder: Int = 0,
    val createdAt: Long,
)

@Serializable
data class BackupRule(val category: String, val keywords: String, val enabled: Boolean = true)

/** What an import changed. */
data class ImportResult(val addedIds: List<String>, val skipped: Int, val collections: Int)

/** Exports the vault to JSON and merges a backup back in. Deleted saves aren't exported. */
class BackupRepository @Inject constructor(private val db: ReelVaultDatabase) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = false }

    suspend fun export(now: Long = System.currentTimeMillis()): String {
        val items = db.itemDao().getAllActive()
        val backup = Backup(
            exportedAt = now,
            items = items.map { item ->
                BackupItem(
                    id = item.id,
                    url = item.url,
                    normalizedUrl = item.normalizedUrl,
                    platform = item.platform.name,
                    title = item.title,
                    caption = item.caption,
                    authorName = item.authorName,
                    authorUrl = item.authorUrl,
                    thumbnailUrl = item.thumbnailUrl,
                    notes = item.notes,
                    isFavorite = item.isFavorite,
                    isPinned = item.isPinned,
                    createdAt = item.createdAt,
                    tags = db.tagDao().getTags(item.id).map { BackupTag(it.name, it.source == TagSource.AUTO) },
                    collectionIds = db.collectionDao().getCollectionIdsFor(item.id),
                )
            },
            collections = db.collectionDao().getAllActive().map {
                BackupCollection(it.id, it.name, it.emoji, it.colorArgb, it.sortOrder, it.createdAt)
            },
            rules = db.tagDao().getRules().map { BackupRule(it.category, it.keywords, it.enabled) },
        )
        return json.encodeToString(Backup.serializer(), backup)
    }

    /**
     * Merges [text] into the vault: saves already here (same normalized link) are kept as they are,
     * new ones are added with their tags and collections, and rules for new categories are added.
     *
     * @throws IllegalArgumentException when [text] isn't a ReelVault backup.
     */
    suspend fun import(text: String, now: Long = System.currentTimeMillis()): ImportResult {
        val backup = runCatching { json.decodeFromString(Backup.serializer(), text) }
            .getOrElse { throw IllegalArgumentException("This file isn't a ReelVault backup", it) }
        require(backup.format == Backup.FORMAT) { "This file isn't a ReelVault backup" }
        require(backup.version <= Backup.VERSION) { "This backup is from a newer ReelVault; update the app first" }

        return db.withTransaction {
            val collections = db.collectionDao()
            val items = db.itemDao()
            val tags = db.tagDao()

            var newCollections = 0
            backup.collections.forEach { c ->
                if (collections.getById(c.id) == null) {
                    collections.upsert(CollectionEntity(c.id, c.name, c.emoji, c.colorArgb, sortOrder = c.sortOrder, createdAt = c.createdAt, updatedAt = now))
                    newCollections++
                }
            }

            val added = mutableListOf<String>()
            var skipped = 0
            backup.items.forEach { b ->
                val existing = items.findByNormalizedUrl(b.normalizedUrl)
                if (existing != null && !existing.deleted) {
                    skipped++
                    return@forEach
                }
                // Re-use a soft-deleted row (the normalized URL is unique) or the backup's own id.
                val id = existing?.id ?: if (items.getById(b.id) == null) b.id else java.util.UUID.randomUUID().toString()
                items.upsert(
                    SavedItemEntity(
                        id = id,
                        url = b.url,
                        normalizedUrl = b.normalizedUrl,
                        platform = Platform.entries.firstOrNull { it.name == b.platform } ?: Platform.OTHER,
                        title = b.title,
                        caption = b.caption,
                        authorName = b.authorName,
                        authorUrl = b.authorUrl,
                        thumbnailUrl = b.thumbnailUrl,
                        notes = b.notes,
                        tagsText = b.tags.map { it.name }.sortedWith(String.CASE_INSENSITIVE_ORDER).joinToString(" "),
                        previewState = if (b.title != null || b.thumbnailUrl != null) PreviewState.READY else PreviewState.PENDING,
                        isFavorite = b.isFavorite,
                        isPinned = b.isPinned,
                        createdAt = b.createdAt,
                        updatedAt = now,
                    ),
                )
                tags.insertIgnore(b.tags.map { ItemTagEntity(id, it.name, if (it.auto) TagSource.AUTO else TagSource.MANUAL) })
                b.collectionIds.filter { collections.getById(it) != null }.forEach {
                    collections.addItem(CollectionItemCrossRef(it, id, addedAt = now))
                }
                added += id
            }

            val known = tags.getRules().map { it.category.lowercase() }.toSet()
            tags.insertRules(backup.rules.filter { it.category.lowercase() !in known }.map { TagRuleEntity(category = it.category, keywords = it.keywords, enabled = it.enabled) })

            ImportResult(addedIds = added, skipped = skipped, collections = newCollections)
        }
    }
}
