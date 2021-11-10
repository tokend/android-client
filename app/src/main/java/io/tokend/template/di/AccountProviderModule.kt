package io.tokend.template.di

import dagger.Module
import dagger.Provides
import io.tokend.template.logic.providers.AccountProvider
import io.tokend.template.logic.session.Session
import javax.inject.Singleton

@Module
class AccountProviderModule {
    @Provides
    @Singleton
    fun accountProvider(session: Session): AccountProvider {
        return session
    }
}