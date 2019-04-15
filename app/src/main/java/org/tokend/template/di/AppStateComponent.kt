package org.tokend.template.di

import dagger.Component
import org.tokend.template.activities.BaseActivity
import org.tokend.template.di.providers.AppModule
import org.tokend.template.di.providers.SessionModule
import org.tokend.template.features.settings.SettingsFragment
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.view.assetchart.AssetChartCard
import org.tokend.template.view.balancepicker.BalancePickerBottomDialogFragment
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AccountProviderModule::class,
    WalletInfoProviderModule::class,
    AppTfaCallbackModule::class,
    ApiProviderModule::class,
    RepositoriesModule::class,
    PersistenceModule::class,
    UrlConfigProviderModule::class,
    UtilModule::class,
    AppModule::class,
    SessionModule::class
])
interface AppStateComponent {
    fun inject(baseActivity: BaseActivity)
    fun inject(baseFragment: BaseFragment)
    fun inject(settingsFragment: SettingsFragment)
    fun inject(assetChartCard: AssetChartCard)
    fun inject(balancePicker: BalancePickerBottomDialogFragment)
}