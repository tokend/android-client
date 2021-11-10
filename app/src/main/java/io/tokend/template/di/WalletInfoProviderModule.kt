package io.tokend.template.di

import dagger.Module
import dagger.Provides
import io.tokend.template.logic.providers.WalletInfoProvider
import io.tokend.template.logic.session.Session
import javax.inject.Singleton

@Module
class WalletInfoProviderModule {
    @Provides
    @Singleton
    fun walletInfoProvider(session: Session): WalletInfoProvider {
        return session
    }
}