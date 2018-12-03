package org.tokend.template.test

import junit.framework.Assert
import org.junit.Test
import org.tokend.sdk.api.favorites.model.AssetPairFavoriteEntry
import org.tokend.template.di.providers.AccountProviderFactory
import org.tokend.template.di.providers.ApiProviderFactory
import org.tokend.template.di.providers.RepositoryProviderImpl
import org.tokend.template.di.providers.WalletInfoProviderFactory
import org.tokend.template.features.invest.logic.SwitchFavoriteUseCase
import org.tokend.template.logic.Session

class SwitchingFavoriteStateTest {

    @Test
    fun switchState() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )
        val apiProvider = ApiProviderFactory().createApiProvider(urlConfigProvider, session)

        val email = "${System.currentTimeMillis()}@mail.com"
        val password = "qwe123".toCharArray()

        val repositoryProvider = RepositoryProviderImpl(apiProvider, session)

        Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        val favoritesRepository = repositoryProvider.favorites()

        val favoriteEntry = AssetPairFavoriteEntry("ETH", "BTC")

        val useCase = SwitchFavoriteUseCase(favoriteEntry, favoritesRepository)

        useCase.perform().blockingAwait()

        Assert.assertTrue(favoritesRepository.itemsSubject.value.isNotEmpty())

        useCase.perform().blockingAwait()

        Assert.assertTrue(favoritesRepository.itemsSubject.value.isEmpty())
    }
}