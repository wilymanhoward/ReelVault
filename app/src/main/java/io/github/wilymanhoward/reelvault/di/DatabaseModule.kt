package io.github.wilymanhoward.reelvault.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.wilymanhoward.reelvault.data.local.ReelVaultDatabase
import javax.inject.Singleton

/** Provides the Room database and its DAOs. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): ReelVaultDatabase =
        Room.databaseBuilder(context, ReelVaultDatabase::class.java, "reelvault.db").build()

    @Provides fun itemDao(db: ReelVaultDatabase) = db.itemDao()
    @Provides fun collectionDao(db: ReelVaultDatabase) = db.collectionDao()
    @Provides fun tagDao(db: ReelVaultDatabase) = db.tagDao()
}
