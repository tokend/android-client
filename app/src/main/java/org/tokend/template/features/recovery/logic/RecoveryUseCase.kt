package org.tokend.template.features.recovery.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import org.tokend.rx.extensions.fromSecretSeedSingle
import org.tokend.rx.extensions.getWalletInfoSingle
import org.tokend.rx.extensions.randomSingle
import org.tokend.sdk.keyserver.KeyServer
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.template.di.providers.*
import org.tokend.template.logic.wallet.WalletUpdateManager
import org.tokend.wallet.Account

/**
 * Recovers user's password
 *
 * @param urlConfigProvider required to create signed API instance
 */
class RecoveryUseCase(
        private val email: String,
        private val recoverySeed: CharArray,
        private val newPassword: CharArray,
        private val walletUpdateManager: WalletUpdateManager,
        private val urlConfigProvider: UrlConfigProvider
) {
    private lateinit var recoveryAccount: Account
    private lateinit var newAccount: Account
    private lateinit var recoveryWallet: WalletInfo

    private lateinit var accountProvider: AccountProvider
    private lateinit var apiProvider: ApiProvider
    private lateinit var signedKeyServer: KeyServer
    private lateinit var walletInfoProvider: WalletInfoProvider

    fun perform(): Completable {
        return getAccounts()
                .doOnSuccess { (recoveryAccount, newAccount) ->
                    this.recoveryAccount = recoveryAccount
                    this.newAccount = newAccount

                    this.accountProvider =
                            AccountProviderFactory().createAccountProvider(recoveryAccount)
                    this.apiProvider =
                            ApiProviderFactory().createApiProvider(urlConfigProvider, accountProvider)
                    this.signedKeyServer = apiProvider.getSignedKeyServer()!!
                }
                .flatMap {
                    getRecoveryWallet()
                }
                .doOnSuccess { recoveryWallet ->
                    this.recoveryWallet = recoveryWallet

                    this.walletInfoProvider =
                            WalletInfoProviderFactory().createWalletInfoProvider(recoveryWallet)
                }
                .flatMap {
                    updateWallet()
                }
                .ignoreElement()
    }

    private fun getAccounts(): Single<Pair<Account, Account>> {
        return Single.zip(
                createAccountFromSeed(),
                generateNewAccount(),
                BiFunction { x: Account, y: Account -> x to y }
        )
    }

    private fun createAccountFromSeed(): Single<Account> {
        return Account.fromSecretSeedSingle(recoverySeed)
    }

    private fun generateNewAccount(): Single<Account> {
        return Account.randomSingle()
    }

    private fun getRecoveryWallet(): Single<WalletInfo> {
        return signedKeyServer.getWalletInfoSingle(email, recoverySeed, true)
    }

    private fun updateWallet(): Single<Boolean> {
        return walletUpdateManager.updateWalletWithNewPassword(
                apiProvider,
                accountProvider,
                walletInfoProvider,
                newAccount,
                newPassword
        )
                .toSingleDefault(true)
    }
}