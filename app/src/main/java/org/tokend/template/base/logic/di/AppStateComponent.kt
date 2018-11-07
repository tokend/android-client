package org.tokend.template.base.logic.di

import dagger.Component
import org.tokend.template.base.activities.BaseActivity
import org.tokend.template.base.fragments.BaseFragment
import org.tokend.template.base.fragments.settings.SettingsFragment
import org.tokend.template.base.logic.di.providers.AppModule
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
    AppModule::class
])
interface AppStateComponent {
    fun inject(baseActivity: BaseActivity)
    fun inject(baseFragment: BaseFragment)
    fun inject(settingsFragment: SettingsFragment)
}