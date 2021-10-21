package org.tokend.template.di.providers

import okhttp3.CookieJar
import org.tokend.sdk.tfa.TfaCallback
import org.tokend.wallet.Account

class ApiProviderFactory {
    fun createApiProvider(
        urlConfigProvider: UrlConfigProvider,
        account: Account? = null,
        tfaCallback: TfaCallback? = null,
        cookieJar: CookieJar? = null
    ): ApiProvider {
        return createApiProvider(
            urlConfigProvider,
            AccountProviderFactory().createAccountProvider(account?.let { listOf(it) }
                ?: emptyList()),
            tfaCallback,
            cookieJar
        )
    }

    fun createApiProvider(
        urlConfigProvider: UrlConfigProvider,
        accountProvider: AccountProvider,
        tfaCallback: TfaCallback? = null,
        cookieJar: CookieJar? = null
    ): ApiProvider {
        return ApiProviderImpl(urlConfigProvider, accountProvider, tfaCallback, cookieJar)
    }
}