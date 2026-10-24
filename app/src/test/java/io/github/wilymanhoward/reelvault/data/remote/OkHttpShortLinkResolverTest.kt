package io.github.wilymanhoward.reelvault.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OkHttpShortLinkResolverTest {
    private val server = MockWebServer().apply { start() }
    private val resolver = OkHttpShortLinkResolver(OkHttpClient())

    @After fun tearDown() = server.shutdown()

    @Test fun followsRedirectChain() = runTest {
        server.enqueue(MockResponse().setResponseCode(301).setHeader("Location", "/hop"))
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/video/42?x=1"))
        server.enqueue(MockResponse().setBody("ok"))
        assertEquals(server.url("/video/42?x=1").toString(), resolver.resolve(server.url("/s/abc").toString()))
    }

    @Test fun networkFailureIsNull() = runTest {
        server.shutdown()
        assertNull(resolver.resolve("http://127.0.0.1:1/nothing"))
    }
}
