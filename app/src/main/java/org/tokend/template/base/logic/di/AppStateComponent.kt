package org.tokend.template.base.logic.di

import dagger.Component
import org.tokend.template.base.activities.BaseActivity
import org.tokend.template.base.fragments.BaseFragment
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AccountProviderModule::class,
    WalletInfoProviderModule::class,
    AppTfaCallbackModule::class,
    ApiProviderModule::class,
    RepositoriesModule::class
])
interface AppStateComponent {
    fun inject(baseActivity: BaseActivity)
    fun inject(baseFragment: BaseFragment)
}