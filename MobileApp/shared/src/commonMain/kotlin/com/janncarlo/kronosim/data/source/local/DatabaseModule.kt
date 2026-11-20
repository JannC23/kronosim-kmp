package com.janncarlo.kronosim.data.source.local

import com.janncarlo.kronosim.util.defaultAsyncDispatcher
import org.koin.dsl.module

val databaseModule = module {
    single<AppDatabase> {
        get<DatabaseProvider>()
            .provideAppDatabaseBuilder()
            .fallbackToDestructiveMigration(dropAllTables = true)
            .setQueryCoroutineContext(defaultAsyncDispatcher)
            .build()
    }
    single { get<AppDatabase>().exampleDao() }
    single { get<AppDatabase>().creditTransactionDao() }
    single { get<AppDatabase>().generationOutputDao() }
    single { get<AppDatabase>().savedTopologyDao() }
}
