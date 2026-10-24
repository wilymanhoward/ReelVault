package io.github.wilymanhoward.reelvault.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.wilymanhoward.reelvault.data.preview.WorkManagerPreviewScheduler
import io.github.wilymanhoward.reelvault.data.remote.NetworkPreviewResolver
import io.github.wilymanhoward.reelvault.data.remote.OEmbedApi
import io.github.wilymanhoward.reelvault.data.remote.OkHttpShortLinkResolver
import io.github.wilymanhoward.reelvault.data.repository.RoomCollectionRepository
import io.github.wilymanhoward.reelvault.data.repository.RoomItemRepository
import io.github.wilymanhoward.reelvault.domain.repository.CollectionRepository
import io.github.wilymanhoward.reelvault.domain.repository.ItemRepository
import io.github.wilymanhoward.reelvault.domain.repository.PreviewResolver
import io.github.wilymanhoward.reelvault.domain.repository.PreviewScheduler
import io.github.wilymanhoward.reelvault.domain.repository.ShortLinkResolver
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** Binds domain interfaces to their data-layer implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds abstract fun itemRepository(impl: RoomItemRepository): ItemRepository
    @Binds abstract fun collectionRepository(impl: RoomCollectionRepository): CollectionRepository
    @Binds abstract fun shortLinkResolver(impl: OkHttpShortLinkResolver): ShortLinkResolver
    @Binds abstract fun previewResolver(impl: NetworkPreviewResolver): PreviewResolver
    @Binds abstract fun previewScheduler(impl: WorkManagerPreviewScheduler): PreviewScheduler

    companion object {
        @Provides
        @Singleton
        fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        @Provides
        @Singleton
        fun oEmbedApi(client: OkHttpClient): OEmbedApi = Retrofit.Builder()
            .baseUrl("https://www.youtube.com/") // required by Retrofit; every call passes its full endpoint
            .client(client)
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OEmbedApi::class.java)
    }
}
