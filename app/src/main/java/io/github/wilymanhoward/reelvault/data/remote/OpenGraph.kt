package io.github.wilymanhoward.reelvault.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import javax.inject.Inject

/** Open Graph / Twitter-card tags read from a page. [finalUrl] is where redirects ended. */
data class OpenGraph(
    val title: String?,
    val description: String?,
    val image: String?,
    val author: String?,
    val finalUrl: String,
) {
    val isEmpty get() = title == null && description == null && image == null
}

/** Pure HTML → [OpenGraph] parsing. */
object OpenGraphParser {
    fun parse(html: String, finalUrl: String): OpenGraph {
        val doc = Jsoup.parse(html, finalUrl)
        fun meta(vararg keys: String) = keys.firstNotNullOfOrNull { key ->
            doc.selectFirst("meta[property=$key], meta[name=$key]")?.attr("content")?.trim()?.takeIf { it.isNotEmpty() }
        }
        return OpenGraph(
            title = meta("og:title", "twitter:title") ?: doc.title().trim().ifEmpty { null },
            description = meta("og:description", "twitter:description", "description"),
            image = meta("og:image:secure_url", "og:image", "twitter:image", "twitter:image:src")?.let { absolute(it, finalUrl) },
            author = meta("author", "article:author", "twitter:creator"),
            finalUrl = finalUrl,
        )
    }

    private fun absolute(url: String, base: String): String? = when {
        url.startsWith("//") -> "https:$url"
        else -> base.toHttpUrlOrNull()?.resolve(url)?.toString()
    }
}

/** Downloads a page and parses its Open Graph tags. */
class OpenGraphFetcher @Inject constructor(private val client: OkHttpClient) {

    /**
     * @return the tags, or null when the page is gone (4xx).
     * @throws IOException on network errors and 5xx, which are worth retrying.
     */
    suspend fun fetch(url: String): OpenGraph? = withContext(Dispatchers.IO) {
        // Platforms serve their tags to link-preview crawlers; TikTok sends a browser only a JS shell.
        val first = load(url, CRAWLER_UA) ?: return@withContext null
        if (!first.isEmpty) first else load(url, BROWSER_UA) ?: first
    }

    private fun load(url: String, userAgent: String): OpenGraph? {
        val request = Request.Builder().url(url).header("User-Agent", userAgent).header("Accept-Language", "en").build()
        client.newCall(request).execute().use { response ->
            if (response.code in 400..499) return null
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $url")
            return OpenGraphParser.parse(response.body?.string().orEmpty(), response.request.url.toString())
        }
    }

    private companion object {
        const val CRAWLER_UA = "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)"
        const val BROWSER_UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36"
    }
}
