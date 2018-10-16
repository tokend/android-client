package org.tokend.template.base.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.keyserver.KeyStorage
import org.tokend.sdk.keyserver.WalletBuilder
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
                val kdf = loginParams.kdfAttributes
                val rootSeed = rootAccount.secretSeed
                        ?: throw IllegalStateException("Provided account has no private key")

                val builder = WalletBuilder(
                        email,
                        password,
                        rootSeed,
                        rootAccount.accountId,
                        kdf,
                        loginParams.id
                )

                recoveryAccount.secretSeed?.also {
                    builder.addRecoveryRelation(it, recoveryAccount.accountId)
                }

                val passwordFactorAccount = Account.random()
                passwordFactorAccount.secretSeed?.also {
                    builder.addPasswordFactorRelation(it, passwordFactorAccount.accountId)
                }

                builder.build()
            }.toSingle().subscribeOn(Schedulers.newThread())
        }
    }
}