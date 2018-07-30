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

    private var apiByUrl: Pair<String, ApiService>? = null
    private var ketStorageByUrl: Pair<String, KeyStorage>? = null

    private var signedApiUrl = ""
    private var signedApiByAccountHash: Pair<Int, ApiService>? = null

    private var signedKeyStorageUrl = ""
    private var signedKeyStorageByAccountHash: Pair<Int, KeyStorage>? = null

    override fun getApi(): ApiService {
        val api = apiByUrl
                ?.takeIf { (url, _) ->
                    this.url == url
                }
                ?.second
                ?: ApiFactory(url).getApiService(null, tfaCallback, cookieJarProvider)

        apiByUrl = Pair(url, api)

        return api
    }

    override fun getKeyStorage(): KeyStorage {
        val keyStorage = ketStorageByUrl
                ?.takeIf { (url, _) ->
                    this.url == url
                }
                ?.second
                ?: KeyStorage(url, tfaCallback, cookieJarProvider)

        ketStorageByUrl = Pair(url, keyStorage)

        return keyStorage
    }

    override fun getSignedApi(): ApiService? {
        val account = accountProvider.getAccount() ?: return null

        val signedApi =
                signedApiByAccountHash
                        ?.takeIf { (accountHash, _) ->
                            accountHash == account.hashCode()
                                    && signedApiUrl == url
                        }
                        ?.second
                        ?: createSignedApiWithAccount(account)

        signedApiByAccountHash = Pair(account.hashCode(), signedApi)
        signedApiUrl = url

        return signedApi
    }

    override fun getSignedKeyStorage(): KeyStorage? {
        val account = accountProvider.getAccount() ?: return null

        val signedKeyStorage =
                signedKeyStorageByAccountHash
                        ?.takeIf { (accountHash, _) ->
                            accountHash == account.hashCode()
                                    && signedKeyStorageUrl == url
                        }
                        ?.second
                        ?: createSignedKeyStorageWithAccount(account)

        signedKeyStorageByAccountHash = Pair(account.hashCode(), signedKeyStorage)
        signedKeyStorageUrl = url

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