package io.github.wilymanhoward.reelvault.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.wilymanhoward.reelvault.data.local.ReelVaultDatabase
import io.github.wilymanhoward.reelvault.domain.model.Platform
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

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RoomRepositoriesTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ReelVaultDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val items = RoomItemRepository(db)
    private val collections = RoomCollectionRepository(db)

    @After fun tearDown() = db.close()

    @Test fun savingASoftDeletedLinkRevivesTheSameRow() = runTest {
        val first = items.save("https://x.com/a/status/1", "key", Platform.X)
        db.itemDao().upsert(db.itemDao().findByNormalizedUrl("key")!!.copy(deleted = true))
        assertNull(items.findByNormalizedUrl("key"))

        val again = items.save("https://x.com/a/status/1?s=20", "key", Platform.X)
        assertEquals(first.id, again.id)
        assertEquals(listOf(first.id), items.observeItems().first().map { it.id })
    }

    @Test fun membershipTogglesAndBumpsItemUpdatedAt() = runTest {
        val item = items.save("https://youtu.be/a", "yt", Platform.YOUTUBE)
        val before = db.itemDao().findByNormalizedUrl("yt")!!.updatedAt
        val food = collections.create("  Food ")
        assertEquals("Food", food.name)

        Thread.sleep(2)
        collections.setMembership(item.id, food.id, member = true)
        assertEquals(setOf(food.id), collections.observeCollectionIdsFor(item.id).first())
        assertTrue(db.itemDao().findByNormalizedUrl("yt")!!.updatedAt > before)

        collections.setMembership(item.id, food.id, member = false)
        assertTrue(collections.observeCollectionIdsFor(item.id).first().isEmpty())
    }

    @Test fun newCollectionsGoLast() = runTest {
        collections.create("A")
        collections.create("B")
        assertEquals(listOf("A", "B"), collections.observeCollections().first().map { it.name })
    }
}
