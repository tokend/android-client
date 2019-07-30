package org.tokend.template.test

import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class RecoveryTest {
    @Test
    fun aRecoveryOfConfirmed() {
        Assert.fail("No new test for KYC recovery")
        /*
        val urlConfigProvider = Util.getUrlConfigProvider()
        val accountProvider = AccountProviderFactory().createAccountProvider()
        val apiProvider = ApiProviderFactory().createApiProvider(urlConfigProvider, accountProvider)

        val email = Util.getEmail()
        val password = Config.DEFAULT_PASSWORD
        val newPassword = "qwerty".toCharArray()

        val (walletData, rootAccount, recoveryAccount)
                = apiProvider.getKeyServer().createAndSaveWallet(email, password).execute().get()
        accountProvider.setAccount(rootAccount)

        val recoverySeed = recoveryAccount.secretSeed!!

        System.out.println("Email is $email")
        System.out.println("Recovery seed is ${recoverySeed.joinToString("")}")

        val useCase = RecoverPasswordUseCase(
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
        */
    }
}