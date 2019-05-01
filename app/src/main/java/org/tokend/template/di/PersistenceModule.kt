package org.tokend.template.di

import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import org.tokend.template.features.kyc.storage.SubmittedKycStatePersistor
import org.tokend.template.logic.persistance.CredentialsPersistor
import org.tokend.template.logic.persistance.UrlConfigPersistor
import javax.inject.Singleton

@Module
class PersistenceModule(
        private val credentialsPreferences: SharedPreferences,
        private val networkPreferences: SharedPreferences,
        private val kycStatePreferences: SharedPreferences
) {
    @Provides
    @Singleton
    fun credentialsPresistor(): CredentialsPersistor {
        return CredentialsPersistor(credentialsPreferences)
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
}