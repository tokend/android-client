package io.tokend.template.di

import dagger.Component
import io.tokend.template.activities.BaseActivity
import io.tokend.template.features.dashboard.balances.view.AssetDistributionChart
import io.tokend.template.features.settings.SettingsFragment
import io.tokend.template.fragments.BaseFragment
import io.tokend.template.view.assetchart.AssetChartCard
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AccountProviderModule::class,
        WalletInfoProviderModule::class,
        AppTfaCallbackModule::class,
        ApiProviderModule::class,
        RepositoriesModule::class,
        PersistenceModule::class,
        UrlConfigModule::class,
        UtilModule::class,
        AppModule::class,
        SessionModule::class,
        LocaleManagerModule::class,
        AppDatabaseModule::class
    ]
)
interface AppStateComponent {
    fun inject(baseActivity: BaseActivity)
    fun inject(baseFragment: BaseFragment)
    fun inject(settingsFragment: SettingsFragment)
    fun inject(assetChartCard: AssetChartCard)
    fun inject(assetDistributionChart: AssetDistributionChart)
}