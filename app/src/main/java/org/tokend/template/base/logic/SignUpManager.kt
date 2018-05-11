package org.tokend.template.base.logic

import com.google.common.io.BaseEncoding
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import org.tokend.sdk.keyserver.KeyStorage
import org.tokend.sdk.keyserver.models.LoginParamsResponse
import org.tokend.template.base.logic.di.providers.ApiProviderFactory
import org.tokend.wallet.Account
import java.security.SecureRandom

class SignUpManager {
    fun signUp(email: String, password: CharArray,
               rootAccount: Account, recoveryAccount: Account,
               derivationSalt: ByteArray = SecureRandom.getSeed(SEED_LENGTH)): Completable {

        val apiProvider = ApiProviderFactory().createApiProvider(rootAccount)
        val walletManager = WalletManager(apiProvider.getKeyStorage())

        val data = object {
            lateinit var loginParams: LoginParamsResponse
        }

        return getDefaultLoginParams(apiProvider.getKeyStorage())
                .flatMap { loginParams ->
                    // Complete default login params with derivation salt.
                    loginParams.kdfAttributes.encodedSalt =
                            BaseEncoding.base64().encode(derivationSalt)
                    data.loginParams = loginParams

                    WalletManager.deriveKeys(email, password, loginParams.kdfAttributes)
                }
                .flatMap { (walletIdHex, walletKey) ->
                    WalletManager.createWallet(email, walletIdHex, walletKey,
                            rootAccount, recoveryAccount,
                            data.loginParams)
                }
                .flatMapCompletable { wallet ->
                    walletManager.saveWallet(wallet)
                }
    }

    private fun getDefaultLoginParams(keyStorage: KeyStorage): Single<LoginParamsResponse> {
        return {
            keyStorage.getApiLoginParams()
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