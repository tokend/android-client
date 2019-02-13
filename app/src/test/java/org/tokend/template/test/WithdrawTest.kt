package org.tokend.template.test

import junit.framework.Assert
import org.junit.Test
import org.tokend.sdk.api.assets.model.SimpleAsset
import org.tokend.sdk.factory.GsonFactory
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.template.data.model.history.details.WithdrawalDetails
import org.tokend.template.di.providers.*
import org.tokend.template.features.withdraw.logic.ConfirmWithdrawalRequestUseCase
import org.tokend.template.features.withdraw.logic.CreateWithdrawalRequestUseCase
import org.tokend.template.logic.FeeManager
import org.tokend.template.logic.Session
import org.tokend.template.logic.transactions.TxManager
import org.tokend.wallet.Account
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.*
import java.math.BigDecimal

class WithdrawTest {
    private val asset = "ETH"
    private val amount = BigDecimal.TEN
    private val destAddress = "0x6a7a43be33ba930fe58f34e07d0ad6ba7adb9b1f"

    @Test
    fun createWithdrawal() {
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

        val useCase = CreateWithdrawalRequestUseCase(
                amount,
                asset,
                destAddress,
                session,
                repositoryProvider.balances(),
                FeeManager(apiProvider)
        )

        val request = useCase.perform().blockingGet()

        Assert.assertEquals("Withdrawal request amount must be equal to the requested one",
                0, amount.compareTo(request.amount))
        Assert.assertEquals("Withdrawal request asset must be equal to the requested one",
                asset, request.asset)
        Assert.assertEquals("Withdrawal request destination address must be equal to the requested one",
                destAddress, request.destinationAddress)
        Assert.assertEquals("Withdrawal request account ID must be equal to the actual one",
                session.getWalletInfo()!!.accountId, request.accountId)
        Assert.assertEquals("Withdrawal request fee must have a valid type",
                FeeType.WITHDRAWAL_FEE.value, request.fee.feeType)
    }

    @Test
    fun confirmWithdrawal() {
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

        Util.getSomeMoney(asset, amount * BigDecimal("2"),
                repositoryProvider, session, TxManager(apiProvider))

        val assetDetails = apiProvider.getSignedApi()?.assets?.getByCode(asset)?.execute()?.get()!!
        makeAssetWithdrawable(assetDetails, repositoryProvider, TxManager(apiProvider))

        val request = CreateWithdrawalRequestUseCase(
                amount,
                asset,
                destAddress,
                session,
                repositoryProvider.balances(),
                FeeManager(apiProvider)
        ).perform().blockingGet()

        val useCase = ConfirmWithdrawalRequestUseCase(
                request,
                session,
                repositoryProvider,
                TxManager(apiProvider)
        )

        useCase.perform().blockingAwait()

        Assert.assertFalse("Balances repository must be invalidated after withdrawal sending",
                repositoryProvider.balances().isFresh)

        Thread.sleep(500)

        val historyRepository = repositoryProvider.balanceChanges(request.balanceId)
        historyRepository.updateIfNotFreshDeferred().blockingAwait()
        val transactions = historyRepository.itemsList

        Assert.assertTrue("History must not be empty after withdrawal sending",
                transactions.isNotEmpty())
        Assert.assertTrue("First history entry must be a withdrawal after withdrawal sending",
                transactions.first().details is WithdrawalDetails)
        Assert.assertEquals("Withdrawal history entry must have a requested destination address",
                destAddress,
                transactions
                        .first()
                        .details
                        .let { it as WithdrawalDetails }
                        .destinationAddress
        )
    }

    private fun makeAssetWithdrawable(asset: SimpleAsset,
                                      repositoryProvider: RepositoryProvider,
                                      txManager: TxManager) {
        val netParams = repositoryProvider.systemInfo().getNetworkParams().blockingGet()

        val req =
                ManageAssetOp.ManageAssetOpRequest.CreateAssetUpdateRequest(
                        ManageAssetOp.ManageAssetOpRequest.ManageAssetOpCreateAssetUpdateRequest(
                                AssetUpdateRequest(
                                        asset.code,
                                        GsonFactory().getBaseGson().toJson(asset.details),
                                        asset.policy or AssetPolicy.WITHDRAWABLE.value,
                                        0,
                                        AssetUpdateRequest.AssetUpdateRequestExt.EmptyVersion()
                                ),
                                0,
                                ManageAssetOp.ManageAssetOpRequest
                                        .ManageAssetOpCreateAssetUpdateRequest
                                        .ManageAssetOpCreateAssetUpdateRequestExt
                                        .EmptyVersion())
                )

        val op = ManageAssetOp(
                0L,
                req,
                ManageAssetOp.ManageAssetOpExt.EmptyVersion()
        )

        val sourceAccount = Account.fromSecretSeed(Config.ADMIN_SEED)

        val tx = TransactionBuilder(netParams, sourceAccount.accountId)
                .addOperation(Operation.OperationBody.ManageAsset(op))
                .build()

        tx.addSignature(sourceAccount)

        txManager.submit(tx).blockingGet()
    }

    @Test
    fun withdrawWithFee() {
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

        val (_, rootAccount, _) = Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        val initialBalance = Util.getSomeMoney(asset, amount * BigDecimal("2"),
                repositoryProvider, session, TxManager(apiProvider))

        val assetDetails = apiProvider.getSignedApi()?.assets?.getByCode(asset)?.execute()?.get()!!
        makeAssetWithdrawable(assetDetails, repositoryProvider, TxManager(apiProvider))

        val txManager = TxManager(apiProvider)

        val feeType = FeeType.WITHDRAWAL_FEE

        val result = Util.addFeeForAccount(
                rootAccount,
                repositoryProvider,
                txManager,
                feeType,
                asset = asset
        )

        Assert.assertTrue("Withdrawal fee must be set", result)

        val request = CreateWithdrawalRequestUseCase(
                amount,
                asset,
                destAddress,
                session,
                repositoryProvider.balances(),
                FeeManager(apiProvider)
        ).perform().blockingGet()

        Assert.assertTrue("Withdrawal request fee must be greater than zero",
                request.fee.total > BigDecimal.ZERO)

        ConfirmWithdrawalRequestUseCase(
                request,
                session,
                repositoryProvider,
                TxManager(apiProvider)
        ).perform().blockingAwait()

        Thread.sleep(500)

        repositoryProvider.balances().updateIfNotFreshDeferred().blockingAwait()

        val currentBalance = repositoryProvider.balances().itemsList
                .find { it.assetCode == asset }!!.available

        val expected = initialBalance - amount - request.fee.total

        Assert.assertEquals("Result balance must be lower than the initial one by withdrawal amount and fee",
                expected, currentBalance)
    }
}