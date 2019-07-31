package org.tokend.template.features.signup.logic

import io.reactivex.Single
import io.reactivex.functions.BiFunction
import org.tokend.rx.extensions.randomSingle
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.wallets.model.EmailAlreadyTakenException
import org.tokend.sdk.api.wallets.model.InvalidCredentialsException
import org.tokend.sdk.keyserver.KeyServer
import org.tokend.sdk.keyserver.models.WalletCreateResult
import org.tokend.wallet.Account

/**
 * Creates wallet with given credentials and submits it
 */
class SignUpUseCase(
        private val email: String,
        private val password: CharArray,
        private val keyServer: KeyServer
) {
    private lateinit var rootAccount: Account
    private lateinit var recoveryAccount: Account

    fun perform(): Single<WalletCreateResult> {
        return ensureEmailIsFree()
                .flatMap {
                    getAccounts()
                }
                .doOnSuccess { (rootAccount, recoveryAccount) ->
                    this.rootAccount = rootAccount
                    this.recoveryAccount = recoveryAccount
                }
                .flatMap {
                    createAndSaveWallet()
                }
    }

    private fun ensureEmailIsFree(): Single<Boolean> {
        return keyServer
                .getLoginParams(email)
                .toSingle()
                .map { false }
                .onErrorResumeNext { error ->
                    if (error is InvalidCredentialsException)
                        Single.just(true)
                    else
                        Single.error(error)
                }
                .flatMap { isFree ->
                    if (!isFree)
                        Single.error(EmailAlreadyTakenException())
                    else
                        Single.just(isFree)
                }
    }

    private fun getAccounts(): Single<Pair<Account, Account>> {
        return Single.zip(
                generateNewAccount(),
                generateNewAccount(),
                BiFunction { x: Account, y: Account -> x to y }
        )
    }

    private fun generateNewAccount(): Single<Account> {
        return Account.randomSingle()
    }

    private fun createAndSaveWallet(): Single<WalletCreateResult> {
        return keyServer.createAndSaveWallet(
                email,
                password,
                rootAccount,
                recoveryAccount
        ).toSingle()
    }
}