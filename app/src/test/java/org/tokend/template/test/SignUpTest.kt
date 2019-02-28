package org.tokend.template.test

import org.junit.Assert
import org.junit.Test
import org.tokend.template.di.providers.ApiProviderFactory
import org.tokend.template.features.signup.logic.SignUpUseCase

class SignUpTest {
    @Test
    fun signUp() {
        val urlConfigProvider = Util.getUrlConfigProvider()

        val email = Util.getEmail()
        val password = Config.DEFAULT_PASSWORD
        val keyStorage = ApiProviderFactory().createApiProvider(urlConfigProvider).getKeyServer()

        val useCase = SignUpUseCase(email, password, keyStorage)

        useCase.perform().blockingGet()

        try {
            val walletInfo = keyStorage.getWalletInfo(email, password, false)
                    .execute().get()

            Assert.assertTrue("Wallet email must be the same as the used one for sign up",
                    email.equals(walletInfo.email, true))
        } catch (e: Exception) {
            Assert.fail("Wallet must be accessible with specified credentials")
        }

    }
}