package org.tokend.template.base.logic.di.providers

import okhttp3.CookieJar
import org.tokend.sdk.api.ApiService
import org.tokend.sdk.api.requests.CookieJarProvider
import org.tokend.sdk.api.requests.RequestSigner
import org.tokend.sdk.api.tfa.TfaCallback
import org.tokend.sdk.factory.ApiFactory
import org.tokend.sdk.keyserver.KeyStorage
import org.tokend.sdk.utils.HashCodes
import org.tokend.wallet.Account

class ApiProviderImpl(
        private val urlConfigProvider: UrlConfigProvider,
        private val accountProvider: AccountProvider,
        private val tfaCallback: TfaCallback?,
        cookieJar: CookieJar?) : ApiProvider {
    private val url: String
        get() = urlConfigProvider.getConfig().api

    private var cookieJarProvider = cookieJar?.let {
        object : CookieJarProvider {
            override fun getCookieJar(): CookieJar {
                return it
            }
        }
    }

    private var apiByHash: Pair<Int, ApiService>? = null
    private var ketStorageByHash: Pair<Int, KeyStorage>? = null

    private var signedApiByHash: Pair<Int, ApiService>? = null
    private var signedKeyStorageByHash: Pair<Int, KeyStorage>? = null

    override fun getApi(): ApiService {
        val hash = url.hashCode()

        val api = apiByHash
                ?.takeIf { (currentHash, _) ->
                    currentHash == hash
                }
                ?.second
                ?: ApiFactory(url).getApiService(null, tfaCallback, cookieJarProvider)

        apiByHash = Pair(hash, api)

        return api
    }

    override fun getKeyStorage(): KeyStorage {
        val hash = url.hashCode()

        val keyStorage = ketStorageByHash
                ?.takeIf { (currentHash, _) ->
                    currentHash == hash
                }
                ?.second
                ?: KeyStorage(url, tfaCallback, cookieJarProvider)

        ketStorageByHash = Pair(hash, keyStorage)

        return keyStorage
    }

    override fun getSignedApi(): ApiService? {
        val account = accountProvider.getAccount() ?: return null
        val hash = HashCodes.ofMany(account, url)

        val signedApi =
                signedApiByHash
                        ?.takeIf { (currentHash, _) ->
                            currentHash == hash
                        }
                        ?.second
                        ?: createSignedApiWithAccount(ApiService::class.java, account)

        signedApiByHash = Pair(hash, signedApi)

        return signedApi
    }

    override fun getSignedKeyStorage(): KeyStorage? {
        val account = accountProvider.getAccount() ?: return null
        val hash = HashCodes.ofMany(account, url)

        val signedKeyStorage =
                signedKeyStorageByHash
                        ?.takeIf { (currentHash, _) ->
                            currentHash == hash
                        }
                        ?.second
                        ?: createSignedKeyStorageWithAccount(account)

        signedKeyStorageByHash = Pair(hash, signedKeyStorage)

        return signedKeyStorage
    }

    private fun <T> createSignedApiWithAccount(apiClass: Class<T>, account: Account): T {
        return ApiFactory(url).getCustomService(apiClass,
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