package io.tokend.template.test

import io.tokend.template.logic.providers.AccountProviderFactory
import io.tokend.template.logic.providers.ApiProviderFactory
import io.tokend.template.logic.providers.RepositoryProviderImpl
import io.tokend.template.logic.providers.WalletInfoProviderFactory
import io.tokend.template.features.changepassword.ChangePasswordUseCase
import io.tokend.template.logic.session.Session
import org.junit.Assert
import org.junit.Test
import org.tokend.sdk.api.tfa.model.TfaFactor
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.sdk.tfa.NeedTfaException
import org.tokend.sdk.tfa.PasswordTfaOtpGenerator
import org.tokend.sdk.tfa.TfaCallback
import org.tokend.sdk.tfa.TfaVerifier

class PasswordChangeTest {
    @Test
    fun changePassword() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
            WalletInfoProviderFactory().createWalletInfoProvider(),
            AccountProviderFactory().createAccountProvider()
        )

        val email = Util.getEmail()
        val password = Config.DEFAULT_PASSWORD
        val newPassword = "qwerty".toCharArray()

        val tfaCallback = object : TfaCallback {
            override fun onTfaRequired(
                exception: NeedTfaException,
                verifierInterface: TfaVerifier.Interface
            ) {
                Assert.assertEquals(TfaFactor.Type.PASSWORD, exception.factorType)
                verifierInterface.verify(
                    PasswordTfaOtpGenerator().generate(
                        exception, session.login, password
                    )
                )
            }
        }

        val apiProvider = ApiProviderFactory().createApiProvider(
            urlConfigProvider, session,
            tfaCallback
        )

        val repositoryProvider = RepositoryProviderImpl(
            apiProvider, session, urlConfigProvider,
            JsonApiToolsProvider.getObjectMapper()
        )

        val (originalWalletData, rootAccount)
                = Util.getVerifiedWallet(email, password, apiProvider, session, repositoryProvider)

        val useCase = ChangePasswordUseCase(
            newPassword,
            apiProvider,
            session,
            session,
            repositoryProvider,
            null,
            null,
            session
        )

        useCase.perform().blockingAwait()

        Thread.sleep(5000)

        val signers = apiProvider.getApi()
            .v3
            .signers
            .get(session.getWalletInfo()!!.accountId)
            .execute()
            .get()

        // Check that account has been updated to the actual.
        val currentAccount = session.getDefaultAccount()!!
        Assert.assertNotEquals(
            "Account in AccountProvider must be updated",
            rootAccount.accountId, currentAccount.accountId
        )

        // Check that new signer has been added.
        Assert.assertTrue("A new signer ${currentAccount.accountId} must be added to account signers",
            signers.any { signer ->
                signer.id == currentAccount.accountId
            })

        // Check that old signer has been removed.
        Assert.assertFalse("The old signer ${rootAccount.accountId} must be removed from account signers",
            signers.any { signer ->
                signer.id == rootAccount.accountId
            })

        // Check that wallet info has been updated to the actual.
        Assert.assertNotEquals(
            "Wallet info in WalletInfoProvider must be updated",
            originalWalletData.id, session.getWalletInfo()!!.walletId
        )

        // Check that we after all can sign in with new password.
        Assert.assertNotNull(
            "Sign in with a new password must complete",
            apiProvider.getKeyServer().getWalletInfo(email, newPassword)
        )
    }
}