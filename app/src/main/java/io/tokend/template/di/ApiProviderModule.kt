package io.tokend.template.di

import dagger.Module
import dagger.Provides
import io.tokend.template.di.providers.AccountProvider
import io.tokend.template.di.providers.ApiProvider
import io.tokend.template.di.providers.ApiProviderFactory
import io.tokend.template.di.providers.UrlConfigProvider
import io.tokend.template.features.tfa.logic.AppTfaCallback
import okhttp3.CookieJar
import javax.inject.Singleton

@Module
class ApiProviderModule(
    private val cookieJar: CookieJar?
) {
    @Provides
    @Singleton
    fun apiProvider(
        urlConfigProvider: UrlConfigProvider,
        accountProvider: AccountProvider,
        tfaCallback: AppTfaCallback
    ): ApiProvider {
        return ApiProviderFactory()
            .createApiProvider(urlConfigProvider, accountProvider, tfaCallback, cookieJar)
    }
}