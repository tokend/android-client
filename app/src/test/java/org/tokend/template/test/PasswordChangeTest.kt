package org.tokend.template.test

import org.junit.Assert
import org.junit.Test
import org.tokend.sdk.api.tfa.model.TfaFactor
import org.tokend.sdk.tfa.NeedTfaException
import org.tokend.sdk.tfa.PasswordTfaOtpGenerator
import org.tokend.sdk.tfa.TfaCallback
import org.tokend.sdk.tfa.TfaVerifier
import org.tokend.template.di.providers.AccountProviderFactory
import org.tokend.template.di.providers.ApiProviderFactory
import org.tokend.template.di.providers.RepositoryProviderImpl
import org.tokend.template.di.providers.WalletInfoProviderFactory
import org.tokend.template.features.changepassword.ChangePasswordUseCase
import org.tokend.template.features.signin.logic.PostSignInManager
import org.tokend.template.features.signin.logic.SignInUseCase
import org.tokend.template.logic.Session
import org.tokend.template.logic.wallet.WalletUpdateManager

class PasswordChangeTest {
    @Test
    fun changePassword() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )

        val email = "${System.currentTimeMillis()}@mail.com"
        val password = "qwe123".toCharArray()
        val newPassword = "qwerty".toCharArray()

        val tfaCallback = object : TfaCallback {
            override fun onTfaRequired(exception: NeedTfaException,
                                       verifierInterface: TfaVerifier.Interface) {
                Assert.assertEquals(TfaFactor.Type.PASSWORD, exception.factorType)
                verifierInterface.verify(PasswordTfaOtpGenerator().generate(
                        exception, email, password
                ))
            }
        }

        val apiProvider = ApiProviderFactory().createApiProvider(urlConfigProvider, session,
                tfaCallback)

        val (originalWalletData, rootAccount, recoveryAccount)
                = apiProvider.getKeyStorage().createAndSaveWallet(email, password)

        System.out.println("Email is $email")
        System.out.println("Recovery seed is ${recoveryAccount.secretSeed!!.joinToString("")}")

        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider)

        SignInUseCase(
                email,
                password,
                apiProvider.getKeyStorage(),
                session,
                null,
                PostSignInManager(repositoryProvider)
        ).perform().blockingAwait()

        val useCase = ChangePasswordUseCase(
                newPassword,
                WalletUpdateManager(repositoryProvider.systemInfo()),
                apiProvider,
                session,
                session
        )

        useCase.perform().blockingAwait()

        val signers = apiProvider.getApi()
                .accounts
                .getSigners(session.getWalletInfo()!!.accountId)
                .execute()
                .get()

        // Check that account has been updated to the actual.
        val currentAccount = session.getAccount()!!
        Assert.assertNotEquals(rootAccount.accountId, currentAccount.accountId)

        // Check that new signer has been added.
        Assert.assertTrue(signers.any { signer ->
            signer.accountId == currentAccount.accountId
        })

        // Check that old signer has been removed.
        Assert.assertFalse(signers.any { signer ->
            signer.accountId == rootAccount.accountId
        })

        // Check that wallet info has been updated to the actual.
        Assert.assertNotEquals(originalWalletData.id, session.getWalletInfo()!!.walletIdHex)

        // Check that we after all can sign in with new password.
        Assert.assertNotNull(apiProvider.getKeyStorage()
                .getWalletInfo(email, newPassword))
    }
}