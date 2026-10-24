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

/**
 * A user-made group of saved items.
 *
 * @property coverItemId the item chosen as cover, or null for "first item".
 * @property coverImage local file path or URL of the cover thumbnail, when there is one.
 */
data class VaultCollection(
    val id: String,
    val name: String,
    val emoji: String?,
    val colorArgb: Long?,
    val coverItemId: String? = null,
    val shareId: String? = null,
    val itemCount: Int = 0,
    val coverImage: String? = null,
)

/** A tag on one item. */
data class Tag(val name: String, val source: TagSource)

/** A tag name with how many saves carry it. */
data class TagSummary(val name: String, val count: Int)

/**
 * An auto-tagging rule: any keyword in an item's text (or image labels) gives it [category].
 */
data class TagRule(val id: Long = 0, val category: String, val keywords: List<String>, val enabled: Boolean = true)

/** Narrows search results. Empty sets / nulls mean "any". */
data class SearchFilter(
    val platforms: Set<Platform> = emptySet(),
    val collectionId: String? = null,
    val tag: String? = null,
) {
    val isEmpty get() = platforms.isEmpty() && collectionId == null && tag == null
}

/** How lists of saves are ordered. Pinned items always come first. */
enum class SortOrder(val label: String) { NEWEST("Newest"), OLDEST("Oldest"), PLATFORM("Platform"), TITLE("A–Z") }

/** Applies [order], keeping pinned items on top. */
fun List<SavedItem>.sortedFor(order: SortOrder): List<SavedItem> {
    val byOrder: Comparator<SavedItem> = when (order) {
        SortOrder.NEWEST -> compareByDescending { it.createdAt }
        SortOrder.OLDEST -> compareBy { it.createdAt }
        SortOrder.PLATFORM -> compareBy<SavedItem> { it.platform.displayName }.thenByDescending { it.createdAt }
        SortOrder.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title ?: it.caption ?: it.authorName ?: it.url }
    }
    return sortedWith(compareByDescending<SavedItem> { it.isPinned }.then(byOrder))
}
