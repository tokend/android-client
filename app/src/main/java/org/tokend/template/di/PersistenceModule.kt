package org.tokend.template.di

import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import org.tokend.template.features.kyc.storage.SubmittedKycStatePersistor
import org.tokend.template.features.localaccount.storage.LocalAccountPersistor
import org.tokend.template.features.localaccount.storage.LocalAccountPersistorOnPreferences
import org.tokend.template.logic.persistence.UrlConfigPersistor
import org.tokend.template.logic.credentials.persistence.CredentialsPersistor
import org.tokend.template.logic.credentials.persistence.CredentialsPersistorOnPreferences
import javax.inject.Singleton

@Module
class PersistenceModule(
        private val credentialsPreferences: SharedPreferences,
        private val networkPreferences: SharedPreferences,
        private val kycStatePreferences: SharedPreferences,
        private val localAccountPreferences: SharedPreferences
) {
    @Provides
    @Singleton
    fun credentialsPresistor(): CredentialsPersistor {
        return CredentialsPersistorOnPreferences(credentialsPreferences)
    }

    @Provides
    @Singleton
    fun urlConfigPresistor(): UrlConfigPersistor {
        return UrlConfigPersistor(networkPreferences)
    }

    @Provides
    @Singleton
    fun kycStatePersistor(): SubmittedKycStatePersistor {
        return SubmittedKycStatePersistor(kycStatePreferences)
    }
    
    @Provides
    @Singleton
    fun localAccountPersistor(): LocalAccountPersistor {
        return LocalAccountPersistorOnPreferences(localAccountPreferences)
    }
}