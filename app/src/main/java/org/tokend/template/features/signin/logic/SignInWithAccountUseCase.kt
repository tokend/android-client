package org.tokend.template.features.signin.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.api.authenticator.AuthResultsApi
import org.tokend.sdk.api.authenticator.model.AuthResult
import org.tokend.sdk.keyserver.KeyStorage
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.template.extensions.toSingle
import org.tokend.template.logic.Session
import org.tokend.template.logic.persistance.CredentialsPersistor
import org.tokend.wallet.Account
import retrofit2.HttpException
import java.net.HttpURLConnection

/**
 * Performs sign in with given [Account]:
 * requests authorization result for given [Account],
 * sets up WalletInfoProvider and AccountProvider,
 * clears CredentialsPersistor.
 *
 * @param postSignInManager if set then [PostSignInManager.doPostSignIn] will be performed
 */
class SignInWithAccountUseCase(
        private val account: Account,
        private val keyStorage: KeyStorage,
        private val authResultsApi: AuthResultsApi,
        private val session: Session,
        private val credentialsPersistor: CredentialsPersistor,
        private val postSignInManager: PostSignInManager?
) {
    class AuthResultNotFoundException : Exception()
    class AuthResultUnsuccessfulException : Exception()

    private val publicKey = account.accountId

    private lateinit var authResult: AuthResult
    private lateinit var walletInfo: WalletInfo

    fun perform(): Completable {
        val scheduler = Schedulers.newThread()

        return getAuthResult()
                .doOnSuccess { authResult ->
                    this.authResult = authResult
                }
                .observeOn(scheduler)
                .flatMap {
                    checkIfAuthResultSuccessful()
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
                .observeOn(scheduler)
                .ignoreElement()
    }

    private fun getAuthResult(): Single<AuthResult> {
        return authResultsApi
                .getAuthResult(publicKey)
                .toSingle()
                .onErrorResumeNext {
                    if (it is HttpException
                            && it.code() == HttpURLConnection.HTTP_NOT_FOUND)
                        Single.error(AuthResultNotFoundException())
                    else
                        Single.error(it)
                }
    }

    private fun getWalletInfo(): Single<WalletInfo> {
        val walletIdHex = authResult.walletId

        return {
            val walletData = keyStorage.getWalletData(walletIdHex)
            val email = walletData.attributes!!.email
            val loginParams = keyStorage.getLoginParams(email)

            WalletInfo(walletData.attributes!!.accountId, email, walletIdHex,
                    CharArray(0), loginParams)
        }.toSingle().subscribeOn(Schedulers.newThread())
    }

    private fun checkIfAuthResultSuccessful(): Single<Boolean> {
        return if (authResult.isSuccessful && authResult.walletId.isNotBlank())
            Single.just(true)
        else
            Single.error(AuthResultUnsuccessfulException())
    }

    private fun updateProviders(): Single<Boolean> {
        session.setWalletInfo(walletInfo)
        credentialsPersistor.clear(false)
        session.setAccount(account)
        session.signInMethod = SignInMethod.AUTHENTICATOR

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