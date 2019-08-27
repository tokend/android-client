package org.tokend.template.features.signup.logic

import io.reactivex.Single
import org.tokend.rx.extensions.randomSingle
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.wallets.model.EmailAlreadyTakenException
import org.tokend.sdk.api.wallets.model.InvalidCredentialsException
import org.tokend.sdk.keyserver.KeyServer
import org.tokend.sdk.keyserver.models.WalletCreateResult
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.template.logic.Session
import org.tokend.template.logic.persistance.CredentialsPersistor
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

    fun perform(): Single<WalletCreateResult> {
        return ensureEmailIsFree()
                .flatMap {
                    getAccount()
                }
                .doOnSuccess { rootAccount ->
                    this.rootAccount = rootAccount
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

    private fun getAccount(): Single<Account> {
        return Account.randomSingle()
    }

    private fun createAndSaveWallet(): Single<WalletCreateResult> {
        return keyServer.createAndSaveWallet(
                email,
                password,
                rootAccount
        ).toSingle()
    }
}