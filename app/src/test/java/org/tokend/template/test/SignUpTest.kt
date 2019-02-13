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
        val password = Config.DEFAULT_PASSWORD
        val keyStorage = ApiProviderFactory().createApiProvider(urlConfigProvider).getKeyServer()

        val useCase = SignUpUseCase(email, password, keyStorage)

        useCase.perform().blockingGet()

        try {
            val walletInfo = keyStorage.getWalletInfo(email, password, false)
                    .execute().get()

            Assert.assertEquals("Wallet email must be the same as the used one for sign up",
                    email, walletInfo.email)
        } catch (e: Exception) {
            Assert.fail("Wallet must be accessible with specified credentials")
        }

    }
}