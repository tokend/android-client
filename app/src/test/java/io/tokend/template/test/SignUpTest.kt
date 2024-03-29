package io.tokend.template.test

import io.tokend.template.logic.providers.ApiProviderFactory
import io.tokend.template.logic.providers.RepositoryProviderImpl
import io.tokend.template.logic.providers.WalletInfoProviderImpl
import io.tokend.template.features.signup.logic.SignUpUseCase
import org.junit.Assert
import org.junit.Test
import org.tokend.sdk.api.wallets.model.EmailAlreadyTakenException
import org.tokend.sdk.factory.JsonApiTools
import org.tokend.sdk.keyserver.KeyServer

class SignUpTest {
    @Test
    fun aSignUp() {
        val urlConfigProvider = Util.getUrlConfigProvider()

        val email = Util.getEmail()
        val password = Config.DEFAULT_PASSWORD
        val apiProvider = ApiProviderFactory().createApiProvider(urlConfigProvider)
        val repositoryProvider = RepositoryProviderImpl(
            apiProvider, WalletInfoProviderImpl(),
            urlConfigProvider,
            JsonApiTools.objectMapper
        )

        val useCase = SignUpUseCase(
            email, password, KeyServer(apiProvider.getApi().wallets),
            repositoryProvider
        )

        useCase.perform().blockingGet()

        try {
            val walletInfo = KeyServer(apiProvider.getApi().wallets)
                .getWallet(email, password, false)
                .execute()
                .get()

            Assert.assertTrue(
                "Wallet login must be the same as the used one for sign up",
                email.equals(walletInfo.login, true)
            )
        } catch (e: Exception) {
            Assert.fail("Wallet must be accessible with specified credentials")
        }
    }

    @Test
    fun bSignUpWithExistingEmail() {
        val urlConfigProvider = Util.getUrlConfigProvider()

        val email = Util.getEmail()
        val password = Config.DEFAULT_PASSWORD
        val apiProvider = ApiProviderFactory().createApiProvider(urlConfigProvider)
        val repositoryProvider = RepositoryProviderImpl(
            apiProvider, WalletInfoProviderImpl(),
            urlConfigProvider,
            JsonApiTools.objectMapper
        )

        val useCase = SignUpUseCase(
            email, password, KeyServer(apiProvider.getApi().wallets),
            repositoryProvider
        )

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
                Assert.fail(
                    "${EmailAlreadyTakenException::class.java.name} expected " +
                            "but ${e::class.java.name} occurred"
                )
            }

            return
        }

        Assert.fail("${EmailAlreadyTakenException::class.java.name} expected")
    }
}