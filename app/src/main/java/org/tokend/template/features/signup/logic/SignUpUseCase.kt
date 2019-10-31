package org.tokend.template.features.signup.logic

import io.reactivex.Single
import org.tokend.rx.extensions.randomSingle
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.wallets.model.EmailAlreadyTakenException
import org.tokend.sdk.api.wallets.model.InvalidCredentialsException
import org.tokend.sdk.keyserver.KeyServer
import org.tokend.sdk.keyserver.models.WalletCreateResult
import org.tokend.template.di.providers.ApiProvider
import org.tokend.wallet.Account

/**
 * Creates wallet with given credentials and submits it
 */
class SignUpUseCase(
        private val email: String,
        private val password: CharArray,
        private val apiProvider: ApiProvider
) {
    private val keyServer = KeyServer(apiProvider.getApi().wallets)
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
                apiProvider.getApi().v3.keyValue,
                rootAccount
        ).toSingle()
    }
}