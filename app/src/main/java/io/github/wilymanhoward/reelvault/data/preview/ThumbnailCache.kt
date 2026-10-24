package io.github.wilymanhoward.reelvault.data.preview

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copies thumbnails into app storage. TikTok and Instagram serve signed CDN URLs that expire,
 * so a local copy is what keeps cards from going blank weeks later.
 */
@Singleton
class ThumbnailCache @Inject constructor(
    @ApplicationContext context: Context,
    private val client: OkHttpClient,
) {
    private val dir = File(context.filesDir, "thumbnails")

    /**
     * Downloads [url] for [itemId].
     *
     * @return the local file path, or null when the server says the image is gone (4xx).
     * @throws IOException on network errors.
     */
    suspend fun store(itemId: String, url: String): String? = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (response.code in 400..499) return@withContext null
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} for thumbnail")
            val body = response.body ?: return@withContext null
            dir.mkdirs()
            val target = File(dir, "$itemId.img")
            // Write to a temp file first so a failed download never replaces a good cached image.
            val temp = File(dir, "$itemId.tmp")
            temp.outputStream().use { out -> body.byteStream().copyTo(out) }
            if (!temp.renameTo(target)) {
                target.delete()
                if (!temp.renameTo(target)) throw IOException("Couldn't move thumbnail into place")
            }
            target.absolutePath
        }
    }

    fun delete(itemId: String) {
        File(dir, "$itemId.img").delete()
    }
}
