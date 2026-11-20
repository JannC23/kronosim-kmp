package com.janncarlo.kronosim.subscription.revenuecat

import com.janncarlo.kronosim.subscription.api.SubscriptionProvider
import com.janncarlo.kronosim.subscription.api.SubscriptionProviderFactory
import com.janncarlo.kronosim.subscription.api.SubscriptionProviderUi

internal actual val subscriptionProviderFactory: SubscriptionProviderFactory
    get() =
        object : SubscriptionProviderFactory {
            override fun createProvider(): SubscriptionProvider = RevenueCatSubscriptionProvider()

            override fun createProviderUi(): SubscriptionProviderUi = RevenueCatSubscriptionProviderUi()
        }
