package io.github.wilymanhoward.reelvault.data.sync

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.wilymanhoward.reelvault.data.local.ReelVaultDatabase
import io.github.wilymanhoward.reelvault.data.local.SavedItemEntity
import io.github.wilymanhoward.reelvault.data.repository.RoomCollectionRepository
import io.github.wilymanhoward.reelvault.data.repository.RoomTagRepository
import io.github.wilymanhoward.reelvault.domain.model.Platform
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** In-memory cloud: each write gets the next "server time". */
private class FakeRemote : RemoteStore {
    val items = mutableMapOf<String, Pulled<RemoteItem>>()
    val collections = mutableMapOf<String, Pulled<RemoteCollection>>()
    var pushedItems = mutableListOf<RemoteItem>()
    private var serverClock = 1_000L

    fun putItem(item: RemoteItem) { items[item.id] = Pulled(item, ++serverClock) }

    override suspend fun pullItems(uid: String, sinceServerMs: Long) = items.values.filter { it.serverUpdatedAt > sinceServerMs }.sortedBy { it.serverUpdatedAt }
    override suspend fun pullCollections(uid: String, sinceServerMs: Long) = collections.values.filter { it.serverUpdatedAt > sinceServerMs }.sortedBy { it.serverUpdatedAt }
    override suspend fun pushItems(uid: String, items: List<RemoteItem>) { pushedItems.addAll(items); items.forEach(::putItem) }
    override suspend fun pushCollections(uid: String, collections: List<RemoteCollection>) {
        collections.forEach { this.collections[it.id] = Pulled(it, ++serverClock) }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SyncEngineTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db = Room.inMemoryDatabaseBuilder(context, ReelVaultDatabase::class.java).allowMainThreadQueries().build()
    private val remote = FakeRemote()
    private val state = SyncStateStore(context)
    private val previews = mutableListOf<String>()
    private val engine = SyncEngine(db, remote, state) { previews += it }.apply { clock = { 5_000_000 } }

    @Before fun setUp() = runTest { state.reset() }
    @After fun tearDown() = db.close()

    private fun local(id: String, key: String, updatedAt: Long, title: String = "local") = SavedItemEntity(
        id = id, url = "https://x.com/$key", normalizedUrl = key, platform = Platform.X, title = title,
        createdAt = 1, updatedAt = updatedAt,
    )

    private fun remoteItem(id: String, key: String, updatedAt: Long, title: String = "remote", deleted: Boolean = false) = RemoteItem(
        id = id, url = "https://x.com/$key", normalizedUrl = key, platform = "X", title = title, caption = null,
        authorName = null, authorUrl = null, thumbnailUrl = null, notes = "", imageLabels = "", isFavorite = false,
        isPinned = false, createdAt = 1, updatedAt = updatedAt, deleted = deleted, collectionIds = emptyList(),
        tags = listOf(RemoteTag("fromCloud", "MANUAL")),
    )

    @Test fun pushesLocalChangesWithMembershipAndTagsOnce() = runTest {
        db.itemDao().upsert(local("i1", "k1", updatedAt = 10))
        val trip = RoomCollectionRepository(db).create("Trip")
        RoomCollectionRepository(db).setMembership("i1", trip.id, member = true)
        RoomTagRepository(db).addTag(listOf("i1"), "beach")

        engine.sync("u")
        val pushed = remote.items.getValue("i1").value
        assertEquals(listOf(trip.id), pushed.collectionIds)
        assertEquals(listOf(RemoteTag("beach", "MANUAL")), pushed.tags)
        assertTrue(remote.collections.containsKey(trip.id))

        remote.pushedItems.clear()
        engine.sync("u")
        // Only echoes of rows pulled back (unchanged data) may be pushed again; nothing new locally.
        assertTrue(remote.pushedItems.none { it.updatedAt > pushed.updatedAt })
    }

    @Test fun newerRemoteWinsOlderRemoteLoses() = runTest {
        db.itemDao().upsert(local("i1", "k1", updatedAt = 100, title = "mine"))
        remote.putItem(remoteItem("i1", "k1", updatedAt = 50, title = "stale"))
        engine.sync("u")
        assertEquals("mine", db.itemDao().getById("i1")?.title)

        remote.putItem(remoteItem("i1", "k1", updatedAt = 200, title = "newer"))
        engine.sync("u")
        val row = db.itemDao().getById("i1")!!
        assertEquals("newer", row.title)
        assertEquals("fromCloud", row.tagsText)
    }

    @Test fun duplicateKeepsSmallerIdRemoteSmaller() = runTest {
        db.itemDao().upsert(local("b-local", "same", updatedAt = 10))
        remote.putItem(remoteItem("a-remote", "same", updatedAt = 10))
        engine.sync("u")

        assertNull(db.itemDao().getById("b-local"))
        assertEquals("a-remote", db.itemDao().findByNormalizedUrl("same")?.id)
        assertTrue(remote.items.getValue("b-local").value.deleted)
    }

    @Test fun duplicateKeepsSmallerIdLocalSmaller() = runTest {
        db.itemDao().upsert(local("a-local", "same", updatedAt = 10))
        remote.putItem(remoteItem("b-remote", "same", updatedAt = 10))
        engine.sync("u")

        assertEquals("a-local", db.itemDao().findByNormalizedUrl("same")?.id)
        assertTrue(remote.items.getValue("b-remote").value.deleted)
        // The tombstone comes back on the next pull and must not churn.
        remote.pushedItems.clear()
        engine.sync("u")
        assertTrue(remote.pushedItems.none { it.id == "b-remote" })
    }

    @Test fun unknownRemoteDeletionIsIgnoredAndNewItemsQueuePreviews() = runTest {
        remote.putItem(remoteItem("gone", "k-gone", updatedAt = 10, deleted = true))
        remote.putItem(remoteItem("new", "k-new", updatedAt = 10).copy(title = null, thumbnailUrl = null))
        engine.sync("u")

        assertNull(db.itemDao().getById("gone"))
        assertFalse(db.itemDao().getById("new")!!.deleted)
        assertEquals(listOf("new"), previews)
    }

    @Test fun switchingAccountsStartsOver() = runTest {
        remote.putItem(remoteItem("i1", "k1", updatedAt = 10))
        engine.sync("u")
        assertEquals(0, state.load("someone-else").pulledItems)
    }
}
