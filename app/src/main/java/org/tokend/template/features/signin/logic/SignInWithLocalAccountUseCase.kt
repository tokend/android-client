package org.tokend.template.features.signin.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.keyserver.models.KdfAttributes
import org.tokend.sdk.keyserver.models.LoginParams
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.template.features.localaccount.model.LocalAccount
import org.tokend.template.features.localaccount.repository.LocalAccountRepository
import org.tokend.template.logic.Session
import org.tokend.template.logic.persistance.CredentialsPersistor
import org.tokend.wallet.Account

/**
 * Performs sign in with given [Account]:
 * requests authorization result for given [Account],
 * sets up WalletInfoProvider and AccountProvider,
 * clears CredentialsPersistor.
 *
 * @param postSignInManager if set then [PostSignInManager.doPostSignIn] will be performed
 */
class SignInWithLocalAccountUseCase(
        private val localAccountRepository: LocalAccountRepository,
        private val session: Session,
        private val credentialsPersistor: CredentialsPersistor,
        private val postSignInManager: PostSignInManager?
) {
    private lateinit var localAccount: LocalAccount
    private lateinit var account: Account
    private lateinit var walletInfo: WalletInfo

    fun perform(): Completable {
        return getLocalAccount()
                .doOnSuccess { localAccount ->
                    this.localAccount = localAccount
                }.flatMap {
                    getAccount()
                }
                .doOnSuccess { account ->
                    this.account = account
                }
                .flatMap {
                    getWalletInfo()
                }
                .doOnSuccess { walletInfo ->
                    this.walletInfo = walletInfo
                }
                .flatMap {
                    updateProviders()
                }
                .flatMap {
                    performPostSignIn()
                }
                .ignoreElement()
    }

    private fun getLocalAccount(): Single<LocalAccount> {
        return localAccountRepository
                .item
                .toMaybe()
                .switchIfEmpty(Single.error(IllegalStateException("There is no local account in the repository")))
    }

    private fun getAccount(): Single<Account> {
        return {
            localAccount.account
        }.toSingle().subscribeOn(Schedulers.computation())
    }

    private fun getWalletInfo(): Single<WalletInfo> {
        val accountId = account.accountId

        return WalletInfo(
                accountId = accountId,
                email = accountId.substring(0..3) + "..." +
                        accountId.substring(accountId.length - 4, accountId.length),
                secretSeed = charArrayOf(),
                walletIdHex = "",
                loginParams = LoginParams("", 0,
                        KdfAttributes("", 0, 0, 0, 0, byteArrayOf()))
        ).toSingle()
    }

    private fun updateProviders(): Single<Boolean> {
        session.setWalletInfo(walletInfo)
        credentialsPersistor.clear(false)
        session.setAccount(account)
        session.signInMethod = SignInMethod.LOCAL_ACCOUNT

        return Single.just(true)
    }

    private fun performPostSignIn(): Single<Boolean> {
        return if (postSignInManager != null)
            postSignInManager
                    .doPostSignIn()
                    .toSingleDefault(true)
        else
            Single.just(false)
    }
}