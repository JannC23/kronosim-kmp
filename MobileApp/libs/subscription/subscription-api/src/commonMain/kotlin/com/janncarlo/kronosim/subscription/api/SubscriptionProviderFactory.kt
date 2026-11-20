package com.janncarlo.kronosim.subscription.api

interface SubscriptionProviderFactory {
    companion object

    fun createProvider(): SubscriptionProvider

    fun createProviderUi(): SubscriptionProviderUi
}
