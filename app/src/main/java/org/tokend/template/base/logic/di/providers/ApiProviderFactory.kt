package org.tokend.template.base.logic.di.providers

import okhttp3.CookieJar
import org.tokend.sdk.api.tfa.TfaCallback
import org.tokend.template.BuildConfig
import org.tokend.wallet.Account

class ApiProviderFactory {
    fun createApiProvider(accountProvider: AccountProvider,
                          tfaCallback: TfaCallback? = null,
                          cookieJar: CookieJar? = null): ApiProvider {
        return createApiProvider(BuildConfig.API_URL, accountProvider, tfaCallback, cookieJar)
    }

    fun createApiProvider(account: Account? = null,
                          tfaCallback: TfaCallback? = null,
                          cookieJar: CookieJar? = null): ApiProvider {
        return createApiProvider(
                BuildConfig.API_URL,
                AccountProviderFactory().createAccountProvider(account),
                tfaCallback,
                cookieJar
        )
    }

    fun createApiProvider(url: String,
                          accountProvider: AccountProvider,
                          tfaCallback: TfaCallback? = null,
                          cookieJar: CookieJar? = null): ApiProvider {
        return ApiProviderImpl(url, accountProvider, tfaCallback, cookieJar)
    }
}