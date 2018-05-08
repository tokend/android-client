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
import org.tokend.wallet.Account

class SignInManager(
        private val keyStorage: KeyStorage,
        private val walletInfoProvider: WalletInfoProvider,
        private val accountProvider: AccountProvider) {
    // region Sign in
    fun signIn(email: String, password: String): Completable {
        return getWalletInfo(email, password)
                .flatMap { walletInfo ->
                    walletInfoProvider.setWalletInfo(walletInfo)
                    getAccount(walletInfo.secretSeed)
                }
                .doOnSuccess { account ->
                    accountProvider.setAccount(account)
                }
                .toCompletable()
    }

    private fun getWalletInfo(email: String, password: String): Single<WalletInfo> {
        return {
            keyStorage.getWalletInfo(email, password)
        }.toSingle()
    }

    private fun getAccount(seed: String): Single<Account> {
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
        )
        val syncActions = listOf<Completable>(
                // Added actions will be performed on after another in
                // provided order.
        )

        val performParallelActions = Completable.merge(parallelActions)
        val performSyncActions = Completable.concat(syncActions)

        return performParallelActions
                .andThen(performSyncActions)
    }
    // endregion
}