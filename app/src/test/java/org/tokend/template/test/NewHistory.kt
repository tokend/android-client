package org.tokend.template.test

import org.junit.Test
import org.tokend.template.data.model.history.converter.DefaultParticipantEffectConverter
import org.tokend.template.data.repository.balancechanges.BalanceChangesCache
import org.tokend.template.data.repository.balancechanges.BalanceChangesRepository
import org.tokend.template.di.providers.AccountProviderFactory
import org.tokend.template.di.providers.ApiProviderFactory
import org.tokend.template.di.providers.RepositoryProviderImpl
import org.tokend.template.di.providers.WalletInfoProviderFactory
import org.tokend.template.features.signin.logic.PostSignInManager
import org.tokend.template.features.signin.logic.SignInUseCase
import org.tokend.template.logic.Session

class NewHistory {
    @Test
    fun newHistory() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )
        val apiProvider = ApiProviderFactory().createApiProvider(urlConfigProvider, session)

        val email = "ole21@mailinator.com"
        val password = "qwe123".toCharArray()

        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider)

        SignInUseCase(
                email,
                password,
                apiProvider.getKeyServer(),
                session,
                null,
                PostSignInManager(repositoryProvider)
        ).perform().blockingAwait()

        val balanceId = "BATISH3VMOX5YY6435MH355KIK4EEJDNZRSKZ52SJHB2FGNIOSJZON7M"

        val converter = DefaultParticipantEffectConverter(balanceId)
        val cache = BalanceChangesCache()
        val repo = BalanceChangesRepository(balanceId, apiProvider, converter, cache)

        repo.updateDeferred().blockingAwait()

        repo.itemsList.forEach { change ->
            System.out.println("${change.action} ${change.amount} ${change.assetCode}")
        }
    }
}