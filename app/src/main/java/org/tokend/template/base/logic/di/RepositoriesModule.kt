package org.tokend.template.base.logic.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.tokend.template.base.logic.di.providers.*
import javax.inject.Singleton

@Module
class RepositoriesModule {
    @Provides
    @Singleton
    fun repositoriesProvider(apiProvider: ApiProvider,
                            walletInfoProvider: WalletInfoProvider,
                             context: Context): RepositoryProvider {
        return RepositoryProviderImpl(apiProvider, walletInfoProvider, context)
    }
}