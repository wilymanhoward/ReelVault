package io.github.wilymanhoward.reelvault.data.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareLinkTest {
    private val host = "reelvault-2945c.web.app"

    @Test fun parsesCollectionLinks() {
        assertEquals("AbC123xyz789", shareIdFromLink("https://$host/c/AbC123xyz789", host))
        assertEquals("AbC123xyz789", shareIdFromLink("https://$host/c/AbC123xyz789/?utm_source=x", host))
    }

    @Test fun rejectsOtherHostsSchemesAndPaths() {
        assertNull(shareIdFromLink("https://evil.example/c/AbC123xyz789", host))
        assertNull(shareIdFromLink("https://$host.evil.example/c/AbC123xyz789", host))
        assertNull(shareIdFromLink("http://$host/c/AbC123xyz789", host))
        assertNull(shareIdFromLink("https://$host/", host))
        assertNull(shareIdFromLink("https://$host/c/", host))
        assertNull(shareIdFromLink("https://$host/c/a/b", host))
        assertNull(shareIdFromLink("https://$host/c/bad%2Fid", host))
    }
}
