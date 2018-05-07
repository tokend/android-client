package org.tokend.template.base.logic.di.providers

import org.tokend.wallet.Account

class AccountProviderImpl : AccountProvider {
    private var account: Account? = null

    override fun setAccount(account: Account?) {
        this.account = account
    }

    override fun getAccount(): Account? {
        return account
    }
}