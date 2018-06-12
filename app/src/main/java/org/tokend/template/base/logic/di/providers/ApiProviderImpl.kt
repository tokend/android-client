package org.tokend.template.base.logic.di.providers

import okhttp3.CookieJar
import org.tokend.sdk.api.ApiService
import org.tokend.sdk.api.requests.CookieJarProvider
import org.tokend.sdk.api.requests.RequestSigner
import org.tokend.sdk.api.tfa.TfaCallback
import org.tokend.sdk.factory.ApiFactory
import org.tokend.sdk.keyserver.KeyStorage
import org.tokend.wallet.Account

class ApiProviderImpl(
        private val url: String,
        private val accountProvider: AccountProvider,
        private val tfaCallback: TfaCallback?,
        cookieJar: CookieJar?) : ApiProvider {

    private var cookieJarProvider = cookieJar?.let {
        object : CookieJarProvider {
            override fun getCookieJar(): CookieJar {
                return it
            }
        }
    }

    private val mApi: ApiService by lazy {
        ApiFactory(url).getApiService(null, tfaCallback, cookieJarProvider)
    }

    private val mKeyStorage: KeyStorage by lazy {
        KeyStorage(url, tfaCallback, cookieJarProvider)
    }

    private var signedApiByAccountHash: Pair<Int, ApiService>? = null
    private var signedKeyStorageByAccountHash: Pair<Int, KeyStorage>? = null

    override fun getApi(): ApiService {
        return mApi
    }

    override fun getKeyStorage(): KeyStorage {
        return mKeyStorage
    }

    override fun getSignedApi(): ApiService? {
        val account = accountProvider.getAccount() ?: return null

        val signedApi =
                signedApiByAccountHash
                        ?.takeIf { (accountHash, _) ->
                            accountHash == account.hashCode()
                        }
                        ?.second
                        ?: createSignedApiWithAccount(account)

        signedApiByAccountHash = Pair(account.hashCode(), signedApi)

        return signedApi
    }

    override fun getSignedKeyStorage(): KeyStorage? {
        val account = accountProvider.getAccount() ?: return null

        val signedKeyStorage =
                signedKeyStorageByAccountHash
                        ?.takeIf { (accountHash, _) ->
                            accountHash == account.hashCode()
                        }
                        ?.second
                        ?: createSignedKeyStorageWithAccount(account)

        signedKeyStorageByAccountHash = Pair(account.hashCode(), signedKeyStorage)

        return signedKeyStorage
    }

    private fun createSignedApiWithAccount(account: Account): ApiService {
        return ApiFactory(url).getApiService(
                object : RequestSigner {
                    override val accountId: String = account.accountId

                    override fun signToBase64(data: ByteArray): String {
                        return account.signDecorated(data).toBase64()
                    }
                },
                tfaCallback, cookieJarProvider)
    }

    private fun createSignedKeyStorageWithAccount(account: Account): KeyStorage {
        return KeyStorage(url, tfaCallback, cookieJarProvider,
                object : RequestSigner {
                    override val accountId: String = account.accountId

                    override fun signToBase64(data: ByteArray): String {
                        return account.signDecorated(data).toBase64()
                    }
                })
    }
}