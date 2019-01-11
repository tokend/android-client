package org.tokend.template.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.tokend.template.di.providers.*
import javax.inject.Singleton

@Module
class RepositoriesModule {
    @Provides
    @Singleton
    fun repositoriesProvider(apiProvider: ApiProvider,
                             walletInfoProvider: WalletInfoProvider,
                             urlConfigProvider: UrlConfigProvider,
                             context: Context): RepositoryProvider {
        return RepositoryProviderImpl(apiProvider, walletInfoProvider, urlConfigProvider, context)
    }
}