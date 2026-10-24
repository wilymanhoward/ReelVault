package io.github.wilymanhoward.reelvault.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Tag formats below are copied from the live pages checked while building Phase 3 (Oct 2026).
class OpenGraphParserTest {
    @Test fun readsPropertyAndNameStyleTags() {
        val og = OpenGraphParser.parse(
            """
            <html><head>
            <meta content="Live Pinning | Share button ideas" data-app="true" name="og:title" property="og:title"/>
            <meta property="og:description" content="How to share &amp; more" />
            <meta property="og:image" content="https://i.pinimg.com/736x/a7/66.jpg" />
            </head></html>
            """,
            "https://www.pinterest.com/pin/1/",
        )
        assertEquals("Live Pinning | Share button ideas", og.title)
        assertEquals("How to share & more", og.description)
        assertEquals("https://i.pinimg.com/736x/a7/66.jpg", og.image)
    }

    @Test fun resolvesProtocolRelativeAndRelativeImages() {
        assertEquals(
            "https://picasso-static.xiaohongshu.com/x.png",
            OpenGraphParser.parse("""<meta property="og:image" content="//picasso-static.xiaohongshu.com/x.png">""", "https://www.xiaohongshu.com/a").image,
        )
        assertEquals(
            "https://example.com/img/a.jpg",
            OpenGraphParser.parse("""<meta property="og:image" content="/img/a.jpg">""", "https://example.com/post/1").image,
        )
    }

    @Test fun emptyContentIsMissing() {
        val og = OpenGraphParser.parse("""<meta property="og:description" content=""/><title>Page</title>""", "https://x.com/a")
        assertNull(og.description)
        assertEquals("Page", og.title)
    }
}

class PreviewShapingTest {
    private fun og(title: String? = null, description: String? = null, image: String? = null, finalUrl: String = "https://a.com/") =
        OpenGraph(title, description, image, author = null, finalUrl = finalUrl)

    @Test fun tiktokAuthorFromTitle() {
        val m = PreviewShaping.tiktok(og(title = "TikTok · Scout, Suki & Stella", image = "img"))
        assertEquals("Scout, Suki & Stella", m.authorName)
        assertEquals("img", m.thumbnailUrl)
        assertNull(PreviewShaping.tiktok(og(title = "TikTok")).authorName)
    }

    @Test fun instagramSplitsAuthorAndCaption() {
        val m = PreviewShaping.instagram(og(title = "Chef Ana on Instagram: \"Pasta in 10 minutes\""))
        assertEquals("Chef Ana", m.authorName)
        assertEquals("Pasta in 10 minutes", m.caption)
    }

    @Test fun instagramProfileStyleTitle() {
        val m = PreviewShaping.instagram(og(title = "Instagram (@instagram) • Instagram photos and videos", description = "686M Followers"))
        assertEquals("Instagram", m.authorName)
        assertEquals("686M Followers", m.caption)
    }

    @Test fun facebookPrefersDescription() {
        assertEquals("How to share", PreviewShaping.facebook(og(title = "2.8K views · 1.3K reactions | How to share | Facebook", description = "How to share")).caption)
        assertEquals("How to share", PreviewShaping.facebook(og(title = "2.8K views · 1.3K reactions | How to share | Facebook")).caption)
    }

    @Test fun xPostTextFromEmbedHtml() = assertEquals(
        "just setting up my twttr",
        PreviewShaping.xPostText("""<blockquote class="twitter-tweet"><p lang="en" dir="ltr">just setting up my twttr</p>&mdash; jack (@jack) <a href="https://x.com/jack/status/20">March 21, 2006</a></blockquote>"""),
    )

    @Test fun redMissingPageDetected() {
        assertTrue(PreviewShaping.isRedMissingPage(og(finalUrl = "https://www.xiaohongshu.com/404?source=/404/sec_x&error_code=300031")))
        assertFalse(PreviewShaping.isRedMissingPage(og(finalUrl = "https://www.xiaohongshu.com/explore/66b5")))
    }
}
