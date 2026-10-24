package io.github.wilymanhoward.reelvault.domain.model

/** Where a preview's metadata stands. */
enum class PreviewState { PENDING, READY, FAILED }

/** Whether a tag was added by the auto-tagger or by the user. */
enum class TagSource { AUTO, MANUAL }

/**
 * A saved video link as the UI sees it.
 *
 * @property url the link used to open the video (short links already resolved, tracking removed).
 * @property normalizedUrl canonical form used to detect duplicates.
 */
data class SavedItem(
    val id: String,
    val url: String,
    val normalizedUrl: String,
    val platform: Platform,
    val title: String?,
    val caption: String?,
    val authorName: String?,
    val thumbnailUrl: String?,
    val localThumbnailPath: String?,
    val notes: String,
    val previewState: PreviewState,
    val isFavorite: Boolean,
    val isPinned: Boolean,
    val createdAt: Long,
)

/** A user-made group of saved items. */
data class VaultCollection(
    val id: String,
    val name: String,
    val emoji: String?,
    val colorArgb: Long?,
)
