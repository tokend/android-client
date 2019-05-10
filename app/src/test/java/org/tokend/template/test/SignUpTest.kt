package org.tokend.template.test

import org.junit.Assert
import org.junit.Test
import org.tokend.sdk.api.wallets.model.EmailAlreadyTakenException
import org.tokend.template.di.providers.ApiProviderFactory
import org.tokend.template.features.signup.logic.SignUpUseCase

class SignUpTest {
    @Test
    fun aSignUp() {
        val urlConfigProvider = Util.getUrlConfigProvider()

        val email = Util.getEmail()
        val password = Config.DEFAULT_PASSWORD
        val keyStorage = ApiProviderFactory().createApiProvider(urlConfigProvider).getKeyServer()

        val useCase = SignUpUseCase(email, password, keyStorage, null)

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

    @Test
    fun bSignUpWithExistingEmail() {
        val urlConfigProvider = Util.getUrlConfigProvider()

        val email = Util.getEmail()
        val password = Config.DEFAULT_PASSWORD
        val keyStorage = ApiProviderFactory().createApiProvider(urlConfigProvider).getKeyServer()

        val useCase = SignUpUseCase(email, password, keyStorage, null)

        try {
            useCase.perform().blockingGet()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            useCase.perform().blockingGet()
        } catch (e: Exception) {
            if (e is RuntimeException && e.cause is EmailAlreadyTakenException) {
                // Nice.
            } else {
                e.printStackTrace()
                Assert.fail("${EmailAlreadyTakenException::class.java.name} expected " +
                        "but ${e::class.java.name} occurred")
            }

            return
        }

        Assert.fail("${EmailAlreadyTakenException::class.java.name} expected")
    }
}