package org.tokend.template.test

import org.tokend.sdk.keyserver.models.WalletCreateResult
import org.tokend.template.data.model.UrlConfig
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.di.providers.UrlConfigProvider
import org.tokend.template.di.providers.UrlConfigProviderFactory
import org.tokend.template.features.signin.logic.PostSignInManager
import org.tokend.template.features.signin.logic.SignInUseCase
import org.tokend.template.logic.Session

object Util {
    fun getUrlConfigProvider(url: String = Config.API): UrlConfigProvider {
        return UrlConfigProviderFactory().createUrlConfigProvider(
                UrlConfig(url, "", "", "")
        )
    }

    fun getVerifiedWallet(email: String,
                          password: CharArray,
                          apiProvider: ApiProvider,
                          session: Session,
                          repositoryProvider: RepositoryProvider?): WalletCreateResult {
        val createResult = apiProvider.getKeyStorage().createAndSaveWallet(email, password)

        System.out.println("Email is $email")
        System.out.println("Recovery seed is " +
                createResult.recoveryAccount.secretSeed!!.joinToString(""))

        SignInUseCase(
                email,
                password,
                apiProvider.getKeyStorage(),
                session,
                null,
                repositoryProvider?.let { PostSignInManager(it) }
        ).perform().blockingAwait()

        return createResult
    }
}