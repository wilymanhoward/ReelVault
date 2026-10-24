package io.github.wilymanhoward.reelvault.di

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.wilymanhoward.reelvault.data.local.ReelVaultDatabase
import io.github.wilymanhoward.reelvault.domain.tagging.AutoTagger
import javax.inject.Singleton

/** Provides the Room database and its DAOs. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): ReelVaultDatabase =
        Room.databaseBuilder(context, ReelVaultDatabase::class.java, "reelvault.db")
            .addCallback(SeedDefaultRules)
            .build()

    @Provides fun itemDao(db: ReelVaultDatabase) = db.itemDao()
    @Provides fun collectionDao(db: ReelVaultDatabase) = db.collectionDao()
    @Provides fun tagDao(db: ReelVaultDatabase) = db.tagDao()
}

/** Ships the default auto-tagging rules in a fresh database. */
internal object SeedDefaultRules : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        AutoTagger.DEFAULT_RULES.forEach { rule ->
            val values = ContentValues().apply {
                put("category", rule.category)
                put("keywords", rule.keywords.joinToString(", "))
                put("enabled", 1)
            }
            db.insert("tag_rules", SQLiteDatabase.CONFLICT_ABORT, values)
        }
    }
}
