package com.janncarlo.kronosim.subscription.adapty

import com.janncarlo.kronosim.subscription.api.SubscriptionProvider
import com.janncarlo.kronosim.subscription.api.SubscriptionProviderFactory
import com.janncarlo.kronosim.subscription.api.SubscriptionProviderUi

internal actual val subscriptionProviderFactory: SubscriptionProviderFactory
    get() =
        object : SubscriptionProviderFactory {
            override fun createProvider(): SubscriptionProvider = AdaptySubscriptionProvider()

            override fun createProviderUi(): SubscriptionProviderUi = AdaptySubscriptionProviderUi()
        }
