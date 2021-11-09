package io.tokend.template.di

import dagger.Module
import dagger.Provides
import io.tokend.template.di.providers.WalletInfoProvider
import io.tokend.template.logic.Session
import javax.inject.Singleton

@Module
class WalletInfoProviderModule {
    @Provides
    @Singleton
    fun walletInfoProvider(session: Session): WalletInfoProvider {
        return session
    }
}