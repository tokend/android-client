package org.tokend.template.base.logic.di

import dagger.Component
import org.tokend.template.base.activities.BaseActivity
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AccountModule::class,
    WalletInfoModule::class,
    AppTfaCallbackModule::class
])
interface AppStateComponent {
    fun inject(baseActivity: BaseActivity)
}