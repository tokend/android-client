package io.tokend.template.logic.providers

import org.tokend.wallet.Account

interface AccountProvider {
    fun getDefaultAccount(): Account

    fun getAccount(accountId: String): Account?

    fun getAccounts(): List<Account>

    fun setAccounts(accounts: List<Account>)
}