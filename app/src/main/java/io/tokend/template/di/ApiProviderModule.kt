package io.tokend.template.di

import dagger.Module
import dagger.Provides
import io.tokend.template.features.tfa.logic.AppTfaCallback
import io.tokend.template.logic.providers.UrlConfigProvider
import io.tokend.template.logic.providers.AccountProvider
import io.tokend.template.logic.providers.ApiProvider
import io.tokend.template.logic.providers.ApiProviderFactory
import io.tokend.template.logic.providers.WalletInfoProvider
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
        walletInfoProvider: WalletInfoProvider,
        tfaCallback: AppTfaCallback
    ): ApiProvider {
        return ApiProviderFactory()
            .createApiProvider(
                urlConfigProvider,
                accountProvider,
                walletInfoProvider,
                tfaCallback,
                cookieJar
            )
    }
}