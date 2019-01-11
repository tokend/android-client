package org.tokend.template.test

import junit.framework.Assert
import org.junit.Test
import org.tokend.sdk.api.base.model.operations.WithdrawalOperation
import org.tokend.sdk.factory.GsonFactory
import org.tokend.template.di.providers.*
import org.tokend.template.extensions.Asset
import org.tokend.template.extensions.toSingle
import org.tokend.template.features.assets.model.AssetRecord
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

        val email = "${System.currentTimeMillis()}@mail.com"
        val password = "qwe123".toCharArray()

        val apiProvider =
                ApiProviderFactory().createApiProvider(urlConfigProvider, session)
        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider)

        Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        val useCase = CreateWithdrawalRequestUseCase(
                amount,
                asset,
                destAddress,
                session,
                FeeManager(apiProvider)
        )

        val request = useCase.perform().blockingGet()

        Assert.assertEquals(0, amount.compareTo(request.amount))
        Assert.assertEquals(asset, request.asset)
        Assert.assertEquals(destAddress, request.destinationAddress)
        Assert.assertEquals(session.getWalletInfo()!!.accountId, request.accountId)
        Assert.assertEquals(FeeType.WITHDRAWAL_FEE.value, request.fee.feeType)
    }

    @Test
    fun confirmWithdrawal() {
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

        Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        Util.getSomeMoney(asset, amount * BigDecimal("2"),
                repositoryProvider, TxManager(apiProvider))

        val assetDetails = apiProvider.getSignedApi()?.assets?.getByCode(asset)?.execute()?.get()!!
        makeAssetWithdrawable(assetDetails, repositoryProvider, TxManager(apiProvider))

        val request = CreateWithdrawalRequestUseCase(
                amount,
                asset,
                destAddress,
                session,
                FeeManager(apiProvider)
        ).perform().blockingGet()

        val useCase = ConfirmWithdrawalRequestUseCase(
                request,
                session,
                repositoryProvider,
                TxManager(apiProvider)
        )

        useCase.perform().blockingAwait()

        Assert.assertFalse(repositoryProvider.balances().isFresh)

        Thread.sleep(500)

        val txRepository = repositoryProvider.transactions(asset)
        txRepository.updateIfNotFreshDeferred().blockingAwait()
        val transactions = txRepository.itemsList

        Assert.assertTrue(transactions.isNotEmpty())
        Assert.assertTrue(transactions.first() is WithdrawalOperation)
        Assert.assertEquals(destAddress,
                transactions
                        .first()
                        .let { it as WithdrawalOperation }
                        .destAddress
        )
    }

    private fun makeAssetWithdrawable(asset: Asset,
                                      repositoryProvider: RepositoryProvider,
                                      txManager: TxManager) {
        val netParams = repositoryProvider.systemInfo().getNetworkParams().blockingGet()

        val req =
                ManageAssetOp.ManageAssetOpRequest.CreateAssetUpdateRequest(
                        AssetUpdateRequest(
                                asset.code,
                                GsonFactory().getBaseGson().toJson(asset.details),
                                asset.policy or AssetPolicy.WITHDRAWABLE.value,
                                AssetUpdateRequest.AssetUpdateRequestExt.EmptyVersion()
                        )
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

        val email = "${System.currentTimeMillis()}@mail.com"
        val password = "qwe123".toCharArray()

        val apiProvider =
                ApiProviderFactory().createApiProvider(urlConfigProvider, session)
        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider)

        val (_, rootAccount, _) = Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        val initialBalance = Util.getSomeMoney(asset, amount * BigDecimal("2"),
                repositoryProvider, TxManager(apiProvider))

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

        Assert.assertTrue(result)

        val request = CreateWithdrawalRequestUseCase(
                amount,
                asset,
                destAddress,
                session,
                FeeManager(apiProvider)
        ).perform().blockingGet()

        Assert.assertTrue(request.fee.total > BigDecimal.ZERO)

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

        Assert.assertEquals(expected, currentBalance)
    }
}