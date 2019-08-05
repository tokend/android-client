package org.tokend.template.test

import junit.framework.Assert
import org.junit.Test
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.template.data.model.AssetRecord
import org.tokend.template.di.providers.*
import org.tokend.template.features.assets.logic.CreateBalanceUseCase
import org.tokend.template.features.deposit.BindExternalAccountUseCase
import org.tokend.template.logic.Session
import org.tokend.template.logic.transactions.TxManager
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

        val email = Util.getEmail()
        val password = Config.DEFAULT_PASSWORD

        val apiProvider =
                ApiProviderFactory().createApiProvider(urlConfigProvider, session)
        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider,
                JsonApiToolsProvider.getObjectMapper())

        val txManager = TxManager(apiProvider)

        val (walletData, rootAccount, _) = Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        val assetCode = Util.createAsset(apiProvider,
                txManager, EXTERNAL_SYSTEM_TYPE)

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

        Assert.assertFalse("Account repository must be invalidated after external account binding",
                repositoryProvider.account().isFresh)

        repositoryProvider.account().updateDeferred().blockingAwait()

        val externalAccounts = repositoryProvider.account().item?.depositAccounts
        val externalAccount = externalAccounts?.find {
            it.type == asset.externalSystemType
        }

        Assert.assertNotNull("Deposit accounts must contain a newly created account of ${asset.externalSystemType} type",
                externalAccount)
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
                                "{\"type\":\"address\",\"data\":" +
                                        "{\"address\":\"testaddr${System.currentTimeMillis()}\"}}",
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

        val sourceAccount = Config.ADMIN_ACCOUNT

        val tx = TransactionBuilder(netParams, sourceAccount.accountId)
                .addOperation(Operation.OperationBody.ManageExternalSystemAccountIdPoolEntry(op))
                .build()

        tx.addSignature(sourceAccount)

        txManager.submit(tx).blockingGet()
    }

    companion object {
        private const val EXTERNAL_SYSTEM_TYPE = "2"
    }
}