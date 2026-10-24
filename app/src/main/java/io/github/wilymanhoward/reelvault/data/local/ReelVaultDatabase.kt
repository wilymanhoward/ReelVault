package io.github.wilymanhoward.reelvault.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/** The on-device database: the app's single source of truth. */
@Database(
    entities = [
        SavedItemEntity::class,
        SavedItemFts::class,
        CollectionEntity::class,
        CollectionItemCrossRef::class,
        ItemTagEntity::class,
        TagRuleEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ReelVaultDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun collectionDao(): CollectionDao
    abstract fun tagDao(): TagDao
}
