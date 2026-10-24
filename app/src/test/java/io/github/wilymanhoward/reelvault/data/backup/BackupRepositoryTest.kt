package io.github.wilymanhoward.reelvault.data.backup

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.wilymanhoward.reelvault.data.local.ReelVaultDatabase
import io.github.wilymanhoward.reelvault.data.repository.RoomCollectionRepository
import io.github.wilymanhoward.reelvault.data.repository.RoomItemRepository
import io.github.wilymanhoward.reelvault.data.repository.RoomTagRepository
import io.github.wilymanhoward.reelvault.di.SeedDefaultRules
import io.github.wilymanhoward.reelvault.domain.model.Platform
import io.github.wilymanhoward.reelvault.domain.model.SearchFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class BackupRepositoryTest {
    private fun newDb() = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ReelVaultDatabase::class.java)
        .addCallback(SeedDefaultRules)
        .allowMainThreadQueries()
        .build()

    private val source = newDb()
    private val target = newDb()

    @After fun tearDown() {
        source.close()
        target.close()
    }

    @Test fun roundTripKeepsNotesTagsAndCollections() = runTest {
        val items = RoomItemRepository(source)
        val saved = items.save("https://www.tiktok.com/@a/video/1", "tiktok.com/@a/video/1", Platform.TIKTOK)
        items.setNotes(saved.id, "try with chili")
        RoomTagRepository(source).addTag(listOf(saved.id), "dinner")
        val trip = RoomCollectionRepository(source).create("Food", "🍝", 0xFFE8475F)
        RoomCollectionRepository(source).setMembership(saved.id, trip.id, member = true)
        items.save("https://youtu.be/x", "youtube.com/watch?v=x", Platform.YOUTUBE).also { items.delete(listOf(it.id)) }

        val json = BackupRepository(source).export(now = 1)
        val result = BackupRepository(target).import(json, now = 2)

        assertEquals("deleted saves aren't exported", 1, result.addedIds.size)
        assertEquals(1, result.collections)
        val restored = RoomItemRepository(target).search("dinner", SearchFilter()).first().single()
        assertEquals("try with chili", restored.notes)
        val col = RoomCollectionRepository(target).observeCollections().first().single()
        assertEquals("Food" to "🍝", col.name to col.emoji)
        assertEquals(1, col.itemCount)
    }

    @Test fun importingTwiceSkipsWhatIsAlreadyThere() = runTest {
        RoomItemRepository(source).save("https://x.com/a/status/1", "x.com/i/status/1", Platform.X)
        val json = BackupRepository(source).export()
        BackupRepository(target).import(json)
        val again = BackupRepository(target).import(json)
        assertTrue(again.addedIds.isEmpty())
        assertEquals(1, again.skipped)
        // Default rules aren't duplicated either.
        assertEquals(11, RoomTagRepository(target).observeRules().first().size)
    }

    @Test fun rejectsOtherFiles() = runTest {
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { BackupRepository(target).import("{\"hello\": 1}") } }
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { BackupRepository(target).import("not json") } }
    }
}
