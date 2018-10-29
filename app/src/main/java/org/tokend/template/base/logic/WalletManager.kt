package org.tokend.template.base.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.keyserver.KeyStorage
import org.tokend.sdk.keyserver.models.LoginParams
import org.tokend.sdk.keyserver.models.WalletData
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.wallet.Account

class WalletManager(
        private val keyStorage: KeyStorage
) {
    // region API interaction
    fun getWalletInfo(email: String, password: CharArray,
                      isRecovery: Boolean = false): Single<WalletInfo> {
        return {
            keyStorage.getWalletInfo(email, password, isRecovery)
        }.toSingle()
    }

    fun saveWallet(walletData: WalletData): Completable {
        return {
            keyStorage.saveWallet(walletData)
        }.toSingle().toCompletable()
    }

    fun updateWallet(walletId: String, walletData: WalletData): Completable {
        return {
            keyStorage.updateWallet(walletId, walletData)
        }.toSingle().toCompletable()
    }
    // endregion

    companion object {
        /**
         * @return Fully armed and ready to go [WalletData].
         */
        fun createWallet(email: String,
                         password: CharArray,
                         rootAccount: Account,
                         recoveryAccount: Account,
                         loginParams: LoginParams): Single<WalletData> {
            return {
                KeyStorage.createWallet(
                        email,
                        password,
                        loginParams,
                        rootAccount,
                        recoveryAccount
                ).walletData
            }.toSingle().subscribeOn(Schedulers.newThread())
        }
    }
}