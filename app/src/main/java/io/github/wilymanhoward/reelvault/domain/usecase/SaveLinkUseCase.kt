package io.github.wilymanhoward.reelvault.domain.usecase

import io.github.wilymanhoward.reelvault.domain.link.PlatformDetector
import io.github.wilymanhoward.reelvault.domain.link.UrlExtractor
import io.github.wilymanhoward.reelvault.domain.link.UrlNormalizer
import io.github.wilymanhoward.reelvault.domain.model.SavedItem
import io.github.wilymanhoward.reelvault.domain.repository.ItemRepository
import io.github.wilymanhoward.reelvault.domain.repository.PreviewScheduler
import io.github.wilymanhoward.reelvault.domain.repository.ShortLinkResolver
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

/** Outcome of saving shared text. */
sealed interface SaveResult {
    data class Saved(val item: SavedItem) : SaveResult
    data class AlreadySaved(val item: SavedItem) : SaveResult
    data object NoLink : SaveResult
}

/** Turns shared or pasted text into a saved item: extract, resolve short link, clean, dedupe, store. */
class SaveLinkUseCase @Inject constructor(
    private val items: ItemRepository,
    private val resolver: ShortLinkResolver,
    private val previews: PreviewScheduler,
) {
    suspend operator fun invoke(text: String): SaveResult {
        val raw = UrlExtractor.extract(text) ?: return SaveResult.NoLink
        val url = UrlNormalizer.clean(resolve(raw))
        val normalized = UrlNormalizer.normalize(url)
        items.findByNormalizedUrl(normalized)?.let { return SaveResult.AlreadySaved(it) }
        val item = items.save(url, normalized, PlatformDetector.detect(url))
        previews.schedule(item.id)
        return SaveResult.Saved(item)
    }

    // Offline, or a dead short link that bounces to the site's home page: keep the short link itself.
    // shortcut: such items stay keyed by their short URL; the Phase 3 refresh job is the place to re-resolve them.
    private suspend fun resolve(raw: String): String {
        if (!UrlNormalizer.isShortLink(raw)) return raw
        val resolved = resolver.resolve(raw) ?: return raw
        val path = resolved.toHttpUrlOrNull()?.encodedPath ?: return raw
        return if (path.trimEnd('/').isEmpty()) raw else resolved
    }
}
