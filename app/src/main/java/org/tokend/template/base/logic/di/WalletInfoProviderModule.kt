package org.tokend.template.base.logic.di

import dagger.Module
import dagger.Provides
import org.tokend.template.base.logic.di.providers.WalletInfoProviderImpl
import org.tokend.template.base.logic.di.providers.WalletInfoProvider
import javax.inject.Singleton

@Module
class WalletInfoProviderModule {
    @Provides
    @Singleton
    fun walletInfoProvider(): WalletInfoProvider {
        return WalletInfoProviderImpl()
    }
}