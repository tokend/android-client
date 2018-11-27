package org.tokend.template.logic

import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.WalletInfoProvider

class Session(
        walletInfoProvider: WalletInfoProvider,
        accountProvider: AccountProvider
) : WalletInfoProvider by walletInfoProvider, AccountProvider by accountProvider {

    var isExpired = false
    var isAuthenticatorUsed = false

    fun reset() {
        isExpired = false
        isAuthenticatorUsed = false

        setWalletInfo(null)
        setAccount(null)
    }
}