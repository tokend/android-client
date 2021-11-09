package io.tokend.template.logic.session

import io.tokend.template.di.providers.AccountProvider
import io.tokend.template.di.providers.WalletInfoProvider
import io.tokend.template.features.localaccount.model.LocalAccount
import io.tokend.template.features.signin.logic.SignInMethod

/**
 * Holds session data
 */
class Session(
    walletInfoProvider: WalletInfoProvider,
    accountProvider: AccountProvider,
    private val sessionInfoStorage: SessionInfoStorage? = null
) : WalletInfoProvider by walletInfoProvider, AccountProvider by accountProvider {
    val login: String
        get() = getWalletInfo()?.login ?: ""

    /**
     * @returns true if session is expired and so sign out is required
     */
    var isExpired = false

    /**
     * @returns [SignInMethod] used to start this session
     */
    var signInMethod: SignInMethod? = null
        set(value) {
            field = value
            if (value != null) {
                sessionInfoStorage?.saveLastSignInMethod(value)
            }
        }

    /**
     * @returns last used [SignInMethod]
     */
    val lastSignInMethod: SignInMethod?
        get() = sessionInfoStorage?.loadLastSignInMethod()

    /**
     * @returns true if session was started with [LocalAccount] sign in
     */
    val isLocalAccountUsed
        get() = signInMethod == SignInMethod.LOCAL_ACCOUNT

    /**
     * Resets the session to the initial state, clears data
     */
    fun reset() {
        isExpired = false
        signInMethod = null

        setWalletInfo(null)
        setAccounts(emptyList())
    }
}