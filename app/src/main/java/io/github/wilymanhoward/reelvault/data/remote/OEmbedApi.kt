package io.github.wilymanhoward.reelvault.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/** oEmbed endpoints. Each platform has its own URL, so it's passed per call. */
interface OEmbedApi {
    @GET
    suspend fun fetch(
        @Url endpoint: String,
        @Query("url") url: String,
        @Query("format") format: String? = null,
        @Query("omit_script") omitScript: Boolean? = null,
        @Query("access_token") accessToken: String? = null,
    ): OEmbedResponse

    companion object {
        // Checked live: TikTok's oEmbed currently returns only the iframe (no title/author/thumbnail),
        // so TikTok also reads Open Graph. X's endpoint moved from publish.twitter.com to publish.x.com.
        const val TIKTOK = "https://www.tiktok.com/oembed"
        const val YOUTUBE = "https://www.youtube.com/oembed"
        const val X = "https://publish.x.com/oembed"
        // Unversioned Graph URLs use the oldest supported API version, so they don't expire with a version.
        const val INSTAGRAM = "https://graph.facebook.com/instagram_oembed"
        const val FACEBOOK = "https://graph.facebook.com/oembed_video"
    }
}

/** The oEmbed fields ReelVault uses. */
@Serializable
data class OEmbedResponse(
    val title: String? = null,
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("author_url") val authorUrl: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    val html: String? = null,
)
