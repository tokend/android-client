package org.tokend.template.base.logic.di

import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import org.tokend.template.base.logic.persistance.CredentialsPersistor
import javax.inject.Singleton

@Module
class PersistenceModule(
        private val credentialsPreferences: SharedPreferences
) {
    @Provides
    @Singleton
    fun credentialsPresistor(): CredentialsPersistor {
        return CredentialsPersistor(credentialsPreferences)
    }
}