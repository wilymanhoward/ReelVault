package io.github.wilymanhoward.reelvault.domain.link

import io.github.wilymanhoward.reelvault.domain.model.Platform
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Finds the link inside shared text such as "Check this out! https://vm.tiktok.com/ZM123/ #fyp". */
object UrlExtractor {
    // Only RFC 3986 characters, so text glued to the link (common in RED shares: "…/AbC，复制本条") is cut off.
    private val URL = Regex("""https?://[A-Za-z0-9\-._~:/?#\[\]@!$&'()*+,;=%]+""", RegexOption.IGNORE_CASE)
    private const val TRAILING_JUNK = ".,;:!?)]}'"

    /** The first http(s) URL in [text], without trailing punctuation, or null if there is none. */
    fun extract(text: String): String? =
        URL.find(text)?.value?.trimEnd { it in TRAILING_JUNK }?.takeIf { it.toHttpUrlOrNull() != null }
}

/** Maps a link's host to its [Platform]. Works on short links too (vm.tiktok.com, youtu.be, …). */
object PlatformDetector {
    private val PINTEREST = Regex("""(^|\.)pinterest\.[a-z.]+$""")

    fun detect(url: String): Platform {
        val host = url.toHttpUrlOrNull()?.host ?: return Platform.OTHER
        fun on(vararg domains: String) = domains.any { host == it || host.endsWith(".$it") }
        return when {
            on("tiktok.com") -> Platform.TIKTOK
            on("instagram.com", "instagr.am") -> Platform.INSTAGRAM
            on("youtube.com", "youtu.be") -> Platform.YOUTUBE
            on("facebook.com", "fb.watch", "fb.com") -> Platform.FACEBOOK
            on("x.com", "twitter.com", "t.co") -> Platform.X
            on("pin.it") || PINTEREST.containsMatchIn(host) -> Platform.PINTEREST
            on("xiaohongshu.com", "xhslink.com") -> Platform.XIAOHONGSHU
            else -> Platform.OTHER
        }
    }
}

/**
 * Cleans links for storage and builds the canonical key used to spot duplicates.
 *
 * [clean] only strips tracking parameters, so the link still opens exactly as shared.
 * [normalize] goes further (one host spelling, no query, canonical paths) so two shares
 * of the same video produce the same key.
 */
object UrlNormalizer {
    private val SHORT_HOSTS = setOf("vm.tiktok.com", "vt.tiktok.com", "pin.it", "xhslink.com", "fb.watch", "t.co")
    private val TRACKING_PARAMS = setOf(
        "fbclid", "gclid", "igsh", "igshid", "si", "feature", "ref", "ref_src", "ref_url", "mc_cid", "mc_eid",
        "share_id", "share_app_id", "share_link_id", "_branch_match_id", "_branch_referrer", "is_from_webapp",
        "sender_device", "sender_web_id", "_r", "_t", "mibextid", "rdid", "app_platform", "app_version",
    )
    // Query params that identify the content on Facebook; everything else there is noise.
    private val FACEBOOK_KEEP = setOf("v", "story_fbid", "fbid", "id")
    private val YOUTUBE_PATH_ID = Regex("""^/(?:shorts|embed|live|v)/([\w-]{6,})""")
    private val INSTAGRAM_POST = Regex("""^/(?:[^/]+/)?(reels?|p|tv)/([\w-]+)""")
    private val X_STATUS = Regex("""^/[^/]+/status(?:es)?/(\d+)""")
    private val PINTEREST_PIN = Regex("""^/pin/([\w-]+)""")

    /** True for links that only redirect to the real video and must be resolved over the network. */
    fun isShortLink(url: String): Boolean {
        val u = url.toHttpUrlOrNull() ?: return false
        return u.host in SHORT_HOSTS ||
            (u.host.endsWith("tiktok.com") && u.encodedPath.startsWith("/t/")) ||
            (u.host.endsWith("facebook.com") && u.encodedPath.startsWith("/share/"))
    }

    /** Removes tracking parameters and the fragment; returns [url] unchanged if it doesn't parse. */
    fun clean(url: String): String {
        val u = url.toHttpUrlOrNull() ?: return url
        return u.newBuilder().fragment(null).removeQuery { it.startsWith("utm_") || it in TRACKING_PARAMS }.build().toString()
    }

    /** The canonical duplicate-detection key for [url]. */
    fun normalize(url: String): String {
        val u = clean(url).toHttpUrlOrNull() ?: return url.trim()
        val host = canonicalHost(u.host)
        val path = u.encodedPath.trimEnd('/').ifEmpty { "/" }
        val b = HttpUrl.Builder().scheme("https").host(host)

        when (PlatformDetector.detect(u.toString())) {
            Platform.YOUTUBE -> youtubeId(u, host, path)?.let {
                return b.host("youtube.com").addPathSegment("watch").addQueryParameter("v", it).build().toString()
            }
            Platform.INSTAGRAM -> INSTAGRAM_POST.find(path)?.let { m ->
                val kind = if (m.groupValues[1] == "p") "p" else "reel"
                return b.addPathSegment(kind).addPathSegment(m.groupValues[2]).build().toString()
            }
            Platform.X -> X_STATUS.find(path)?.let {
                return b.addPathSegments("i/status/${it.groupValues[1]}").build().toString()
            }
            Platform.PINTEREST -> PINTEREST_PIN.find(path)?.let {
                return b.host("pinterest.com").addPathSegment("pin").addPathSegment(it.groupValues[1]).build().toString()
            }
            Platform.FACEBOOK -> {
                b.encodedPath(path)
                u.queryParameterNames.filter { it in FACEBOOK_KEEP }.sorted()
                    .forEach { name -> b.addQueryParameter(name, u.queryParameter(name)) }
                return b.build().toString()
            }
            // TikTok and RED identify the video in the path; their query is per-share noise (incl. xsec_token).
            Platform.TIKTOK, Platform.XIAOHONGSHU -> return b.encodedPath(path).build().toString()
            else -> Unit
        }
        b.encodedPath(path)
        u.queryParameterNames.sorted().forEach { name ->
            u.queryParameterValues(name).forEach { b.addQueryParameter(name, it) }
        }
        return b.build().toString()
    }

    private fun canonicalHost(host: String): String {
        val bare = host.removePrefix("www.").removePrefix("m.").removePrefix("mobile.")
        return when (bare) {
            "twitter.com" -> "x.com"
            "instagr.am" -> "instagram.com"
            "fb.com" -> "facebook.com"
            else -> bare
        }
    }

    private fun youtubeId(u: HttpUrl, host: String, path: String): String? = when {
        host == "youtu.be" -> path.removePrefix("/").substringBefore('/').ifEmpty { null }
        path == "/watch" -> u.queryParameter("v")
        else -> YOUTUBE_PATH_ID.find(path)?.groupValues?.get(1)
    }

    private fun HttpUrl.Builder.removeQuery(drop: (String) -> Boolean): HttpUrl.Builder {
        build().queryParameterNames.filter(drop).forEach { removeAllQueryParameters(it) }
        return this
    }
}
