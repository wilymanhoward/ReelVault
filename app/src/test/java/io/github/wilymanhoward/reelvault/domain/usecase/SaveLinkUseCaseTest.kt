package io.github.wilymanhoward.reelvault.domain.usecase

import io.github.wilymanhoward.reelvault.domain.model.Platform
import io.github.wilymanhoward.reelvault.domain.model.PreviewState
import io.github.wilymanhoward.reelvault.domain.model.SavedItem
import io.github.wilymanhoward.reelvault.domain.model.SearchFilter
import io.github.wilymanhoward.reelvault.domain.repository.ItemRepository
import io.github.wilymanhoward.reelvault.domain.repository.PreviewScheduler
import io.github.wilymanhoward.reelvault.domain.repository.ShortLinkResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveLinkUseCaseTest {
    private class FakeItems : ItemRepository {
        val byKey = mutableMapOf<String, SavedItem>()
        override fun observeItems(): Flow<List<SavedItem>> = emptyFlow()
        override suspend fun findByNormalizedUrl(normalizedUrl: String) = byKey[normalizedUrl]
        override suspend fun save(url: String, normalizedUrl: String, platform: Platform) = SavedItem(
            id = "id${byKey.size}", url = url, normalizedUrl = normalizedUrl, platform = platform, title = null,
            caption = null, authorName = null, thumbnailUrl = null, localThumbnailPath = null, notes = "",
            previewState = PreviewState.PENDING, isFavorite = false, isPinned = false, createdAt = 0,
        ).also { byKey[normalizedUrl] = it }

        // Not used by SaveLinkUseCase.
        override fun observeItem(id: String): Flow<SavedItem?> = emptyFlow()
        override fun search(query: String, filter: SearchFilter): Flow<List<SavedItem>> = emptyFlow()
        override suspend fun setNotes(id: String, notes: String) = Unit
        override suspend fun setFavorite(id: String, favorite: Boolean) = Unit
        override suspend fun setPinned(id: String, pinned: Boolean) = Unit
        override suspend fun delete(ids: Collection<String>) = Unit
        override suspend fun restore(ids: Collection<String>) = Unit
    }

    private val items = FakeItems()
    private val resolved = mutableListOf<String>()
    private var redirectTo: String? = "https://www.tiktok.com/@chef/video/42?_r=1&is_from_webapp=1"
    private val scheduled = mutableListOf<String>()
    private val useCase = SaveLinkUseCase(items, ShortLinkResolver { resolved += it; redirectTo }, PreviewScheduler { scheduled += it })

    @Test fun resolvesShortLinkAndStoresCleanedUrl() = runTest {
        val result = useCase("Best pasta ever https://vm.tiktok.com/ZM42/ #food") as SaveResult.Saved
        assertEquals(listOf("https://vm.tiktok.com/ZM42/"), resolved)
        assertEquals("https://www.tiktok.com/@chef/video/42", result.item.url)
        assertEquals("https://tiktok.com/@chef/video/42", result.item.normalizedUrl)
        assertEquals(Platform.TIKTOK, result.item.platform)
    }

    @Test fun secondShareOfSameVideoIsDuplicate() = runTest {
        useCase("https://vm.tiktok.com/ZM42/")
        val again = useCase("https://www.tiktok.com/@chef/video/42?lang=en")
        assertTrue(again is SaveResult.AlreadySaved)
        assertEquals("only the first save fetches a preview", 1, scheduled.size)
    }

    @Test fun deadShortLinkKeepsOriginal() = runTest {
        redirectTo = "https://www.tiktok.com/?_r=1"
        val result = useCase("https://vm.tiktok.com/ZMdead/") as SaveResult.Saved
        assertEquals("https://vm.tiktok.com/ZMdead/", result.item.url)
    }

    @Test fun offlineKeepsOriginal() = runTest {
        redirectTo = null
        val result = useCase("https://pin.it/abc") as SaveResult.Saved
        assertEquals("https://pin.it/abc", result.item.url)
        assertEquals(Platform.PINTEREST, result.item.platform)
    }

    @Test fun longLinksSkipNetwork() = runTest {
        useCase("https://youtu.be/dQw4w9WgXcQ")
        assertTrue(resolved.isEmpty())
    }

    @Test fun noLink() = runTest { assertEquals(SaveResult.NoLink, useCase("just words")) }
}
