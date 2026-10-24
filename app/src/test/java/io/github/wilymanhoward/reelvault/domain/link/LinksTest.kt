package io.github.wilymanhoward.reelvault.domain.link

import io.github.wilymanhoward.reelvault.domain.model.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlExtractorTest {
    @Test fun findsUrlInsideCaption() =
        assertEquals("https://vm.tiktok.com/ZMabc123/", UrlExtractor.extract("Check this out! https://vm.tiktok.com/ZMabc123/ #fyp"))

    @Test fun dropsTrailingPunctuation() =
        assertEquals("https://youtu.be/dQw4w9WgXcQ", UrlExtractor.extract("Watch (https://youtu.be/dQw4w9WgXcQ)."))

    @Test fun stopsAtGluedChineseText() =
        assertEquals("http://xhslink.com/a/AbCd123", UrlExtractor.extract("52 好看 http://xhslink.com/a/AbCd123，复制本条信息"))

    @Test fun takesFirstOfSeveral() =
        assertEquals("https://x.com/a/status/1", UrlExtractor.extract("https://x.com/a/status/1 and https://x.com/b/status/2"))

    @Test fun noLink() = assertNull(UrlExtractor.extract("no link here, just www.words"))
}

class PlatformDetectorTest {
    private fun detect(url: String) = PlatformDetector.detect(url)

    @Test fun shortAndLongHosts() {
        assertEquals(Platform.TIKTOK, detect("https://vm.tiktok.com/ZM1/"))
        assertEquals(Platform.TIKTOK, detect("https://www.tiktok.com/@a/video/1"))
        assertEquals(Platform.INSTAGRAM, detect("https://www.instagram.com/reel/abc/"))
        assertEquals(Platform.YOUTUBE, detect("https://youtu.be/abc"))
        assertEquals(Platform.YOUTUBE, detect("https://m.youtube.com/shorts/abc"))
        assertEquals(Platform.FACEBOOK, detect("https://fb.watch/abc/"))
        assertEquals(Platform.X, detect("https://twitter.com/a/status/1"))
        assertEquals(Platform.X, detect("https://t.co/abc"))
        assertEquals(Platform.PINTEREST, detect("https://pin.it/abc"))
        assertEquals(Platform.PINTEREST, detect("https://www.pinterest.co.uk/pin/1/"))
        assertEquals(Platform.XIAOHONGSHU, detect("https://www.xiaohongshu.com/explore/1"))
        assertEquals(Platform.XIAOHONGSHU, detect("http://xhslink.com/a/1"))
    }

    @Test fun lookalikeHostsAreNotPlatforms() {
        assertEquals(Platform.OTHER, detect("https://evil-tiktok.com/video/1"))
        assertEquals(Platform.OTHER, detect("https://notpinterest.com/pin/1"))
        assertEquals(Platform.OTHER, detect("https://example.com"))
        assertEquals(Platform.OTHER, detect("not a url"))
    }
}

class UrlNormalizerTest {
    private val ytKey = "https://youtube.com/watch?v=dQw4w9WgXcQ"

    @Test fun cleanStripsTrackingButKeepsMeaningfulParams() {
        assertEquals("https://www.youtube.com/shorts/abc123DEF", UrlNormalizer.clean("https://www.youtube.com/shorts/abc123DEF?si=XYZ&feature=share"))
        val red = UrlNormalizer.clean("https://www.xiaohongshu.com/discovery/item/64abc?xsec_token=T&utm_source=x#top")
        assertEquals("https://www.xiaohongshu.com/discovery/item/64abc?xsec_token=T", red)
    }

    @Test fun youtubeFormsShareOneKey() {
        assertEquals(ytKey, UrlNormalizer.normalize("https://youtu.be/dQw4w9WgXcQ?si=x"))
        assertEquals(ytKey, UrlNormalizer.normalize("https://m.youtube.com/watch?v=dQw4w9WgXcQ&t=10"))
        assertEquals(ytKey, UrlNormalizer.normalize("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
    }

    @Test fun tiktokDropsQueryAndWww() = assertEquals(
        "https://tiktok.com/@user/video/7234567890123456789",
        UrlNormalizer.normalize("https://www.tiktok.com/@user/video/7234567890123456789?is_from_webapp=1&sender_device=pc&_r=1"),
    )

    @Test fun instagramReelForms() {
        val key = "https://instagram.com/reel/C1abcDEF"
        assertEquals(key, UrlNormalizer.normalize("https://www.instagram.com/reels/C1abcDEF/?igsh=xyz"))
        assertEquals(key, UrlNormalizer.normalize("https://www.instagram.com/someone/reel/C1abcDEF/"))
    }

    @Test fun twitterBecomesXStatus() =
        assertEquals("https://x.com/i/status/1234567890", UrlNormalizer.normalize("https://twitter.com/someone/status/1234567890?s=20&t=abc"))

    @Test fun pinterestCountryDomainAndSentSuffix() =
        assertEquals("https://pinterest.com/pin/123456", UrlNormalizer.normalize("https://www.pinterest.co.uk/pin/123456/sent/?invite_code=x"))

    @Test fun facebookKeepsVideoId() =
        assertEquals("https://facebook.com/watch?v=987", UrlNormalizer.normalize("https://m.facebook.com/watch/?v=987&mibextid=abc"))

    @Test fun redDropsPerShareToken() = assertEquals(
        "https://xiaohongshu.com/discovery/item/64abc",
        UrlNormalizer.normalize("https://www.xiaohongshu.com/discovery/item/64abc?xsec_token=T&xsec_source=app_share"),
    )

    @Test fun genericSortsQueryAndDropsFragment() =
        assertEquals("https://example.com/video?a=1&b=2", UrlNormalizer.normalize("http://example.com/video/?b=2&a=1&utm_source=x#frag"))

    @Test fun shortLinks() {
        assertTrue(UrlNormalizer.isShortLink("https://vm.tiktok.com/ZM1/"))
        assertTrue(UrlNormalizer.isShortLink("https://www.tiktok.com/t/ZT1/"))
        assertTrue(UrlNormalizer.isShortLink("https://www.facebook.com/share/r/abc/"))
        assertTrue(UrlNormalizer.isShortLink("https://t.co/abc"))
        assertFalse(UrlNormalizer.isShortLink("https://youtu.be/abc")) // rewritten locally, no network needed
        assertFalse(UrlNormalizer.isShortLink("https://www.tiktok.com/@a/video/1"))
    }
}
