package org.tokend.template.base.logic.di

import dagger.Module
import dagger.Provides
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.base.logic.di.providers.RepositoryProvider
import org.tokend.template.base.logic.di.providers.RepositoryProviderImpl
import org.tokend.template.base.logic.di.providers.WalletInfoProvider
import javax.inject.Singleton

@Module
class RepositoriesModule {
    @Provides
    @Singleton
    fun repositoriesProvider(apiProvider: ApiProvider,
                            walletInfoProvider: WalletInfoProvider): RepositoryProvider {
        return RepositoryProviderImpl(apiProvider, walletInfoProvider)
    }
}