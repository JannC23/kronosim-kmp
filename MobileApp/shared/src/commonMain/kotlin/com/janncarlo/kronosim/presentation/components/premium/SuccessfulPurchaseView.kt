package com.janncarlo.kronosim.presentation.components.premium

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.janncarlo.kronosim.designsystem.components.premium.PremiumFeatureUiState
import com.janncarlo.kronosim.designsystem.components.premium.SuccessfulPurchaseContent
import com.janncarlo.kronosim.util.Constants.subscriptionUrl
import com.janncarlo.kronosim.util.inappreview.rememberInAppReviewTrigger
import com.janncarlo.kronosim.util.logging.AppLogger
import com.janncarlo.kronosim.util.logging.logSuccessfulPurchase

@Composable
fun SuccessfulPurchaseView(
    features: List<PremiumFeatureUiState> = emptyList(),
    expirationDate: String? = null,
    isLifetime: Boolean = false,
    isRecurring: Boolean = true,
    modifier: Modifier = Modifier,
    onContinue: () -> Unit = {},
) {
    val inAppReviewTrigger = rememberInAppReviewTrigger()
    LaunchedEffect(Unit) {
        AppLogger.logSuccessfulPurchase()
        inAppReviewTrigger.triggerAfterSuccessfulPurchase()
    }

    SuccessfulPurchaseContent(
        subscriptionUrl = subscriptionUrl,
        features = features,
        expirationDate = expirationDate,
        isLifetime = isLifetime,
        isRecurring = isRecurring,
        modifier = modifier,
        onContinue = onContinue,
    )
}
