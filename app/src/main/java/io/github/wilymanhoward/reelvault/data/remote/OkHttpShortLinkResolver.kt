package io.github.wilymanhoward.reelvault.data.remote

import io.github.wilymanhoward.reelvault.domain.repository.ShortLinkResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

/**
 * Resolves short links by letting OkHttp follow the redirect chain.
 *
 * Checked against the live hosts: vm.tiktok.com, pin.it, xhslink.com and fb.watch all answer with
 * plain 30x redirects, so no HTML/JS parsing is needed. The body is never read.
 */
class OkHttpShortLinkResolver @Inject constructor(private val client: OkHttpClient) : ShortLinkResolver {
    override suspend fun resolve(url: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            client.newCall(Request.Builder().url(url).get().build()).execute().use { it.request.url.toString() }
        }.getOrNull()
    }
}
