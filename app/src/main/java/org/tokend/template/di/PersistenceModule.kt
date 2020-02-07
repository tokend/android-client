package org.tokend.template.di

import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import org.tokend.template.data.repository.base.ObjectPersistence
import org.tokend.template.features.kyc.model.ActiveKyc
import org.tokend.template.features.kyc.storage.ActiveKycPersistence
import org.tokend.template.features.localaccount.model.LocalAccount
import org.tokend.template.features.localaccount.storage.LocalAccountPersistenceOnPrefs
import org.tokend.template.logic.credentials.persistence.CredentialsPersistence
import org.tokend.template.logic.credentials.persistence.CredentialsPersistenceOnPreferences
import org.tokend.template.logic.persistence.UrlConfigPersistor
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
        return CredentialsPersistenceOnPreferences(persistencePreferences)
    }

    @Provides
    @Singleton
    fun urlConfigPresistor(): UrlConfigPersistor {
        return UrlConfigPersistor(networkPreferences)
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