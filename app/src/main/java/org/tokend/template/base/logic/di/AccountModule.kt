package org.tokend.template.base.logic.di

import dagger.Module
import dagger.Provides
import org.tokend.wallet.Account

@Module
class AccountModule {
    companion object {
        var account: Account? = null
    }

    @Provides
    fun provideAccount(): Account? {
        return account
    }
}