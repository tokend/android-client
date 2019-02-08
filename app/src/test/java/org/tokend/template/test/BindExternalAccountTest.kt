package org.tokend.template.test

import junit.framework.Assert
import org.junit.Test
import org.tokend.template.di.providers.*
import org.tokend.template.features.assets.logic.CreateBalanceUseCase
import org.tokend.template.features.assets.model.AssetRecord
import org.tokend.template.features.deposit.BindExternalAccountUseCase
import org.tokend.template.logic.Session
import org.tokend.template.logic.transactions.TxManager
import org.tokend.wallet.Account
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.CreateExternalSystemAccountIdPoolEntryActionInput
import org.tokend.wallet.xdr.ManageExternalSystemAccountIdPoolEntryOp
import org.tokend.wallet.xdr.Operation

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
        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider)

        val txManager = TxManager(apiProvider)

        val (walletData, rootAccount, _) = Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        val assetCode = Util.createAsset(Account.fromSecretSeed(Config.ADMIN_SEED), apiProvider,
                txManager, "0")

        CreateBalanceUseCase(
                assetCode,
                repositoryProvider.balances(),
                repositoryProvider.systemInfo(),
                session,
                txManager
        ).perform().blockingAwait()

        val asset = repositoryProvider.balances()
                .itemsList
                .find {
                    it.asset.code == assetCode
                }
                ?.asset

        Assert.assertNotNull("Environment has no assets backed by external system", asset)
        asset!!

        addNewExternalSystemAddress(asset, repositoryProvider, TxManager(apiProvider))

        val useCase = BindExternalAccountUseCase(
                asset.code,
                asset.externalSystemType!!,
                session,
                repositoryProvider.systemInfo(),
                repositoryProvider.balances(),
                repositoryProvider.account(),
                session,
                txManager
        )

        useCase.perform().blockingAwait()

        Assert.assertFalse(repositoryProvider.account().isFresh)

        repositoryProvider.account().updateDeferred().blockingAwait()

        val externalAccounts = repositoryProvider.account().item?.depositAccounts
        val externalAccount = externalAccounts?.find {
            it.type == asset.externalSystemType
        }

        Assert.assertNotNull(externalAccount)
    }

    private fun addNewExternalSystemAddress(asset: AssetRecord,
                                            repositoryProvider: RepositoryProvider,
                                            txManager: TxManager) {
        val netParams = repositoryProvider.systemInfo().getNetworkParams().blockingGet()

        val op = ManageExternalSystemAccountIdPoolEntryOp(
                ManageExternalSystemAccountIdPoolEntryOp
                        .ManageExternalSystemAccountIdPoolEntryOpActionInput.Create(
                        CreateExternalSystemAccountIdPoolEntryActionInput(
                                asset.externalSystemType!!,
                                "testaddr${System.currentTimeMillis()}",
                                0,
                                CreateExternalSystemAccountIdPoolEntryActionInput
                                        .CreateExternalSystemAccountIdPoolEntryActionInputExt
                                        .EmptyVersion()
                        )
                ),
                ManageExternalSystemAccountIdPoolEntryOp
                        .ManageExternalSystemAccountIdPoolEntryOpExt
                        .EmptyVersion()
        )

        val sourceAccount = Account.fromSecretSeed(Config.ADMIN_SEED)

        val tx = TransactionBuilder(netParams, sourceAccount.accountId)
                .addOperation(Operation.OperationBody.ManageExternalSystemAccountIdPoolEntry(op))
                .build()

        tx.addSignature(sourceAccount)

        txManager.submit(tx).blockingGet()
    }
}