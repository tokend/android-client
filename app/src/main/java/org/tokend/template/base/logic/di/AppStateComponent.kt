package org.tokend.template.base.logic.di

import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AccountModule::class,
    WalletInfoModule::class
])
interface AppStateComponent {

}