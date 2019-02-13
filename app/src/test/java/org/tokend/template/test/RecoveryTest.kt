package org.tokend.template.test

import junit.framework.Assert
import org.junit.Test
import org.tokend.template.data.repository.SystemInfoRepository
import org.tokend.template.di.providers.AccountProviderFactory
import org.tokend.template.di.providers.ApiProviderFactory
import org.tokend.template.features.recovery.logic.RecoveryUseCase
import org.tokend.template.logic.wallet.WalletUpdateManager

class RecoveryTest {
    @Test
    fun recoveryOfConfirmed() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val accountProvider = AccountProviderFactory().createAccountProvider()
        val apiProvider = ApiProviderFactory().createApiProvider(urlConfigProvider, accountProvider)

        val email = "${System.currentTimeMillis()}@mail.com"
        val password = Config.DEFAULT_PASSWORD
        val newPassword = "qwerty".toCharArray()

        val (walletData, rootAccount, recoveryAccount)
                = apiProvider.getKeyServer().createAndSaveWallet(email, password).execute().get()
        accountProvider.setAccount(rootAccount)

        val recoverySeed = recoveryAccount.secretSeed!!

        System.out.println("Email is $email")
        System.out.println("Recovery seed is ${recoverySeed.joinToString("")}")

        // Create user.
        apiProvider.getSignedApi()!!
                .users
                .create(walletData.attributes!!.accountId)
                .execute()

        val useCase = RecoveryUseCase(
                email,
                recoverySeed,
                newPassword,
                WalletUpdateManager(SystemInfoRepository(apiProvider)),
                urlConfigProvider
        )

        useCase.perform().blockingAwait()

        try {
            apiProvider.getKeyServer().getWalletInfo(email, newPassword)
                    .execute().get()
        } catch (e: Exception) {
            Assert.fail("Recovered wallet must be accessible with a new password")
        }
    }

    @Test
    fun recoveryOfNonConfirmed() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val apiProvider = ApiProviderFactory().createApiProvider(urlConfigProvider)

        val email = "${System.currentTimeMillis()}@mail.com"
        val password = Config.DEFAULT_PASSWORD
        val newPassword = "qwerty".toCharArray()

        val (_, _, recoveryAccount)
                = apiProvider.getKeyServer().createAndSaveWallet(email, password)
                .execute().get()

        val recoverySeed = recoveryAccount.secretSeed!!

        System.out.println("Email is $email")
        System.out.println("Recovery seed is ${recoverySeed.joinToString("")}")

        val useCase = RecoveryUseCase(
                email,
                recoverySeed,
                newPassword,
                WalletUpdateManager(SystemInfoRepository(apiProvider)),
                urlConfigProvider
        )

        useCase.perform().blockingAwait()

        try {
            apiProvider.getKeyServer().getWalletInfo(email, newPassword)
                    .execute().get()
        } catch (e: Exception) {
            Assert.fail("Recovered wallet must be accessible with a new password")
        }
    }
}