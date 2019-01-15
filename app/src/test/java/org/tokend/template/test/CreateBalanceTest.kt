package org.tokend.template.test

import org.junit.Assert
import org.junit.Test
import org.tokend.sdk.api.assets.model.AssetDetails
import org.tokend.sdk.factory.GsonFactory
import org.tokend.template.di.providers.*
import org.tokend.template.features.assets.logic.CreateBalanceUseCase
import org.tokend.template.logic.Session
import org.tokend.template.logic.transactions.TxManager
import org.tokend.wallet.Account
import org.tokend.wallet.PublicKeyFactory
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.AssetCreationRequest
import org.tokend.wallet.xdr.ManageAssetOp
import org.tokend.wallet.xdr.Operation
import java.math.BigDecimal

class CreateBalanceTest {
    @Test
    fun createBalance() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )

        val email = "${System.currentTimeMillis()}@mail.com"
        val password = "qwe123".toCharArray()

        val apiProvider =
                ApiProviderFactory().createApiProvider(urlConfigProvider, session)
        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider)

        val (walletData, rootAccount, _) = Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        val txManager = TxManager(apiProvider)

        val assetCode = Util.createAsset(Account.fromSecretSeed(Config.ADMIN_SEED), apiProvider,
                txManager, session)

        val useCase = CreateBalanceUseCase(
                assetCode,
                repositoryProvider.balances(),
                repositoryProvider.systemInfo(),
                session,
                txManager
        )

        useCase.perform().blockingAwait()

        Assert.assertTrue(repositoryProvider.balances().itemsList
                .any {
                    it.assetCode == assetCode
                }
        )
    }
}