package io.tokend.template.test

import io.tokend.template.di.providers.AccountProviderFactory
import io.tokend.template.di.providers.ApiProviderFactory
import io.tokend.template.di.providers.RepositoryProviderImpl
import io.tokend.template.di.providers.WalletInfoProviderFactory
import io.tokend.template.features.assets.logic.CreateBalanceUseCase
import io.tokend.template.logic.Session
import io.tokend.template.logic.TxManager
import org.junit.Assert
import org.junit.Test
import org.tokend.sdk.factory.JsonApiToolsProvider

class CreateBalanceTest {
    @Test
    fun createBalance() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
            WalletInfoProviderFactory().createWalletInfoProvider(),
            AccountProviderFactory().createAccountProvider()
        )

        val email = Util.getEmail()
        val password = Config.DEFAULT_PASSWORD

        val apiProvider =
            ApiProviderFactory().createApiProvider(urlConfigProvider, session)
        val repositoryProvider = RepositoryProviderImpl(
            apiProvider, session, urlConfigProvider,
            JsonApiToolsProvider.getObjectMapper()
        )

        Util.getVerifiedWallet(
            email, password, apiProvider, session, repositoryProvider
        )

        val txManager = TxManager(apiProvider)

        val assetCode = Util.createAsset(apiProvider, txManager)

        val useCase = CreateBalanceUseCase(
            assetCode,
            repositoryProvider.balances,
            repositoryProvider.systemInfo,
            session,
            txManager
        )

        useCase.perform().blockingAwait()

        Assert.assertTrue("Balances must contain a newly created balance of $assetCode asset",
            repositoryProvider.balances.itemsList
                .any {
                    it.assetCode == assetCode
                }
        )
    }
}