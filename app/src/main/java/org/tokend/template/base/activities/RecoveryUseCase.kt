package org.tokend.template.base.activities

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.template.base.logic.WalletManager
import org.tokend.template.base.logic.WalletPasswordManager
import org.tokend.template.base.logic.di.providers.*
import org.tokend.wallet.Account

class RecoveryUseCase(
        private val email: String,
        private val recoverySeed: CharArray,
        private val newPassword: CharArray,
        private val walletPasswordManager: WalletPasswordManager,
        private val urlConfigProvider: UrlConfigProvider
) {
    private lateinit var recoveryAccount: Account
    private lateinit var newAccount: Account
    private lateinit var recoveryWallet: WalletInfo

    private lateinit var accountProvider: AccountProvider
    private lateinit var apiProvider: ApiProvider
    private lateinit var walletManager: WalletManager
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
                    this.walletManager =
                            WalletManager(apiProvider.getSignedKeyStorage()!!)
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
                .toCompletable()
    }

    private fun getAccounts(): Single<Pair<Account, Account>> {
        return Single.zip(
                createAccountFromSeed(),
                generateNewAccount(),
                BiFunction { x: Account, y: Account -> x to y }
        )
    }

    private fun createAccountFromSeed(): Single<Account> {
        return {
            Account.fromSecretSeed(recoverySeed)
        }.toSingle().subscribeOn(Schedulers.newThread())
    }

    private fun generateNewAccount(): Single<Account> {
        return {
            Account.random()
        }.toSingle().subscribeOn(Schedulers.newThread())
    }

    private fun getRecoveryWallet(): Single<WalletInfo> {
        return walletManager
                .getWalletInfo(email, recoverySeed, true)
    }

    private fun updateWallet(): Single<Boolean> {
        return walletPasswordManager.updateWalletWithNewPassword(
                apiProvider,
                accountProvider,
                walletInfoProvider,
                null,
                newAccount,
                newPassword
        )
                .toSingleDefault(true)
    }
}