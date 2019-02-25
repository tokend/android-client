package org.tokend.template.test

import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.template.di.providers.*
import org.tokend.template.features.signin.logic.PostSignInManager
import org.tokend.template.features.signin.logic.SignInUseCase
import org.tokend.template.logic.Session

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SignInTest {
    @Test
    fun aFirstSignIn() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )
        val apiProvider = ApiProviderFactory().createApiProvider(urlConfigProvider, session)

        val email = Util.getEmail()
        val password = Config.DEFAULT_PASSWORD

        val (walletData, rootAccount, recoveryAccount)
                = apiProvider.getKeyServer().createAndSaveWallet(email, password)
                .execute().get()

        System.out.println("Email is $email")
        System.out.println("Recovery seed is ${recoveryAccount.secretSeed!!.joinToString("")}")

        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider,
                JsonApiToolsProvider.getObjectMapper())

        val useCase = SignInUseCase(
                email,
                password,
                apiProvider.getKeyServer(),
                session,
                null,
                PostSignInManager(repositoryProvider)
        )

        useCase.perform().blockingAwait()

        Assert.assertEquals("WalletInfoProvider must hold an actual wallet data",
                walletData.attributes!!.accountId, session.getWalletInfo()!!.accountId)
        Assert.assertArrayEquals("AccountProvider must hold an actual account",
                rootAccount.secretSeed, session.getAccount()?.secretSeed)

        checkRepositories(repositoryProvider)
    }

    @Test
    fun bRegularSignIn() {
        performSignInTest("${System.currentTimeMillis()}@mail.com")
    }

    @Test
    fun cDifferentEmailCaseSignIn() {
        performSignInTest("aBcD${System.currentTimeMillis()}@mail.com")
    }

    private fun performSignInTest(email: String) {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )
        val apiProvider = ApiProviderFactory().createApiProvider(urlConfigProvider, session)

        val password = Config.DEFAULT_PASSWORD

        val (walletData, rootAccount, recoveryAccount)
                = apiProvider.getKeyServer().createAndSaveWallet(email, password)
                .execute().get()

        System.out.println("Email is $email")
        System.out.println("Recovery seed is ${recoveryAccount.secretSeed!!.joinToString("")}")

        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider,
                JsonApiToolsProvider.getObjectMapper())

        val useCase = SignInUseCase(
                email,
                password,
                apiProvider.getKeyServer(),
                session,
                null,
                PostSignInManager(repositoryProvider)
        )

        useCase.perform().blockingAwait()

        Assert.assertEquals("WalletInfoProvider must hold an actual wallet data",
                walletData.attributes!!.accountId, session.getWalletInfo()!!.accountId)
        Assert.assertArrayEquals("AccountProvider must hold an actual account",
                rootAccount.secretSeed, session.getAccount()?.secretSeed)

        checkRepositories(repositoryProvider)
    }

    private fun checkRepositories(repositoryProvider: RepositoryProvider) {
        Assert.assertTrue("Balances repository must be updated after sign in",
                repositoryProvider.balances().isFresh)
        Assert.assertTrue("TFA factors repository must be updated after sign in",
                repositoryProvider.tfaFactors().isFresh)
    }
}