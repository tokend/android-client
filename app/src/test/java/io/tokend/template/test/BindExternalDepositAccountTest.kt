package io.tokend.template.test

import io.tokend.template.features.assets.logic.CreateBalanceUseCase
import io.tokend.template.features.assets.model.AssetRecord
import io.tokend.template.features.deposit.logic.BindExternalSystemDepositAccountUseCase
import io.tokend.template.logic.session.Session
import io.tokend.template.logic.TxManager
import io.tokend.template.logic.providers.*
import org.junit.Assert
import org.junit.Test
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.CreateExternalSystemAccountIdPoolEntryActionInput
import org.tokend.wallet.xdr.ManageExternalSystemAccountIdPoolEntryOp
import org.tokend.wallet.xdr.Operation

class BindExternalDepositAccountTest {
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
        val repositoryProvider = RepositoryProviderImpl(
            apiProvider, session, urlConfigProvider,
            JsonApiToolsProvider.getObjectMapper()
        )

        val txManager = TxManager(apiProvider)

        val (walletData, rootAccount) = Util.getVerifiedWallet(
            email, password, apiProvider, session, repositoryProvider
        )

        val assetCode = Util.createAsset(
            apiProvider,
            txManager, EXTERNAL_SYSTEM_TYPE
        )

        CreateBalanceUseCase(
            assetCode,
            repositoryProvider.balances,
            repositoryProvider.systemInfo,
            session,
            txManager
        ).perform().blockingAwait()

        val asset = repositoryProvider.balances
            .itemsList
            .find {
                it.asset.code == assetCode
            }
            ?.asset

        Assert.assertNotNull("Environment has no assets backed by external system", asset)
        asset!!

        addNewExternalSystemAddress(asset, repositoryProvider, TxManager(apiProvider))

        val useCase = BindExternalSystemDepositAccountUseCase(
            asset = asset.code,
            externalSystemType = asset.externalSystemType!!,
            walletInfoProvider = session,
            systemInfoRepository = repositoryProvider.systemInfo,
            balancesRepository = repositoryProvider.balances,
            accountRepository = repositoryProvider.account,
            accountProvider = session,
            txManager = txManager
        )

        useCase.perform().blockingAwait()

        var externalAccount = repositoryProvider.account.item
            ?.getDepositAccount(asset)

        Assert.assertNotNull(
            "Deposit accounts must contain a newly created account of ${asset.externalSystemType} type",
            externalAccount
        )

        Thread.sleep(5000)

        repositoryProvider.account.updateDeferred().blockingAwait()

        externalAccount = repositoryProvider.account.item
            ?.getDepositAccount(asset)

        Assert.assertNotNull(
            "Newly created account must be listed in Horizon",
            externalAccount
        )
    }

    private fun addNewExternalSystemAddress(
        asset: AssetRecord,
        repositoryProvider: RepositoryProvider,
        txManager: TxManager
    ) {
        val netParams = repositoryProvider.systemInfo.getNetworkParams().blockingGet()

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