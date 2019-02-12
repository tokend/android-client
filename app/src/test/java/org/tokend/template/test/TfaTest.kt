package org.tokend.template.test

import com.marcelkliemannel.kotlinonetimepassword.GoogleAuthenticator
import io.reactivex.Completable
import junit.framework.Assert
import org.junit.Test
import org.tokend.sdk.api.tfa.model.TfaFactor
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.sdk.tfa.NeedTfaException
import org.tokend.sdk.tfa.PasswordTfaOtpGenerator
import org.tokend.sdk.tfa.TfaCallback
import org.tokend.sdk.tfa.TfaVerifier
import org.tokend.template.di.providers.AccountProviderFactory
import org.tokend.template.di.providers.ApiProviderFactory
import org.tokend.template.di.providers.RepositoryProviderImpl
import org.tokend.template.di.providers.WalletInfoProviderFactory
import org.tokend.template.features.tfa.logic.DisableTfaUseCase
import org.tokend.template.features.tfa.logic.EnableTfaUseCase
import org.tokend.template.features.tfa.model.TfaFactorCreationResult
import org.tokend.template.logic.Session
import org.tokend.template.util.confirmation.ConfirmationProvider
import java.util.*

class TfaTest {
    @Test
    fun enableTfa() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )

        val email = "${System.currentTimeMillis()}@mail.com"
        val password = "qwe123".toCharArray()

        var addedFactorId = 0L
        var authenticator: GoogleAuthenticator? = null

        val tfaCallback = object : TfaCallback {
            override fun onTfaRequired(exception: NeedTfaException,
                                       verifierInterface: TfaVerifier.Interface) {
                when (exception.factorType) {
                    TfaFactor.Type.PASSWORD ->
                        verifierInterface.verify(
                                PasswordTfaOtpGenerator()
                                        .generate(exception, email, password)
                        )
                    TfaFactor.Type.TOTP -> {
                        verifierInterface.verify(
                                authenticator!!.generate(Date()),
                                onError = {
                                    verifierInterface.cancelVerification()
                                }
                        )
                    }
                    else ->
                        Assert.fail("Unknown 2FA type: ${exception.factorType}")
                }
            }
        }

        val apiProvider =
                ApiProviderFactory().createApiProvider(urlConfigProvider, session, tfaCallback)
        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider,
                JsonApiToolsProvider.getObjectMapper())

        Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        val confirmation = object : ConfirmationProvider<TfaFactorCreationResult> {
            override fun requestConfirmation(payload: TfaFactorCreationResult): Completable {
                Assert.assertEquals(TfaFactor.Type.TOTP, payload.newFactor.type)
                authenticator = GoogleAuthenticator(payload.confirmationAttributes["secret"].toString())
                addedFactorId = payload.newFactor.id
                return Completable.complete()
            }
        }
        val useCase = EnableTfaUseCase(
                TfaFactor.Type.TOTP,
                repositoryProvider.tfaFactors(),
                confirmation
        )

        useCase.perform().blockingAwait()

        Assert.assertTrue(repositoryProvider.tfaFactors().itemsList.any {
            it.id == addedFactorId && it.priority > 0
        })
    }

    @Test
    fun disableTfa() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )

        val email = "${System.currentTimeMillis()}@mail.com"
        val password = "qwe123".toCharArray()

        var authenticator: GoogleAuthenticator? = null

        val tfaCallback = object : TfaCallback {
            override fun onTfaRequired(exception: NeedTfaException,
                                       verifierInterface: TfaVerifier.Interface) {
                when (exception.factorType) {
                    TfaFactor.Type.PASSWORD ->
                        verifierInterface.verify(
                                PasswordTfaOtpGenerator()
                                        .generate(exception, email, password)
                        )
                    TfaFactor.Type.TOTP -> {
                        verifierInterface.verify(
                                authenticator!!.generate(Date()),
                                onError = {
                                    verifierInterface.cancelVerification()
                                }
                        )
                    }
                    else ->
                        Assert.fail("Unknown 2FA type: ${exception.factorType}")
                }
            }
        }

        val apiProvider =
                ApiProviderFactory().createApiProvider(urlConfigProvider, session, tfaCallback)
        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider,
                JsonApiToolsProvider.getObjectMapper())

        Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        val confirmation =  object : ConfirmationProvider<TfaFactorCreationResult> {
            override fun requestConfirmation(payload: TfaFactorCreationResult): Completable {
                authenticator = GoogleAuthenticator(payload.confirmationAttributes["secret"].toString())
                return Completable.complete()
            }
        }
        EnableTfaUseCase(
                TfaFactor.Type.TOTP,
                repositoryProvider.tfaFactors(),
                confirmation
        ).perform().blockingAwait()

        val useCase = DisableTfaUseCase(
                TfaFactor.Type.TOTP,
                repositoryProvider.tfaFactors()
        )

        useCase.perform().blockingAwait()

        Assert.assertFalse(repositoryProvider.tfaFactors().itemsList.any {
            it.type == TfaFactor.Type.TOTP && it.priority > 0
        })
    }
}