package com.janncarlo.kronosim.util

import com.janncarlo.kronosim.data.source.featureflag.FeatureFlagManager
import com.janncarlo.kronosim.data.source.featureflag.NoImplFeatureFlagManager
import com.janncarlo.kronosim.data.source.local.DatabaseProvider
import com.janncarlo.kronosim.data.source.local.DatabaseProviderImpl
import com.janncarlo.kronosim.data.source.local.databaseModule
import com.janncarlo.kronosim.data.source.preferences.PreferencesDataStoreProvider
import com.janncarlo.kronosim.data.source.preferences.PreferencesDataStoreProviderImpl
import com.janncarlo.kronosim.presentation.components.ads.AdsManager
import com.janncarlo.kronosim.presentation.components.ads.NoImplAdsManager
import com.janncarlo.kronosim.util.analytics.Analytics
import com.janncarlo.kronosim.util.analytics.NoImplAnalytics
import com.janncarlo.kronosim.util.file.FileManager
import com.janncarlo.kronosim.util.file.FileManagerImpl
import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import com.mmk.kmpnotifier.push.firebase.FirebasePush
import io.github.vinceglb.filekit.FileKit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    includes(databaseModule)
    singleOf(::DatabaseProviderImpl) bind DatabaseProvider::class
    singleOf(::PreferencesDataStoreProviderImpl) bind PreferencesDataStoreProvider::class
    factory<FileManager> { FileManagerImpl() }
    factoryOf(::AppUtilImpl) bind AppUtil::class
    single { NoImplFeatureFlagManager } bind FeatureFlagManager::class
    single { NoImplAnalytics } bind Analytics::class
    single { NoImplAdsManager } bind AdsManager::class
}

internal actual fun onApplicationStartPlatformSpecific() {
    KMPNotifier.initialize(NotificationPlatformConfiguration.Desktop(), FirebasePush)
    FileKit.init("com.janncarlo.kronosim")
}

actual fun getPlatform(): Platform = Platform.Desktop

internal actual val isAndroid: Boolean
    get() = false
internal actual val isDebug: Boolean
    get() = System.getProperty("app.debug") == "true" ||
        System.getenv("APP_DEBUG") == "true"

actual val defaultAsyncDispatcher: CoroutineDispatcher = Dispatchers.IO
