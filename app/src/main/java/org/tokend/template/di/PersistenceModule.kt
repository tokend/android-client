package org.tokend.template.di

import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import org.tokend.template.data.storage.persistence.ObjectPersistence
import org.tokend.template.features.kyc.storage.ActiveKycPersistence
import org.tokend.template.features.localaccount.model.LocalAccount
import org.tokend.template.features.localaccount.storage.LocalAccountPersistenceOnPrefs
import org.tokend.template.features.urlconfig.model.UrlConfig
import org.tokend.template.features.urlconfig.storage.UrlConfigPersistence
import org.tokend.template.logic.credentials.persistence.CredentialsPersistence
import org.tokend.template.logic.credentials.persistence.CredentialsPersistenceImpl
import org.tokend.template.logic.credentials.persistence.WalletInfoPersistence
import org.tokend.template.logic.credentials.persistence.WalletInfoPersistenceImpl
import javax.inject.Singleton

@Module
class PersistenceModule(
        private val persistencePreferences: SharedPreferences,
        private val localAccountPreferences: SharedPreferences,
        private val networkPreferences: SharedPreferences
) {
    @Provides
    @Singleton
    fun credentialsPresistor(): CredentialsPersistence {
        return CredentialsPersistenceImpl(persistencePreferences)
    }
    @Provides
    @Singleton
    fun walletInfoPresistor(): WalletInfoPersistence {
        return WalletInfoPersistenceImpl(persistencePreferences)
    }

    @Provides
    @Singleton
    fun urlConfigPersistence(): ObjectPersistence<UrlConfig> {
        return UrlConfigPersistence(networkPreferences)
    }

    @Provides
    @Singleton
    fun activeKycPersistence(): ActiveKycPersistence {
        return ActiveKycPersistence(persistencePreferences)
    }

    @Provides
    @Singleton
    fun persistencePreferences(): SharedPreferences {
        return persistencePreferences
    }

    @Provides
    @Singleton
    fun localAccountPersistence(): ObjectPersistence<LocalAccount> {
        return LocalAccountPersistenceOnPrefs(localAccountPreferences)
    }
}