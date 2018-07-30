package org.tokend.template.base.logic.di

import dagger.Module
import dagger.Provides
import okhttp3.CookieJar
import org.tokend.template.base.logic.AppTfaCallback
import org.tokend.template.base.logic.di.providers.AccountProvider
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.base.logic.di.providers.ApiProviderFactory
import org.tokend.template.base.logic.di.providers.UrlConfigProvider
import javax.inject.Singleton

@Module
class ApiProviderModule(
        private val cookieJar: CookieJar?) {
    @Provides
    @Singleton
    fun apiProvider(urlConfigProvider: UrlConfigProvider,
                    accountProvider: AccountProvider,
                    tfaCallback: AppTfaCallback): ApiProvider {
        return ApiProviderFactory()
                .createApiProvider(urlConfigProvider, accountProvider, tfaCallback, cookieJar)
    }
}