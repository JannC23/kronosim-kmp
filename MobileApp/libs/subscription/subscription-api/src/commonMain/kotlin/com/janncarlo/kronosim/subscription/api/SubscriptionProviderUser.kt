package com.janncarlo.kronosim.subscription.api

class SubscriptionProviderUser(
    val grantedAccesses: Map<String, GrantedAccess>,
    val activeSubscriptionIds: Set<String>,
)
