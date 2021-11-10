package io.tokend.template.logic.providers

import org.tokend.wallet.Account

class AccountProviderFactory {
    fun createAccountProvider(): AccountProvider {
        return AccountProviderImpl()
    }

    fun createAccountProvider(accounts: List<Account>): AccountProvider {
        return createAccountProvider().apply { setAccounts(accounts) }
    }
}