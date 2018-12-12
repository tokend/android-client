package org.tokend.template.test

import org.junit.Assert
import org.junit.Test
import org.tokend.template.di.providers.*
import org.tokend.template.features.signin.logic.PostSignInManager
import org.tokend.template.features.signin.logic.SignInUseCase
import org.tokend.template.logic.Session

class SignInTest {
    @Test
    fun firstSignIn() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )
        val apiProvider = ApiProviderFactory().createApiProvider(urlConfigProvider, session)

        val email = "${System.currentTimeMillis()}@mail.com"
        val password = "qwe123".toCharArray()

        val (walletData, rootAccount, recoveryAccount)
                = apiProvider.getKeyStorage().createAndSaveWallet(email, password)

        System.out.println("Email is $email")
        System.out.println("Recovery seed is ${recoveryAccount.secretSeed!!.joinToString("")}")

        val repositoryProvider = RepositoryProviderImpl(apiProvider, session)

        val useCase = SignInUseCase(
                email,
                password,
                apiProvider.getKeyStorage(),
                session,
                null,
                PostSignInManager(repositoryProvider)
        )

        useCase.perform().blockingAwait()

        Assert.assertEquals(walletData.attributes!!.accountId, session.getWalletInfo()!!.accountId)
        Assert.assertArrayEquals(rootAccount.secretSeed, session.getAccount()?.secretSeed)

        checkRepositories(repositoryProvider)
    }

    @Test
    fun regularSignIn() {
        performSignInTest("${System.currentTimeMillis()}@mail.com")
    }

    @Test
    fun differentEmailCaseSignIn() {
        performSignInTest("aBcD${System.currentTimeMillis()}@mail.com")
    }

    private fun performSignInTest(email: String) {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )
        val apiProvider = ApiProviderFactory().createApiProvider(urlConfigProvider, session)

        val password = "qwe123".toCharArray()

        val (walletData, rootAccount, recoveryAccount)
                = apiProvider.getKeyStorage().createAndSaveWallet(email, password)

        System.out.println("Email is $email")
        System.out.println("Recovery seed is ${recoveryAccount.secretSeed!!.joinToString("")}")

        // Create user
        ApiProviderFactory().createApiProvider(urlConfigProvider, rootAccount)
                .getSignedApi()!!
                .users
                .create(walletData.attributes!!.accountId)
                .execute()

        val repositoryProvider = RepositoryProviderImpl(apiProvider, session)

        val useCase = SignInUseCase(
                email,
                password,
                apiProvider.getKeyStorage(),
                session,
                null,
                PostSignInManager(repositoryProvider)
        )

        useCase.perform().blockingAwait()

        Assert.assertEquals(walletData.attributes!!.accountId, session.getWalletInfo()!!.accountId)
        Assert.assertArrayEquals(rootAccount.secretSeed, session.getAccount()?.secretSeed)

        checkRepositories(repositoryProvider)
    }

    private fun checkRepositories(repositoryProvider: RepositoryProvider) {
        Assert.assertTrue(repositoryProvider.balances().isFresh)
        Assert.assertTrue(repositoryProvider.tfaFactors().isFresh)
        Assert.assertTrue(repositoryProvider.account().isFresh)
    }
}