package org.tokend.template.test

import com.marcelkliemannel.kotlinonetimepassword.GoogleAuthenticator
import io.reactivex.Completable
import junit.framework.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
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

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TfaTest {
    private val factorType = TfaFactor.Type.TOTP

    @Test
    fun aEnableTfa() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
            WalletInfoProviderFactory().createWalletInfoProvider(),
            AccountProviderFactory().createAccountProvider()
        )

        val email = Util.getEmail()
        val password = Config.DEFAULT_PASSWORD

        var authenticator: GoogleAuthenticator? = null

        val tfaCallback = object : TfaCallback {
            override fun onTfaRequired(
                exception: NeedTfaException,
                verifierInterface: TfaVerifier.Interface
            ) {
                when (exception.factorType) {
                    TfaFactor.Type.PASSWORD ->{
                        verifierInterface.verify(
                            PasswordTfaOtpGenerator()
                                .generate(exception, session.login, password)
                        )
                    }
                    TfaFactor.Type.TOTP -> {
                        verifierInterface.verify(
                            authenticator!!.generate(Date()),
                            onError = {
                                verifierInterface.cancelVerification()
                            }
                        )
                    }
                    else ->
                        Assert.fail("Unknown TFA type: ${exception.factorType}")
                }
            }
        }

        val apiProvider =
            ApiProviderFactory().createApiProvider(urlConfigProvider, session, tfaCallback)
        val repositoryProvider = RepositoryProviderImpl(
            apiProvider, session, urlConfigProvider,
            JsonApiToolsProvider.getObjectMapper()
        )

        Util.getVerifiedWallet(
            email, password, apiProvider, session, repositoryProvider
        )

        val confirmation = object : ConfirmationProvider<TfaFactorCreationResult> {
            override fun requestConfirmation(payload: TfaFactorCreationResult): Completable {
                Assert.assertEquals(
                    "Newly created factor must be $factorType",
                    factorType, payload.newFactor.type
                )
                authenticator =
                    GoogleAuthenticator(payload.confirmationAttributes["secret"].toString())
                return Completable.complete()
            }
        }
        val useCase = EnableTfaUseCase(
            factorType,
            repositoryProvider.tfaFactors(),
            confirmation
        )

        useCase.perform().blockingAwait()

        Assert.assertTrue("TFA factors repository must contain a newly created active factor of type $factorType",
            repositoryProvider.tfaFactors().itemsList.any {
                it.type == factorType && it.priority > 0
            })
    }

    @Test
    fun bDisableTfa() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
            WalletInfoProviderFactory().createWalletInfoProvider(),
            AccountProviderFactory().createAccountProvider()
        )

        val email = Util.getEmail()
        val password = Config.DEFAULT_PASSWORD

        var authenticator: GoogleAuthenticator? = null

        val tfaCallback = object : TfaCallback {
            override fun onTfaRequired(
                exception: NeedTfaException,
                verifierInterface: TfaVerifier.Interface
            ) {
                when (exception.factorType) {
                    TfaFactor.Type.PASSWORD ->
                        verifierInterface.verify(
                            PasswordTfaOtpGenerator()
                                .generate(exception, session.login, password)
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
                        Assert.fail("Unknown TFA type: ${exception.factorType}")
                }
            }
        }

        val apiProvider =
            ApiProviderFactory().createApiProvider(urlConfigProvider, session, tfaCallback)
        val repositoryProvider = RepositoryProviderImpl(
            apiProvider, session, urlConfigProvider,
            JsonApiToolsProvider.getObjectMapper()
        )

        Util.getVerifiedWallet(
            email, password, apiProvider, session, repositoryProvider
        )

        val confirmation = object : ConfirmationProvider<TfaFactorCreationResult> {
            override fun requestConfirmation(payload: TfaFactorCreationResult): Completable {
                authenticator =
                    GoogleAuthenticator(payload.confirmationAttributes["secret"].toString())
                return Completable.complete()
            }
        }
        EnableTfaUseCase(
            factorType,
            repositoryProvider.tfaFactors(),
            confirmation
        ).perform().blockingAwait()

        val useCase = DisableTfaUseCase(
            factorType,
            repositoryProvider.tfaFactors()
        )

        useCase.perform().blockingAwait()

        Assert.assertFalse(
            "TFA factors repository must not contain an active factor of type $factorType",
            repositoryProvider.tfaFactors().itemsList.any {
                it.type == factorType && it.priority > 0
            })
    }
}