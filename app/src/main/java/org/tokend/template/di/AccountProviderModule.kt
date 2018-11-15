package org.tokend.template.di

import dagger.Module
import dagger.Provides
import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.AccountProviderFactory
import javax.inject.Singleton

@Module
class AccountProviderModule {
    @Provides
    @Singleton
    fun accountProvider(): AccountProvider {
        return AccountProviderFactory().createAccountProvider()
    }
}