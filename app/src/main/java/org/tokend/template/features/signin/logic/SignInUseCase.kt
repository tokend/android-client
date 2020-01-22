package org.tokend.template.features.signin.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.rx.extensions.fromSecretSeedSingle
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.keyserver.KeyServer
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.template.logic.Session
import org.tokend.template.logic.credentials.persistence.CredentialsPersistence
import org.tokend.wallet.Account

/**
 * Performs sign in with given credentials:
 * performs keypair loading and decryption,
 * sets up [WalletInfoProvider] and [AccountProvider], updates [CredentialsPersistence] if it is set.
 * If [CredentialsPersistence] contains saved credentials no network calls will be performed.
 *
 * @see PostSignInManager
 */
class SignInUseCase(
        private val email: String,
        private val password: CharArray,
        private val keyServer: KeyServer,
        private val session: Session,
        private val credentialsPersistence: CredentialsPersistence?,
        private val postSignInActions: (() -> Completable)?
) {
    private lateinit var walletInfo: WalletInfo
    private lateinit var account: Account

    fun perform(): Completable {
        val scheduler = Schedulers.newThread()

        return getWalletInfo(email, password)
                .doOnSuccess { walletInfo ->
                    this.walletInfo = walletInfo
                }
                .observeOn(scheduler)
                .flatMap {
                    getAccountFromWalletInfo()
                }
                .doOnSuccess { account ->
                    this.account = account
                }
                .flatMap {
                    updateProviders()
                }
                .flatMap {
                    performPostSignIn()
                }
                .observeOn(scheduler)
                .retry { attempt, error ->
                    error is PostSignInManager.AuthMismatchException && attempt == 1
                }
                .ignoreElement()
    }

    private fun getWalletInfo(email: String, password: CharArray): Single<WalletInfo> {
        val networkRequest = keyServer.getWalletInfo(email, password).toSingle()

        return credentialsPersistence
                ?.takeIf { it.getSavedEmail() == email }
                ?.loadCredentialsMaybe(password)
                ?.switchIfEmpty(networkRequest)
                ?: networkRequest
    }

    private fun getAccountFromWalletInfo(): Single<Account> {
        return Account.fromSecretSeedSingle(walletInfo.secretSeed)
    }

    private fun updateProviders(): Single<Boolean> {
        session.setWalletInfo(walletInfo)
        credentialsPersistence?.saveCredentials(walletInfo, password)
        session.setAccount(account)
        session.signInMethod = SignInMethod.CREDENTIALS

        return Single.just(true)
    }

    private fun performPostSignIn(): Single<Boolean> {
        return postSignInActions
                ?.invoke()
                ?.doOnError {
                    if (it is PostSignInManager.AuthMismatchException) {
                        credentialsPersistence?.clear(keepEmail = true)
                    }
                }
                ?.toSingleDefault(true)
                ?: Single.just(false)
    }
}