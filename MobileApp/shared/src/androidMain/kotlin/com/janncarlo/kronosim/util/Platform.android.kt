package com.janncarlo.kronosim.util

import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.janncarlo.kronosim.common.BuildConfig
import com.janncarlo.kronosim.data.source.featureflag.FeatureFlagManager
import com.janncarlo.kronosim.data.source.featureflag.FeatureFlagManagerImpl
import com.janncarlo.kronosim.data.source.local.DatabaseProvider
import com.janncarlo.kronosim.data.source.local.DatabaseProviderImpl
import com.janncarlo.kronosim.data.source.local.databaseModule
import com.janncarlo.kronosim.data.source.preferences.PreferencesDataStoreProvider
import com.janncarlo.kronosim.data.source.preferences.PreferencesDataStoreProviderImpl
import com.janncarlo.kronosim.presentation.components.ads.AdsManager
import com.janncarlo.kronosim.presentation.components.ads.AdsManagerImpl
import com.janncarlo.kronosim.shared.R
import com.janncarlo.kronosim.util.analytics.Analytics
import com.janncarlo.kronosim.util.analytics.FirebaseAnalyticsImpl
import com.janncarlo.kronosim.util.file.FileManager
import com.janncarlo.kronosim.util.file.FileManagerImpl
import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import com.mmk.kmpnotifier.push.firebase.FirebasePush
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
    single<FeatureFlagManagerImpl> {
        val remoteConfig = Firebase.remoteConfig.apply {
            setConfigSettingsAsync(
                remoteConfigSettings {
                    // set minimumFetchIntervalInSeconds to 0 to get fresh updates in debug mode for testing
                    if (BuildConfig.DEBUG) minimumFetchIntervalInSeconds = 3600
                },
            )
            setDefaultsAsync(FeatureFlagManager.DEFAULT_VALUES)
        }
        FeatureFlagManagerImpl(remoteConfig = remoteConfig)
    } bind FeatureFlagManager::class
    single { FirebaseAnalyticsImpl(firebaseAnalytics = Firebase.analytics) } bind Analytics::class
    singleOf(::AdsManagerImpl) bind AdsManager::class
}

internal actual fun onApplicationStartPlatformSpecific() {
    KMPNotifier.initialize(
        NotificationPlatformConfiguration.Android(
            notificationIconResId = R.drawable.ic_notification,
        ),
        FirebasePush,
    )
}

actual fun getPlatform(): Platform = Platform.Android

internal actual val isAndroid = true
internal actual val isDebug = BuildConfig.DEBUG
actual val defaultAsyncDispatcher: CoroutineDispatcher = Dispatchers.IO
