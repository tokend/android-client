package org.tokend.template.test

import org.junit.Test
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.template.data.model.history.converter.DefaultParticipantEffectConverter
import org.tokend.template.data.model.history.details.IssuanceDetails
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

        val email = "1549876678839@mail.com"
        val password = "qwe123".toCharArray()

        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider,
                JsonApiToolsProvider.getObjectMapper())

        SignInUseCase(
                email,
                password,
                apiProvider.getKeyServer(),
                session,
                null,
                PostSignInManager(repositoryProvider)
        ).perform().blockingAwait()

        val balanceId = "BCLO5Y6QOJG4WC4KMJWEP5W5HUNFUC7YCYPFMWXILAD25MK6Y374XDFD"

        val converter = DefaultParticipantEffectConverter(balanceId)
        val cache = BalanceChangesCache()
        val repo = BalanceChangesRepository(balanceId, apiProvider, converter, cache)

        repo.updateDeferred().blockingAwait()

        repo.itemsList.forEach { change ->
            System.out.println("${change.action} ${change.amount} ${change.assetCode}")
        }
    }
}