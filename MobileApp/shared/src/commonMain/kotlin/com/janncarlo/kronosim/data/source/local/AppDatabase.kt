package com.janncarlo.kronosim.data.source.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.janncarlo.kronosim.data.source.local.dao.CreditTransactionDao
import com.janncarlo.kronosim.data.source.local.dao.ExampleDao
import com.janncarlo.kronosim.data.source.local.dao.GenerationOutputDao
import com.janncarlo.kronosim.data.source.local.dao.SavedTopologyDao
import com.janncarlo.kronosim.data.source.local.entity.CreditTransactionEntity
import com.janncarlo.kronosim.data.source.local.entity.ExampleEntity
import com.janncarlo.kronosim.data.source.local.entity.GenerationOutputEntity
import com.janncarlo.kronosim.data.source.local.entity.SavedTopologyEntity

@Database(
    entities = [
        GenerationOutputEntity::class,
        CreditTransactionEntity::class,
        ExampleEntity::class,
        SavedTopologyEntity::class,
    ],
    version = 2,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun generationOutputDao(): GenerationOutputDao
    abstract fun creditTransactionDao(): CreditTransactionDao
    abstract fun exampleDao(): ExampleDao
    abstract fun savedTopologyDao(): SavedTopologyDao
}

interface DatabaseProvider {
    fun provideAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
