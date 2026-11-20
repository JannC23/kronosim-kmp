package com.janncarlo.kronosim.presentation.components.ads

interface AdsManager {
    fun initialize()
    val interstitialAdLoader: FullScreenAdLoader
    val rewardedAdLoader: FullScreenAdLoader
}
