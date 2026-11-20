package com.janncarlo.kronosim.subscription.adapty

import com.janncarlo.kronosim.subscription.api.SubscriptionProviderFactory

val SubscriptionProviderFactory.Companion.Adapty: SubscriptionProviderFactory
    get() = subscriptionProviderFactory

internal expect val subscriptionProviderFactory: SubscriptionProviderFactory
