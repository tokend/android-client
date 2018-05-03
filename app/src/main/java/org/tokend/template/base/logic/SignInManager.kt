package org.tokend.template.base.logic

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.keyserver.KeyStorage
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.template.BuildConfig
import org.tokend.template.base.logic.di.AccountModule
import org.tokend.template.base.logic.di.WalletInfoModule
import org.tokend.wallet.Account

object SignInManager {
    fun signIn(email: String, password: String): Completable {
        return getWalletInfo(email, password)
                .flatMap { walletInfo ->
                    WalletInfoModule.walletInfo = walletInfo

                    getAccount(walletInfo.secretSeed)
                }
                .doOnNext { account ->
                    AccountModule.account = account
                }
                .ignoreElements()
    }

    private fun getWalletInfo(email: String, password: String): Observable<WalletInfo> {
        return Observable.defer {
            Observable.just(KeyStorage(BuildConfig.API_URL)
                    .getWalletInfo(email, password))
        }
    }

    private fun getAccount(seed: String): Observable<Account> {
        return Observable.defer {
            Observable.just(Account.fromSecretSeed(seed))
        }.subscribeOn(Schedulers.computation())
    }
}