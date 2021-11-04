package org.tokend.template.di.providers

import org.tokend.wallet.Account

interface AccountProvider {
    fun getAccount(): Account?

    fun getAccount(accountId: String): Account?

    fun getAccounts(): List<Account>

    fun setAccounts(accounts: List<Account>)
}