package org.tokend.template.base.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import org.tokend.sdk.keyserver.KeyStorage
import org.tokend.sdk.keyserver.models.LoginParams
import org.tokend.template.base.logic.di.providers.ApiProviderFactory
import org.tokend.template.base.logic.di.providers.UrlConfigProvider
import org.tokend.wallet.Account

class SignUpManager(
        private val urlConfigProvider: UrlConfigProvider
) {
    /**
     * Creates and submits a new wallet.
     */
    fun signUp(email: String, password: CharArray,
               rootAccount: Account, recoveryAccount: Account): Completable {

        val apiProvider = ApiProviderFactory().createApiProvider(urlConfigProvider, rootAccount)
        val walletManager = WalletManager(apiProvider.getKeyStorage())

        return getDefaultLoginParams(apiProvider.getKeyStorage())
                .flatMap { loginParams ->
                    WalletManager.createWallet(
                            email,
                            password,
                            rootAccount,
                            recoveryAccount,
                            loginParams
                    )
                }
                .flatMapCompletable { wallet ->
                    walletManager.saveWallet(wallet)
                }
    }

    private fun getDefaultLoginParams(keyStorage: KeyStorage): Single<LoginParams> {
        return {
            keyStorage.getLoginParams()
        }.toSingle()
    }

    companion object {
        private const val SEED_LENGTH = 16

        fun getRandomAccount(): Single<Account> {
            return {
                Account.random()
            }.toSingle()
        }
    }
}