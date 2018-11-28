package org.tokend.template.test

import org.junit.*
import org.tokend.template.BuildConfig
import org.tokend.template.data.model.UrlConfig
import org.tokend.template.di.providers.ApiProviderFactory
import org.tokend.template.di.providers.UrlConfigProviderImpl
import org.tokend.template.features.signup.logic.SignUpUseCase

class SignUpTest {
    @Test
    fun signUp() {
        val defaultUrlConfig = UrlConfig(BuildConfig.API_URL, "", "", "")
        val urlConfigProvider = UrlConfigProviderImpl().apply {
            setConfig(defaultUrlConfig)
        }

        val email = "${System.currentTimeMillis()}@mail.com"
        val password = "qwe123".toCharArray()
        val keyStorage = ApiProviderFactory().createApiProvider(urlConfigProvider).getKeyStorage()

        val useCase = SignUpUseCase(email, password, keyStorage)

        useCase.perform().blockingGet()

        val walletInfo = keyStorage.getWalletInfo(email, password, false)

        Assert.assertEquals(email, walletInfo.email)
    }
}