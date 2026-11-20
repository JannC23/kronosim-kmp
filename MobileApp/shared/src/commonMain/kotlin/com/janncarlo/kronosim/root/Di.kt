package com.janncarlo.kronosim.root

import com.janncarlo.kronosim.common.BuildConfig
import com.janncarlo.kronosim.data.BackgroundExecutor
import com.janncarlo.kronosim.data.repository.CreditRepository
import com.janncarlo.kronosim.data.repository.GenerationRepository
import com.janncarlo.kronosim.data.repository.SubscriptionRepository
import com.janncarlo.kronosim.data.repository.UserRepository
import com.janncarlo.kronosim.data.source.ai.ReplicateGenerationProvider
import com.janncarlo.kronosim.data.source.preferences.PreferencesDataStoreProvider
import com.janncarlo.kronosim.data.source.preferences.UserPreferences
import com.janncarlo.kronosim.data.source.preferences.UserPreferencesImpl
import com.janncarlo.kronosim.data.source.remote.HttpClientFactory
import com.janncarlo.kronosim.data.source.remote.apiservices.ApiService
import com.janncarlo.kronosim.data.source.remote.apiservices.TemporaryFileUploadApiService
import com.janncarlo.kronosim.data.source.remote.apiservices.ai.AiTransport
import com.janncarlo.kronosim.data.source.remote.apiservices.ai.OpenAiApiService
import com.janncarlo.kronosim.data.source.remote.apiservices.ai.ReplicateApiService
import com.janncarlo.kronosim.domain.engine.ConsensusEngine
import com.janncarlo.kronosim.domain.model.credit.creditSystemConfig
import com.janncarlo.kronosim.domain.usecase.AiGenerationProvider
import com.janncarlo.kronosim.presentation.screens.account.AccountViewModel
import com.janncarlo.kronosim.presentation.screens.creditbalance.CreditBalanceViewModel
import com.janncarlo.kronosim.presentation.screens.gallery.GalleryViewModel
import com.janncarlo.kronosim.presentation.screens.generationresult.GenerationResultViewModel
import com.janncarlo.kronosim.presentation.screens.home.HomeViewModel
import com.janncarlo.kronosim.presentation.screens.onboarding.OnBoardingViewModel
import com.janncarlo.kronosim.presentation.screens.paywall.PaywallViewModel
import com.janncarlo.kronosim.presentation.screens.profile.ProfileViewModel
import com.janncarlo.kronosim.presentation.screens.simulator.SimulatorViewModel
import com.janncarlo.kronosim.presentation.screens.subscriptions.SubscriptionsViewModel
import com.janncarlo.kronosim.subscription.api.MockSubscriptionProvider
import com.janncarlo.kronosim.subscription.api.NoOpSubscriptionProviderUi
import com.janncarlo.kronosim.subscription.api.SubscriptionProvider
import com.janncarlo.kronosim.subscription.api.SubscriptionProviderFactory
import com.janncarlo.kronosim.subscription.api.SubscriptionProviderUi
import com.janncarlo.kronosim.util.ApplicationScope
import com.janncarlo.kronosim.util.Constants
import com.janncarlo.kronosim.util.defaultAsyncDispatcher
import com.janncarlo.kronosim.util.extensions.nowEpochMillis
import com.janncarlo.kronosim.util.isAndroid
import com.janncarlo.kronosim.util.logging.Logger
import com.janncarlo.kronosim.util.logging.NapierLogger
import com.janncarlo.kronosim.util.logging.TelegramLogger
import com.janncarlo.kronosim.util.platformModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

/**
 - Koin module graph for the shared app. [AppInitializer] loads [appModules] at startup.                      *
 - Layered by concern: [domainModule] (pure, empty), [dataModule] (infra + repositories),
 - [presentationModule] (ViewModels), plus the per-target expect/actual `platformModule`.
 */

// Empty by design — the domain layer is pure (models/exceptions), nothing to inject.
private val domainModule = module {
}

// Infrastructure + repositories: scopes/dispatchers, preferences, network, auth & subscription
// providers (selected via Constants), repositories, loggers, AI provider, and the credit system.
private val dataModule = module {
    singleOf(::ApplicationScope)
    single<Random> { Random.Default }
    singleOf(::ConsensusEngine)
    factory { defaultAsyncDispatcher } bind CoroutineContext::class
    factory { BackgroundExecutor.IO } bind BackgroundExecutor::class

    // Preferences Source. The DataStore instance stays out of the Koin graph on
    // purpose — generic types erase to `DataStore`, so a second DataStore<T>
    // registered later would silently collide with this one.
    single { UserPreferencesImpl(get<PreferencesDataStoreProvider>().providePreferencesDataStore()) } bind UserPreferences::class

    // Remote source
    single { HttpClientFactory.default() }
    single(named("aiDirectClient")) { HttpClientFactory.noAuth() }
    single { AiTransport(proxyClient = get(), directClient = get(named("aiDirectClient"))) }
    single { TemporaryFileUploadApiService(HttpClientFactory.fileUpload()) }

    factoryOf(::ApiService)
    factoryOf(::OpenAiApiService)
    factoryOf(::ReplicateApiService)

    // Subscription Provider. When no real SDK key is set (isSubscriptionMockActive), swap in the
    // MockSubscriptionProvider so the paywall/purchase/unlock flow is explorable with zero keys.
    // Auto-reverts to the real provider (Adapty/RevenueCat) the moment a key is configured.
    factory { AppConfiguration.subscriptionProviderFactory } bind SubscriptionProviderFactory::class
    single {
        if (isSubscriptionMockActive()) {
            val userPreferences = get<UserPreferences>()
            MockSubscriptionProvider(
                readPremiumPurchased = {
                    userPreferences.getBoolean(MockSubscriptionProvider.KEY_MOCK_PREMIUM_PURCHASED, false)
                },
                writePremiumPurchased = {
                    userPreferences.putBoolean(MockSubscriptionProvider.KEY_MOCK_PREMIUM_PURCHASED, it)
                },
                premiumAccessId = Constants.PAYWALL_PREMIUM_ACCESS,
                creditPackPrefix = Constants.CREDIT_PACK_PRODUCT_ID_PREFIX,
                creditPackPlacementId = Constants.PAYWALL_PLACEMENT_CREDITS_PACK,
                currentTimeMillis = ::nowEpochMillis,
            )
        } else {
            get<SubscriptionProviderFactory>().createProvider()
        }
    } bind SubscriptionProvider::class
    factory {
        if (isSubscriptionMockActive()) {
            NoOpSubscriptionProviderUi
        } else {
            get<SubscriptionProviderFactory>().createProviderUi()
        }
    } bind SubscriptionProviderUi::class

    // Repositories
    single { UserRepository(get(), get(), get(), get()) }
    single { SubscriptionRepository(get(), get(), get(), get()) }
    single { GenerationRepository(get(), get(), get(), get(), get(), get(), get()) }

    // Loggers
    factory { TelegramLogger(get(), get(), get()) } bind Logger::class
    factory { NapierLogger() } bind Logger::class

    //    factory<AiGenerationProvider> { OpenAiImageGenerationProvider(get(), get()) }
    factory<AiGenerationProvider> { ReplicateGenerationProvider(get(), get()) }

    initializeCreditSystem()
}

// ViewModels, scoped per NavEntry. PaywallViewModel takes a placementId param.
private val presentationModule = module {
    viewModelOf(::OnBoardingViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::SimulatorViewModel)
    viewModelOf(::GalleryViewModel)
    viewModelOf(::ProfileViewModel)
    viewModel { (placementId: String?) ->
        PaywallViewModel(
            placementId = placementId,
            subscriptionRepository = get(),
                         creditRepository = get(),
                         userRepository = get(),
                         featureFlagManager = get(),
        )
    }
    viewModelOf(::AccountViewModel)
    viewModelOf(::SubscriptionsViewModel)
    viewModelOf(::GenerationResultViewModel)
    viewModelOf(::CreditBalanceViewModel)

    // Add new view models below — generate_screen.sh inserts here.
}

private fun Module.initializeCreditSystem() {
    single {
        val subscriptionRepository = get<SubscriptionRepository>()
        val appCreditSystemConfig = creditSystemConfig {
            oneTimeBonus("welcome_bonus_credit", 1)
            //            val userPreferences = get<UserPreferences>()
            //            oneTimeBonus(
            //                id = "referral_bonus",
            //                amount = 1,
            //                condition = {
            //                    userPreferences.getBoolean(UserPreferences.KEY_REFERRAL_COMPLETED)
            //                }
            //            )
            //            recurringWeekly(
            //                id = "free_plan_weekly",
            //                amount = 2,
            //                condition = {
            //                    !subscriptionRepository.hasPremiumAccess()
            //                }
            //            )

            recurringWeekly(
                id = "premium_plan_weekly", // Gives all premium users 10 credits per week
                amount = 10,
                condition = {
                    subscriptionRepository.hasPremiumAccess()
                },
            )
        }

        CreditRepository(appCreditSystemConfig, get(), get(), get(), get())
    }
}

/**
 - True when the active-platform subscription SDK key is still a placeholder, i.e. no real                    *
 - Adapty/RevenueCat account is wired yet. While true the app runs [MockSubscriptionProvider] so the
 - whole paywall → purchase → unlock flow is explorable with zero keys. Auto-off once a real key is set.
 */
private fun isSubscriptionMockActive(): Boolean {
    val key =
    if (isAndroid) {
        BuildConfig.SUBSCRIPTION_PROVIDER_ANDROID_API_KEY
    } else {
        BuildConfig.SUBSCRIPTION_PROVIDER_IOS_API_KEY
    }
    return key.isBlank() || key == MockSubscriptionProvider.PLACEHOLDER_KEY
}

// All Koin modules loaded at startup. platformModule is the expect/actual per-target module.
internal val appModules: List<Module> get() = platformModule + domainModule + dataModule + presentationModule
