package org.tokend.template.test

import org.junit.Assert
import org.junit.Test
import org.tokend.template.di.providers.ApiProviderFactory
import org.tokend.template.features.signup.logic.SignUpUseCase

class SignUpTest {
    @Test
    fun signUp() {
        val urlConfigProvider = Util.getUrlConfigProvider()

        val email = "${System.currentTimeMillis()}@mail.com"
        val password = "qwe123".toCharArray()
        val keyStorage = ApiProviderFactory().createApiProvider(urlConfigProvider).getKeyServer()

        val useCase = SignUpUseCase(email, password, keyStorage)

        useCase.perform().blockingGet()

        val walletInfo = keyStorage.getWalletInfo(email, password, false)
                .execute().get()

        Assert.assertEquals(email, walletInfo.email)
    }
}