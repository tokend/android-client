package org.tokend.template.test

import junit.framework.Assert
import org.junit.Test
import org.tokend.template.di.providers.AccountProviderFactory
import org.tokend.template.di.providers.ApiProviderFactory
import org.tokend.template.di.providers.RepositoryProviderImpl
import org.tokend.template.di.providers.WalletInfoProviderFactory
import org.tokend.template.features.deposit.BindExternalAccountUseCase
import org.tokend.template.logic.Session
import org.tokend.template.logic.transactions.TxManager

class BindExternalAccountTest {
    @Test
    fun bindExternalSystemAccount() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )

        val email = "${System.currentTimeMillis()}@mail.com"
        val password = "qwe123".toCharArray()

        val apiProvider =
                ApiProviderFactory().createApiProvider(urlConfigProvider, session)
        val repositoryProvider = RepositoryProviderImpl(apiProvider, session)

        val (walletData, rootAccount, _) = Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        val asset = repositoryProvider.balances()
                .itemsList
                .find {
                    it.assetDetails!!.isBackedByExternalSystem
                }
                ?.assetDetails

        Assert.assertNotNull("Environment has no assets backed by external system", asset)
        asset!!

        val useCase = BindExternalAccountUseCase(
                asset.code,
                asset.details.externalSystemType!!,
                session,
                repositoryProvider.systemInfo(),
                repositoryProvider.balances(),
                repositoryProvider.account(),
                session,
                TxManager(apiProvider)
        )

        useCase.perform().blockingAwait()

        Assert.assertFalse(repositoryProvider.account().isFresh)

        repositoryProvider.account().updateDeferred().blockingAwait()

        val externalAccounts = repositoryProvider.account().item?.externalAccounts
        val externalAccount = externalAccounts?.find {
            it.type.value == asset.details.externalSystemType
        }

        Assert.assertNotNull(externalAccount)
    }
}