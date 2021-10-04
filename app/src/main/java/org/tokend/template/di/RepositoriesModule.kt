package org.tokend.template.di

import android.content.Context
import android.content.SharedPreferences
import com.fasterxml.jackson.databind.ObjectMapper
import dagger.Module
import dagger.Provides
import org.tokend.template.data.storage.persistence.ObjectPersistence
import org.tokend.template.db.AppDatabase
import org.tokend.template.di.providers.*
import org.tokend.template.features.kyc.storage.ActiveKycPersistence
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
            activeKycPersistence: ActiveKycPersistence,
            localAccountPersistence: ObjectPersistence<LocalAccount>,
            persistencePreferences: SharedPreferences,
            database: AppDatabase
    ): RepositoryProvider {
        return RepositoryProviderImpl(apiProvider, walletInfoProvider, urlConfigProvider,
                mapper, context, activeKycPersistence, localAccountPersistence,
                persistencePreferences, database)
    }
}