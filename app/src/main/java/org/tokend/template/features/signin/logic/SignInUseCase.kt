package org.tokend.template.features.signin.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.rx.extensions.fromSecretSeedSingle
import org.tokend.sdk.keyserver.KeyStorage
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.template.logic.Session
import org.tokend.template.logic.persistance.CredentialsPersistor
import org.tokend.wallet.Account

/**
 * Performs sign in with given credentials:
 * performs keypair loading and decryption,
 * sets up WalletInfoProvider and AccountProvider, updates CredentialsPersistor if it is set.
 * If CredentialsPersistor contains saved credentials no network calls will be performed.
 *
 * @param postSignInManager if set then [PostSignInManager.doPostSignIn] will be performed
 */
class SignInUseCase(
        private val email: String,
        private val password: CharArray,
        private val keyStorage: KeyStorage,
        private val session: Session,
        private val credentialsPersistor: CredentialsPersistor?,
        private val postSignInManager: PostSignInManager?
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
        return {
            credentialsPersistor
                    ?.takeIf { it.getSavedEmail() == email }
                    ?.loadCredentials(password)
                    ?: keyStorage.getWalletInfo(email, password)
        }.toSingle()
    }

    private fun getAccountFromWalletInfo(): Single<Account> {
        return Account.fromSecretSeedSingle(walletInfo.secretSeed)
    }

    private fun updateProviders(): Single<Boolean> {
        session.setWalletInfo(walletInfo)
        credentialsPersistor?.saveCredentials(walletInfo, password)
        session.setAccount(account)

        return Single.just(true)
    }

    private fun performPostSignIn(): Single<Boolean> {
        return if (postSignInManager != null)
            postSignInManager
                    .doPostSignIn()
                    .doOnError {
                        if (it is PostSignInManager.AuthMismatchException) {
                            credentialsPersistor?.clear(keepEmail = true)
                        }
                    }
                    .toSingleDefault(true)
        else
            Single.just(false)
    }
}