package io.github.wilymanhoward.reelvault.ui.common

import io.github.wilymanhoward.reelvault.domain.repository.CollectionRepository
import io.github.wilymanhoward.reelvault.domain.repository.ItemRepository
import io.github.wilymanhoward.reelvault.domain.repository.PreviewScheduler
import io.github.wilymanhoward.reelvault.domain.repository.TagRepository
import javax.inject.Inject

/** Actions every list of saves offers: delete with undo, batch move and tag, preview refresh. */
class BatchActions @Inject constructor(
    private val items: ItemRepository,
    private val collections: CollectionRepository,
    private val tags: TagRepository,
    private val previews: PreviewScheduler,
) {
    // One refresh per item per screen lifetime, so a thumbnail that stays dead can't loop refreshes.
    private val refreshed = mutableSetOf<String>()

    suspend fun delete(ids: Collection<String>) = items.delete(ids)
    suspend fun restore(ids: Collection<String>) = items.restore(ids)
    suspend fun addTag(ids: Collection<String>, name: String) = tags.addTag(ids, name)
    suspend fun addToCollection(ids: Collection<String>, collectionId: String) = collections.addItems(ids, collectionId)

    suspend fun addToNewCollection(ids: Collection<String>, name: String) {
        if (name.isBlank()) return
        collections.addItems(ids, collections.create(name).id)
    }

    /** A card's remote thumbnail answered 403/404: re-fetch the preview for a fresh URL. */
    fun onThumbnailGone(itemId: String) {
        if (refreshed.add(itemId)) previews.schedule(itemId)
    }
}
