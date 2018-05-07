package org.tokend.template.base.logic.di

import dagger.Module
import dagger.Provides
import okhttp3.CookieJar
import org.tokend.template.base.logic.AppTfaCallback
import org.tokend.template.base.logic.di.providers.AccountProvider
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.base.logic.di.providers.ApiProviderImpl
import javax.inject.Singleton

@Module
class ApiProviderModule(
        private val url: String,
        private val cookieJar: CookieJar?) {
    @Provides
    @Singleton
    fun apiProvider(accountProvider: AccountProvider,
                    tfaCallback: AppTfaCallback): ApiProvider {
        return ApiProviderImpl(url, accountProvider, tfaCallback, cookieJar)
    }
}