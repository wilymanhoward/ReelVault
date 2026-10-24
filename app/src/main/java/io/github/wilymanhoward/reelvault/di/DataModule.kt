package io.github.wilymanhoward.reelvault.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.wilymanhoward.reelvault.data.remote.OkHttpShortLinkResolver
import io.github.wilymanhoward.reelvault.data.repository.RoomCollectionRepository
import io.github.wilymanhoward.reelvault.data.repository.RoomItemRepository
import io.github.wilymanhoward.reelvault.domain.repository.CollectionRepository
import io.github.wilymanhoward.reelvault.domain.repository.ItemRepository
import io.github.wilymanhoward.reelvault.domain.repository.ShortLinkResolver
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** Binds domain interfaces to their data-layer implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds abstract fun itemRepository(impl: RoomItemRepository): ItemRepository
    @Binds abstract fun collectionRepository(impl: RoomCollectionRepository): CollectionRepository
    @Binds abstract fun shortLinkResolver(impl: OkHttpShortLinkResolver): ShortLinkResolver

    companion object {
        @Provides
        @Singleton
        fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
