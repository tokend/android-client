package io.tokend.template.features.signin.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.tokend.template.logic.credentials.model.WalletInfoRecord
import io.tokend.template.logic.credentials.persistence.CredentialsPersistence
import io.tokend.template.logic.credentials.persistence.WalletInfoPersistence
import io.tokend.template.logic.session.Session
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.keyserver.KeyServer
import org.tokend.wallet.Account

/**
 * Performs sign in with given credentials:
 * performs keypair loading and decryption,
 * sets up [Session], updates [CredentialsPersistence] and [WalletInfoPersistence] if any is set.
 * If [WalletInfoPersistence] contains saved credentials no network calls will be performed.
 *
 * @see PostSignInManager
 */
class SignInUseCase(
    private val login: String,
    private val password: CharArray,
    private val keyServer: KeyServer,
    private val session: Session,
    private val credentialsPersistence: CredentialsPersistence?,
    private val walletInfoPersistence: WalletInfoPersistence?,
    private val postSignInActions: (() -> Completable)?
) {
    private lateinit var walletInfo: WalletInfoRecord

    private var ignoreWalletInfoPersistence = false

    fun perform(): Completable {
        val scheduler = Schedulers.newThread()

        return getWalletInfo(login, password)
            .doOnSuccess { walletInfo ->
                this.walletInfo = walletInfo
            }
            .observeOn(scheduler)
            .flatMap {
                updateProviders()
            }
            .flatMap {
                performPostSignIn()
            }
            .observeOn(scheduler)
            .doOnError { error ->
                // In this case it is possible that password had been changed
                // to the same one, so we can try one more time.
                if (error is PostSignInManager.AuthMismatchException) {
                    ignoreWalletInfoPersistence = true
                }
            }
            .retry { attempt, error ->
                error is PostSignInManager.AuthMismatchException && attempt == 1
            }
            .ignoreElement()
    }

    private fun getWalletInfo(login: String, password: CharArray) = Single.defer {
        val networkRequest = keyServer
            .getWallet(login, password)
            .toSingle()
            .map(::WalletInfoRecord)

        walletInfoPersistence
            ?.takeIf { !ignoreWalletInfoPersistence }
            ?.loadWalletInfoMaybe(login, password)
            ?.switchIfEmpty(networkRequest)
            ?: networkRequest
    }

    private fun updateProviders(): Single<Boolean> {
        Companion.updateProviders(
            walletInfo, walletInfo.accounts, password, session,
            credentialsPersistence, walletInfoPersistence, SignInMethod.CREDENTIALS
        )
        return Single.just(true)
    }

    private fun performPostSignIn(): Single<Boolean> {
        return postSignInActions
            ?.invoke()
            ?.doOnError {
                if (it is PostSignInManager.AuthMismatchException) {
                    walletInfoPersistence?.clearWalletInfo(login)
                }
            }
            ?.toSingleDefault(true)
            ?: Single.just(false)
    }

    companion object {
        fun updateProviders(
            walletInfo: WalletInfoRecord,
            accounts: List<Account>,
            password: CharArray,
            session: Session,
            credentialsPersistence: CredentialsPersistence?,
            walletInfoPersistence: WalletInfoPersistence?,
            signInMethod: SignInMethod?
        ) {
            session.setWalletInfo(walletInfo)
            session.setAccounts(accounts)
            session.signInMethod = signInMethod

            credentialsPersistence?.saveCredentials(walletInfo.login, password)
            walletInfoPersistence?.saveWalletInfo(walletInfo, password)
        }
    }
}