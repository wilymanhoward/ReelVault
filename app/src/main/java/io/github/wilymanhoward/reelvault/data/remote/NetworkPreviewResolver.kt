package io.github.wilymanhoward.reelvault.data.remote

import io.github.wilymanhoward.reelvault.BuildConfig
import io.github.wilymanhoward.reelvault.domain.model.Platform
import io.github.wilymanhoward.reelvault.domain.repository.PreviewMetadata
import io.github.wilymanhoward.reelvault.domain.repository.PreviewResolver
import kotlinx.serialization.SerializationException
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/** Fetches previews for one platform. */
private fun interface PreviewStrategy {
    suspend fun fetch(url: String, normalizedUrl: String): PreviewMetadata?
}

/**
 * [PreviewResolver] with one strategy per platform and a generic Open Graph fallback.
 * oEmbed failures that aren't network errors fall through to Open Graph.
 */
class NetworkPreviewResolver @Inject constructor(
    private val oEmbed: OEmbedApi,
    private val openGraph: OpenGraphFetcher,
) : PreviewResolver {

    private val metaToken = BuildConfig.META_OEMBED_TOKEN.ifBlank { null }

    private val generic = PreviewStrategy { url, _ -> openGraph.fetch(url)?.let(PreviewShaping::generic) }

    private val strategies: Map<Platform, PreviewStrategy> = mapOf(
        Platform.TIKTOK to PreviewStrategy { url, _ ->
            val embed = optional { oEmbed.fetch(OEmbedApi.TIKTOK, url) }
            val og = openGraph.fetch(url)?.let(PreviewShaping::tiktok)
            merge(embed?.toMetadata(), og)
        },
        Platform.YOUTUBE to PreviewStrategy { url, normalizedUrl ->
            val id = normalizedUrl.toHttpUrlOrNull()?.queryParameter("v")
            val embed = optional { oEmbed.fetch(OEmbedApi.YOUTUBE, url, format = "json") } ?: return@PreviewStrategy null
            embed.toMetadata().copy(thumbnailUrl = id?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" } ?: embed.thumbnailUrl)
        },
        Platform.X to PreviewStrategy { url, _ ->
            val embed = optional { oEmbed.fetch(OEmbedApi.X, url, omitScript = true) }
            val text = embed?.html?.let(PreviewShaping::xPostText)
            val og = openGraph.fetch(url)
            merge(
                embed?.let { PreviewMetadata(caption = text, authorName = it.authorName, authorUrl = it.authorUrl) },
                og?.let { PreviewMetadata(thumbnailUrl = it.image) },
            )
        },
        Platform.INSTAGRAM to metaStrategy(OEmbedApi.INSTAGRAM, PreviewShaping::instagram),
        Platform.FACEBOOK to metaStrategy(OEmbedApi.FACEBOOK, PreviewShaping::facebook),
        Platform.XIAOHONGSHU to PreviewStrategy { url, _ ->
            openGraph.fetch(url)?.takeUnless(PreviewShaping::isRedMissingPage)?.let(PreviewShaping::generic)
        },
    )

    override suspend fun resolve(url: String, normalizedUrl: String, platform: Platform): PreviewMetadata? =
        (strategies[platform] ?: generic).fetch(url, normalizedUrl)?.takeUnless { it.isEmpty }

    /** Meta's oEmbed when a token is configured, Open Graph otherwise (and for whatever oEmbed lacks). */
    private fun metaStrategy(endpoint: String, shape: (OpenGraph) -> PreviewMetadata) = PreviewStrategy { url, _ ->
        val embed = metaToken?.let { token -> optional { oEmbed.fetch(endpoint, url, accessToken = token) } }
        if (embed?.thumbnailUrl != null) return@PreviewStrategy embed.toMetadata()
        merge(embed?.toMetadata(), openGraph.fetch(url)?.let(shape))
    }

    // An oEmbed miss (4xx, odd JSON) just means "try Open Graph"; only network errors propagate.
    private suspend fun <T> optional(block: suspend () -> T): T? = try {
        block()
    } catch (e: HttpException) {
        if (e.code() >= 500) throw IOException("oEmbed HTTP ${e.code()}", e) else null
    } catch (e: SerializationException) {
        null
    }

    private fun merge(primary: PreviewMetadata?, secondary: PreviewMetadata?): PreviewMetadata? {
        if (primary == null || secondary == null) return primary ?: secondary
        return PreviewMetadata(
            title = primary.title ?: secondary.title,
            caption = primary.caption ?: secondary.caption,
            authorName = primary.authorName ?: secondary.authorName,
            authorUrl = primary.authorUrl ?: secondary.authorUrl,
            thumbnailUrl = primary.thumbnailUrl ?: secondary.thumbnailUrl,
        )
    }

    private fun OEmbedResponse.toMetadata() = PreviewMetadata(
        title = title?.takeIf { it.isNotBlank() },
        authorName = authorName?.takeIf { it.isNotBlank() },
        authorUrl = authorUrl,
        thumbnailUrl = thumbnailUrl,
    )
}

/** Platform-specific clean-up of scraped tags. Pure, so it's unit-tested against real tag formats. */
internal object PreviewShaping {
    private val INSTAGRAM_TITLE = Regex("""^(.+?) on Instagram: ["“](.*)["”]\s*$""", RegexOption.DOT_MATCHES_ALL)
    private val INSTAGRAM_HANDLE = Regex("""^(.+?) \(@""")

    fun generic(og: OpenGraph) = PreviewMetadata(
        title = og.title,
        caption = og.description?.takeIf { it != og.title },
        authorName = og.author,
        thumbnailUrl = og.image,
    )

    /** TikTok's og:title is "TikTok · {author}" and the caption is usually absent. */
    fun tiktok(og: OpenGraph) = PreviewMetadata(
        caption = og.description,
        authorName = og.title?.substringAfter("TikTok · ", "")?.ifBlank { null },
        thumbnailUrl = og.image,
    )

    /** Instagram's og:title is `{author} on Instagram: "{caption}"`. */
    fun instagram(og: OpenGraph): PreviewMetadata {
        val match = og.title?.let(INSTAGRAM_TITLE::find)
        return PreviewMetadata(
            caption = match?.groupValues?.get(2)?.ifBlank { null } ?: og.description,
            authorName = match?.groupValues?.get(1) ?: og.title?.let(INSTAGRAM_HANDLE::find)?.groupValues?.get(1),
            thumbnailUrl = og.image,
        )
    }

    /** Facebook's og:title is "{views} · {reactions} | {caption} | Facebook"; the description is the clean caption. */
    fun facebook(og: OpenGraph) = PreviewMetadata(
        caption = og.description ?: og.title?.removeSuffix(" | Facebook")?.substringAfter(" | "),
        thumbnailUrl = og.image,
    )

    /** The post text from X's oEmbed blockquote. */
    fun xPostText(html: String): String? = Jsoup.parse(html).selectFirst("blockquote p")?.text()?.ifBlank { null }

    /** RED redirects links without a valid xsec_token to a /404 page that still has a generic og:image. */
    fun isRedMissingPage(og: OpenGraph) = og.finalUrl.toHttpUrlOrNull()?.encodedPath?.startsWith("/404") == true
}
