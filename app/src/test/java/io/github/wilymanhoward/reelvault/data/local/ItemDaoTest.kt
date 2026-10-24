package io.github.wilymanhoward.reelvault.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.wilymanhoward.reelvault.domain.model.Platform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ItemDaoTest {
    private lateinit var db: ReelVaultDatabase
    private lateinit var dao: ItemDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ReelVaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.itemDao()
    }

    @After
    fun tearDown() = db.close()

    private fun item(id: String, title: String, tags: String = "", deleted: Boolean = false) = SavedItemEntity(
        id = id,
        url = "https://example.com/$id",
        normalizedUrl = "https://example.com/$id",
        platform = Platform.TIKTOK,
        title = title,
        tagsText = tags,
        createdAt = 1,
        updatedAt = 1,
        deleted = deleted,
    )

    @Test
    fun ftsFindsTitleAndTagsAndSkipsDeleted() = runTest {
        dao.upsert(item("a", "Easy pasta recipe", tags = "Food"))
        dao.upsert(item("b", "Leg day workout", tags = "Fitness"))
        dao.upsert(item("c", "Pasta deleted", deleted = true))

        assertEquals(listOf("a"), dao.search("pasta*").first().map { it.id })
        assertEquals(listOf("b"), dao.search("fitness").first().map { it.id })
    }

    @Test
    fun ftsFollowsUpdates() = runTest {
        dao.upsert(item("a", "Old title"))
        dao.upsert(item("a", "New title"))

        assertTrue(dao.search("old").first().isEmpty())
        assertEquals(listOf("a"), dao.search("new").first().map { it.id })
    }

    @Test
    fun findByNormalizedUrlIncludesSoftDeleted() = runTest {
        dao.upsert(item("a", "Gone", deleted = true))

        assertEquals("a", dao.findByNormalizedUrl("https://example.com/a")?.id)
        assertTrue(dao.observeItems().first().isEmpty())
    }
}
