package org.tokend.template.di

import android.content.Context
import android.content.SharedPreferences
import com.fasterxml.jackson.databind.ObjectMapper
import dagger.Module
import dagger.Provides
import org.tokend.template.data.repository.base.ObjectPersistence
import org.tokend.template.db.AppDatabase
import org.tokend.template.di.providers.*
import org.tokend.template.features.kyc.storage.SubmittedKycStatePersistence
import org.tokend.template.features.localaccount.model.LocalAccount
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
            kycStatePersistence: SubmittedKycStatePersistence,
            localAccountPersistence: ObjectPersistence<LocalAccount>,
            persistencePreferences: SharedPreferences,
            database: AppDatabase
    ): RepositoryProvider {
        return RepositoryProviderImpl(apiProvider, walletInfoProvider, urlConfigProvider,
                mapper, context, kycStatePersistence, localAccountPersistence,
                persistencePreferences, database)
    }
}