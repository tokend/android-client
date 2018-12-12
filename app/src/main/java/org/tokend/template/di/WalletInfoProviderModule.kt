package org.tokend.template.di

import dagger.Module
import dagger.Provides
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.logic.Session
import javax.inject.Singleton

@Module
class WalletInfoProviderModule {
    @Provides
    @Singleton
    fun walletInfoProvider(session: Session): WalletInfoProvider {
        return session
    }
}