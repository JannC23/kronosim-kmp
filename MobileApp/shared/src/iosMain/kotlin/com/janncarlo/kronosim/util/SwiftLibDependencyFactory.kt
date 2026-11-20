package com.janncarlo.kronosim.util

import com.janncarlo.kronosim.data.source.featureflag.FeatureFlagManager
import com.janncarlo.kronosim.presentation.components.ads.AdsManager
import com.janncarlo.kronosim.presentation.components.ads.IosAdsDisplayer
import com.janncarlo.kronosim.util.analytics.Analytics

/**
This factory is used to help to use swift libraries in KMP. Actual implementations are provided in swift.
 */
interface SwiftLibDependencyFactory {
    fun provideFeatureFlagManagerImpl(): FeatureFlagManager
    fun provideFirebaseAnalyticsImpl(): Analytics
    fun provideAdsManagerImpl(): AdsManager
    fun provideIosAdsDisplayer(): IosAdsDisplayer
}
