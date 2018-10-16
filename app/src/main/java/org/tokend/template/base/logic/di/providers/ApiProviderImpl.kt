package org.tokend.template.base.logic.di.providers

import okhttp3.CookieJar
import org.tokend.sdk.api.TokenDApi
import org.tokend.sdk.keyserver.KeyStorage
import org.tokend.sdk.tfa.TfaCallback
import org.tokend.sdk.utils.CookieJarProvider
import org.tokend.sdk.utils.HashCodes
import org.tokend.template.base.logic.AccountRequestSigner

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

    private var apiByHash: Pair<Int, TokenDApi>? = null
    private var ketStorageByHash: Pair<Int, KeyStorage>? = null

    private var signedApiByHash: Pair<Int, TokenDApi>? = null
    private var signedKeyStorageByHash: Pair<Int, KeyStorage>? = null

    override fun getApi(): TokenDApi {
        val hash = url.hashCode()

        val api = apiByHash
                ?.takeIf { (currentHash, _) ->
                    currentHash == hash
                }
                ?.second
                ?: TokenDApi(
                        url,
                        null,
                        tfaCallback,
                        cookieJarProvider
                )

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
                ?: KeyStorage(getApi().wallets)

        ketStorageByHash = Pair(hash, keyStorage)

        return keyStorage
    }

    override fun getSignedApi(): TokenDApi? {
        val account = accountProvider.getAccount() ?: return null
        val hash = HashCodes.ofMany(account.accountId, url)

        val signedApi =
                signedApiByHash
                        ?.takeIf { (currentHash, _) ->
                            currentHash == hash
                        }
                        ?.second
                        ?: TokenDApi(
                                url,
                                AccountRequestSigner(account),
                                tfaCallback,
                                cookieJarProvider
                        )

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
                        ?: KeyStorage(getSignedApi()?.wallets!!)

        signedKeyStorageByHash = Pair(hash, signedKeyStorage)

        return signedKeyStorage
    }
}