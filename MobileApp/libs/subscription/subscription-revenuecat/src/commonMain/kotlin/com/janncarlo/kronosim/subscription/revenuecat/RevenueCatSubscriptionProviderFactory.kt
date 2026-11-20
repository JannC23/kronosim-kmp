package com.janncarlo.kronosim.subscription.revenuecat

import com.janncarlo.kronosim.subscription.api.SubscriptionProviderFactory

val SubscriptionProviderFactory.Companion.RevenueCat: SubscriptionProviderFactory
    get() = subscriptionProviderFactory

internal expect val subscriptionProviderFactory: SubscriptionProviderFactory
