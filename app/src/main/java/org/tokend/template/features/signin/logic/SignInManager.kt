package org.tokend.template.features.signin.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.keyserver.KeyStorage
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.logic.persistance.CredentialsPersistor
import org.tokend.wallet.Account
import retrofit2.HttpException
import java.net.HttpURLConnection

class SignInManager(
        private val keyStorage: KeyStorage,
        private val walletInfoProvider: WalletInfoProvider,
        private val accountProvider: AccountProvider,
        private val credentialsPersistor: CredentialsPersistor) {
    class InvalidPersistedCredentialsException : Exception()

    // region Sign in
    /**
     * Performs keypair loading and decryption,
     * sets up WalletInfoProvider and AccountProvider, updates CredentialsPersistor.
     * If CredentialsPersistor contains saved credentials no network calls will be performed.
     */
    fun signIn(email: String, password: CharArray): Completable {
        return getWalletInfo(email, password)
                .flatMap { walletInfo ->
                    walletInfoProvider.setWalletInfo(walletInfo)
                    credentialsPersistor.saveCredentials(walletInfo, password)
                    getAccount(walletInfo.secretSeed)
                }
                .doOnSuccess { account ->
                    walletInfoProvider.getWalletInfo()?.secretSeed?.fill('0')
                    accountProvider.setAccount(account)
                }
                .toCompletable()
    }

    private fun getWalletInfo(email: String, password: CharArray): Single<WalletInfo> {
        return {
            credentialsPersistor
                    .takeIf { it.getSavedEmail() == email }
                    ?.loadCredentials(password)
                    ?: keyStorage.getWalletInfo(email, password)
        }.toSingle()
    }

    private fun getAccount(seed: CharArray): Single<Account> {
        return {
            Account.fromSecretSeed(seed)
        }.toSingle().subscribeOn(Schedulers.newThread())
    }
    // endregion

    // region Post sign in
    /**
     * Updates all important repositories, creates account on first sign in.
     */
    fun doPostSignIn(repositoryProvider: RepositoryProvider): Completable {
        val parallelActions = listOf<Completable>(
                // Added actions will be performed simultaneously.

                repositoryProvider.balances().updateDeferred(),
                repositoryProvider.tfaBackends().updateDeferred()
                        .onErrorResumeNext {
                            if (it is HttpException
                                    && it.code() == HttpURLConnection.HTTP_NOT_FOUND)
                                Completable.error(
                                        InvalidPersistedCredentialsException()
                                )
                            else
                                Completable.error(it)
                        },
                repositoryProvider.favorites().updateIfNotFreshDeferred()
        )
        val syncActions = listOf<Completable>(
                // Added actions will be performed on after another in
                // provided order.

                // Update account just to create user for the first time.
                repositoryProvider.account().updateDeferred()
                        .onErrorResumeNext { error ->
                            if (error is HttpException
                                    && error.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                                createUnverifiedUser(repositoryProvider)
                            } else if (error is HttpException
                                    && error.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                Completable.error(InvalidPersistedCredentialsException())
                            } else {
                                // Other account update errors are not critical.
                                Completable.complete()
                            }
                        }
        )

        val performParallelActions = Completable.merge(parallelActions)
        val performSyncActions = Completable.concat(syncActions)

        return performSyncActions
                .andThen(performParallelActions)
                .doOnError {
                    if (it is InvalidPersistedCredentialsException) {
                        credentialsPersistor.clear(keepEmail = true)
                    }
                }
    }

    private fun createUnverifiedUser(repositoryProvider: RepositoryProvider): Completable {
        return repositoryProvider.user().createUnverified()
                .onErrorResumeNext {
                    if (it is HttpException
                            && (it.code() == HttpURLConnection.HTTP_UNAUTHORIZED
                                    || it.code() == HttpURLConnection.HTTP_FORBIDDEN)
                    )
                        Completable.error(
                                InvalidPersistedCredentialsException()
                        )
                    else
                        Completable.error(it)
                }
    }
    // endregion
}