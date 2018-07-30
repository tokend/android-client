package org.tokend.template.base.logic.di.providers

import org.tokend.wallet.Account

class AccountProviderFactory {
    fun createAccountProvider(): AccountProvider {
        return AccountProviderImpl()
    }

    fun createAccountProvider(account: Account?): AccountProvider {
        return createAccountProvider().apply { setAccount(account) }
    }
}