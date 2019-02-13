package org.tokend.template.test

import org.junit.Assert
import org.junit.Test
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.template.di.providers.AccountProviderFactory
import org.tokend.template.di.providers.ApiProviderFactory
import org.tokend.template.di.providers.RepositoryProviderImpl
import org.tokend.template.di.providers.WalletInfoProviderFactory
import org.tokend.template.features.assets.logic.CreateBalanceUseCase
import org.tokend.template.logic.Session
import org.tokend.template.logic.transactions.TxManager
import org.tokend.wallet.Account

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
        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider,
                JsonApiToolsProvider.getObjectMapper())

        Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        val txManager = TxManager(apiProvider)

        val assetCode = Util.createAsset(Account.fromSecretSeed(Config.ADMIN_SEED), apiProvider,
                txManager)

        val useCase = CreateBalanceUseCase(
                assetCode,
                repositoryProvider.balances(),
                repositoryProvider.systemInfo(),
                session,
                txManager
        )

        useCase.perform().blockingAwait()

        Assert.assertTrue("Balances must contain a newly created balance of $assetCode asset",
                repositoryProvider.balances().itemsList
                        .any {
                            it.assetCode == assetCode
                        }
        )
    }
}