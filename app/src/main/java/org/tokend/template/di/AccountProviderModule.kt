package org.tokend.template.di

import dagger.Module
import dagger.Provides
import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.logic.Session
import javax.inject.Singleton

@Module
class AccountProviderModule {
    @Provides
    @Singleton
    fun accountProvider(session: Session): AccountProvider {
        return session
    }
}