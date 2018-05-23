package org.tokend.template.base.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.keyserver.KeyStorage
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.template.base.logic.di.providers.AccountProvider
import org.tokend.template.base.logic.di.providers.RepositoryProvider
import org.tokend.template.base.logic.di.providers.WalletInfoProvider
import org.tokend.template.base.logic.persistance.CredentialsPersistor
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
            credentialsPersistor.takeIf { it.hasCredentials(email) }
                    ?.loadCredentials(password)
                    ?: keyStorage.getWalletInfo(email, password)
        }.toSingle()
    }

    private fun getAccount(seed: CharArray): Single<Account> {
        return {
            Account.fromSecretSeed(seed)
        }.toSingle().subscribeOn(Schedulers.computation())
    }
    // endregion

    // region Post sign in
    fun doPostSignIn(repositoryProvider: RepositoryProvider): Completable {
        val parallelActions = listOf<Completable>(
                // Added actions will be performed simultaneously.
                repositoryProvider.balances().updateDeferred()
                        .onErrorResumeNext {
                            if (it is HttpException
                                    && it.code() == HttpURLConnection.HTTP_UNAUTHORIZED)
                                Completable.error(
                                        InvalidPersistedCredentialsException()
                                )
                            else
                                Completable.error(it)
                        },
                repositoryProvider.tfaBackends().updateDeferred()
                        .onErrorResumeNext {
                            if (it is HttpException
                                    && it.code() == HttpURLConnection.HTTP_NOT_FOUND)
                                Completable.error(
                                        InvalidPersistedCredentialsException()
                                )
                            else
                                Completable.error(it)
                        }
        )
        val syncActions = listOf<Completable>(
                // Added actions will be performed on after another in
                // provided order.
        )

        val performParallelActions = Completable.merge(parallelActions)
        val performSyncActions = Completable.concat(syncActions)

        return performParallelActions
                .andThen(performSyncActions)
                .doOnError {
                    if (it is InvalidPersistedCredentialsException) {
                        credentialsPersistor.clear(keepEmail = true)
                    }
                }
    }
    // endregion
}