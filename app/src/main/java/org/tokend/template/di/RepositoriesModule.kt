package org.tokend.template.di

import android.content.Context
import com.fasterxml.jackson.databind.ObjectMapper
import dagger.Module
import dagger.Provides
import org.tokend.template.di.providers.*
import org.tokend.template.features.kyc.storage.SubmittedKycStatePersistor
import org.tokend.template.features.localaccount.storage.LocalAccountPersistor
import javax.inject.Singleton

@Module
class RepositoriesModule {
    @Provides
    @Singleton
    fun repositoriesProvider(
            apiProvider: ApiProvider,
            walletInfoProvider: WalletInfoProvider,
            urlConfigProvider: UrlConfigProvider,
            mapper: ObjectMapper,
            context: Context,
            kycStatePersistor: SubmittedKycStatePersistor,
            localAccountPersistor: LocalAccountPersistor
    ): RepositoryProvider {
        return RepositoryProviderImpl(apiProvider, walletInfoProvider, urlConfigProvider,
                mapper, context, kycStatePersistor, localAccountPersistor)
    }
}