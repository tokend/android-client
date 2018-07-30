package org.tokend.template.base.logic.di.providers

import org.tokend.wallet.Account

interface AccountProvider {
    fun getAccount(): Account?
    fun setAccount(account: Account?)
}