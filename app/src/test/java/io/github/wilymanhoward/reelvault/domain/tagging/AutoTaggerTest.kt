package io.github.wilymanhoward.reelvault.domain.tagging

import io.github.wilymanhoward.reelvault.domain.model.Platform
import io.github.wilymanhoward.reelvault.domain.model.PreviewState
import io.github.wilymanhoward.reelvault.domain.model.SavedItem
import io.github.wilymanhoward.reelvault.domain.model.SortOrder
import io.github.wilymanhoward.reelvault.domain.model.TagRule
import io.github.wilymanhoward.reelvault.domain.model.sortedFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoTaggerTest {
    private val rules = AutoTagger.DEFAULT_RULES

    @Test fun hashtagsLowercasedAndNumbersOnlySkipped() =
        assertEquals(setOf("fyp", "pastanight", "größe"), AutoTagger.hashtags("Yum #FYP #PastaNight #2024 #Größe"))

    @Test fun keywordsMapToCategoriesWithPlurals() {
        assertEquals(setOf("Food"), AutoTagger.categories("3 easy recipes for dinner", rules))
        assertEquals(setOf("Fitness"), AutoTagger.categories("My leg day workouts", rules))
    }

    @Test fun shortKeywordsNeedExactWords() {
        assertTrue(AutoTagger.categories("fresh air and aid", rules).isEmpty())
        assertEquals(setOf("Tech"), AutoTagger.categories("new AI phone", rules))
    }

    @Test fun multiWordKeyword() = assertEquals(setOf("Finance"), AutoTagger.categories("My side hustle story", rules))

    @Test fun disabledRulesIgnored() {
        val off = listOf(TagRule(category = "Food", keywords = listOf("pasta"), enabled = false))
        assertTrue(AutoTagger.categories("pasta", off).isEmpty())
    }

    @Test fun imageLabelsAndHashtagsFeedCategories() {
        val tags = AutoTagger.autoTags(title = null, caption = "Sunday vibes #food #beach", imageLabels = listOf("Pasta"), rules = rules)
        // #food repeats the Food category so it's dropped; #beach stays and also triggers Travel.
        assertEquals(setOf("Food", "Travel", "beach"), tags)
    }
}

class SortOrderTest {
    private fun item(id: String, created: Long, title: String?, platform: Platform = Platform.TIKTOK, pinned: Boolean = false) = SavedItem(
        id = id, url = "u$id", normalizedUrl = "n$id", platform = platform, title = title, caption = null, authorName = null,
        thumbnailUrl = null, localThumbnailPath = null, notes = "", previewState = PreviewState.READY, isFavorite = false,
        isPinned = pinned, createdAt = created,
    )

    private val items = listOf(
        item("a", 1, "banana", Platform.YOUTUBE),
        item("b", 3, "Apple", Platform.INSTAGRAM),
        item("c", 2, "cherry", Platform.TIKTOK, pinned = true),
    )

    @Test fun pinnedFirstThenOrder() {
        assertEquals(listOf("c", "b", "a"), items.sortedFor(SortOrder.NEWEST).map { it.id })
        assertEquals(listOf("c", "a", "b"), items.sortedFor(SortOrder.OLDEST).map { it.id })
        assertEquals(listOf("c", "b", "a"), items.sortedFor(SortOrder.TITLE).map { it.id })
        assertEquals(listOf("c", "b", "a"), items.sortedFor(SortOrder.PLATFORM).map { it.id })
    }
}
