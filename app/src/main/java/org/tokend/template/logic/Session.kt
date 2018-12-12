package org.tokend.template.logic

import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.WalletInfoProvider

/**
 * Holds session data
 */
class Session(
        walletInfoProvider: WalletInfoProvider,
        accountProvider: AccountProvider
) : WalletInfoProvider by walletInfoProvider, AccountProvider by accountProvider {

    /**
     * @returns true if session is expired and so sign out is required
     */
    var isExpired = false

    /**
     * @returns true if session was started with TokenD Authenticator auth
     */
    var isAuthenticatorUsed = false

    /**
     * Resets the session to the initial state, clears data
     */
    fun reset() {
        isExpired = false
        isAuthenticatorUsed = false

        setWalletInfo(null)
        setAccount(null)
    }
}