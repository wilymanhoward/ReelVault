package io.github.wilymanhoward.reelvault.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.wilymanhoward.reelvault.data.local.ReelVaultDatabase
import io.github.wilymanhoward.reelvault.di.SeedDefaultRules
import io.github.wilymanhoward.reelvault.domain.model.Platform
import io.github.wilymanhoward.reelvault.domain.model.PreviewState
import io.github.wilymanhoward.reelvault.domain.model.SearchFilter
import io.github.wilymanhoward.reelvault.domain.model.TagSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class FtsMatchTest {
    @Test fun wordsBecomePrefixTerms() = assertEquals("pasta* night*", ftsMatch("  Pasta, NIGHT! "))
    // Quotes, '-' and '*' are dropped; "OR" survives only as the lowercase term or*, which FTS4 doesn't treat as an operator.
    @Test fun operatorsAndQuotesDropped() = assertEquals("a* or* b*", ftsMatch("\"a\" OR -b*"))
    @Test fun nothingLeft() = assertNull(ftsMatch(" ,.!? "))
}

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TagAndSearchTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ReelVaultDatabase::class.java)
        .addCallback(SeedDefaultRules)
        .allowMainThreadQueries()
        .build()
    private val items = RoomItemRepository(db)
    private val tags = RoomTagRepository(db)
    private val collections = RoomCollectionRepository(db)

    @After fun tearDown() = db.close()

    private suspend fun saveWithCaption(key: String, caption: String, platform: Platform = Platform.TIKTOK): String {
        val item = items.save("https://example.com/$key", key, platform)
        db.itemDao().updatePreview(item.id, null, caption, "Chef", null, null, null, PreviewState.READY, 1)
        return item.id
    }

    @Test fun defaultRulesAreSeeded() = runTest {
        assertEquals(11, tags.observeRules().first().size)
    }

    @Test fun autoTagsKeepManualTagsAndAreSearchable() = runTest {
        val id = saveWithCaption("k1", "Easy recipe #weeknight")
        tags.addTag(listOf(id), "#mine")
        tags.applyAutoTags(id)

        val result = tags.observeTags(id).first().associate { it.name to it.source }
        assertEquals(mapOf("Food" to TagSource.AUTO, "weeknight" to TagSource.AUTO, "mine" to TagSource.MANUAL), result)
        assertEquals(listOf(id), items.search("weekn", SearchFilter()).first().map { it.id })
        assertEquals(listOf(id), items.search("mine", SearchFilter()).first().map { it.id })
    }

    @Test fun renameMergesAndBecomesManual() = runTest {
        val a = saveWithCaption("k1", "pasta")
        val b = saveWithCaption("k2", "nothing here")
        tags.applyAutoTags(a) // Food (auto)
        tags.addTag(listOf(b), "Cooking")
        tags.renameTag("Food", "Cooking")

        assertEquals(listOf("Cooking" to 2), tags.observeAllTags().first().map { it.name to it.count })
        assertEquals(TagSource.MANUAL, tags.observeTags(a).first().single().source)
        tags.applyAutoTags(a) // re-running rules adds Food back but keeps the renamed tag
        assertEquals(setOf("Cooking", "Food"), tags.observeTags(a).first().map { it.name }.toSet())
    }

    @Test fun filtersCombine() = runTest {
        val a = saveWithCaption("k1", "pasta", Platform.TIKTOK)
        val b = saveWithCaption("k2", "pasta", Platform.YOUTUBE)
        val trip = collections.create("Trip")
        collections.setMembership(b, trip.id, member = true)

        assertEquals(setOf(a, b), items.search("", SearchFilter()).first().map { it.id }.toSet())
        assertEquals(listOf(b), items.search("pasta", SearchFilter(platforms = setOf(Platform.YOUTUBE))).first().map { it.id })
        assertEquals(listOf(b), items.search("", SearchFilter(collectionId = trip.id)).first().map { it.id })
        assertTrue(items.search("", SearchFilter(collectionId = trip.id, platforms = setOf(Platform.TIKTOK))).first().isEmpty())
    }

    @Test fun collectionStatsUseFirstItemAsCoverUntilChosen() = runTest {
        val a = saveWithCaption("k1", "x")
        val b = saveWithCaption("k2", "y")
        db.itemDao().updatePreview(a, null, "x", null, null, "https://img/a", null, PreviewState.READY, 2)
        db.itemDao().updatePreview(b, null, "y", null, null, "https://img/b", null, PreviewState.READY, 2)
        val c = collections.create("C")
        collections.setMembership(a, c.id, true)
        Thread.sleep(2)
        collections.setMembership(b, c.id, true)

        val first = collections.observeCollections().first().single()
        assertEquals(2, first.itemCount)
        assertEquals("https://img/a", first.coverImage)

        collections.setCover(c.id, b)
        assertEquals("https://img/b", collections.observeCollections().first().single().coverImage)

        items.delete(listOf(a))
        assertEquals(1, collections.observeCollections().first().single().itemCount)
    }

    @Test fun reorder() = runTest {
        val x = collections.create("X")
        val y = collections.create("Y")
        collections.reorder(listOf(y.id, x.id))
        assertEquals(listOf("Y", "X"), collections.observeCollections().first().map { it.name })
    }
}
