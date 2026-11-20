package com.janncarlo.kronosim.subscription.api

data class PurchaseError(
    val message: String,
    val code: String? = null,
)
