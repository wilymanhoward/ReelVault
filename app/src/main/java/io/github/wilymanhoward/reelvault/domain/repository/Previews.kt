package io.github.wilymanhoward.reelvault.domain.repository

import io.github.wilymanhoward.reelvault.domain.model.Platform

/** What a preview card shows. Any field may be missing; the card falls back gracefully. */
data class PreviewMetadata(
    val title: String? = null,
    val caption: String? = null,
    val authorName: String? = null,
    val authorUrl: String? = null,
    val thumbnailUrl: String? = null,
) {
    val isEmpty get() = title == null && caption == null && authorName == null && thumbnailUrl == null
}

/** Fetches preview metadata for a link, with one strategy per platform. */
interface PreviewResolver {
    /**
     * @return the metadata, or null when the platform has nothing for this link (deleted, private, 404).
     * @throws java.io.IOException on network trouble, so the caller can retry later.
     */
    suspend fun resolve(url: String, normalizedUrl: String, platform: Platform): PreviewMetadata?
}

/** Queues background preview fetching. */
fun interface PreviewScheduler {
    /** Fetch (or re-fetch, e.g. after an expired thumbnail) the preview for [itemId]. */
    fun schedule(itemId: String)
}
