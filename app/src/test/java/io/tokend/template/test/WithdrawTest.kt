package io.tokend.template.test

import io.tokend.template.di.providers.AccountProviderFactory
import io.tokend.template.di.providers.ApiProviderFactory
import io.tokend.template.di.providers.RepositoryProviderImpl
import io.tokend.template.di.providers.WalletInfoProviderFactory
import io.tokend.template.features.fees.logic.FeeManager
import io.tokend.template.features.history.model.details.BalanceChangeCause
import io.tokend.template.features.withdraw.logic.ConfirmWithdrawalRequestUseCase
import io.tokend.template.features.withdraw.logic.CreateWithdrawalRequestUseCase
import io.tokend.template.logic.Session
import io.tokend.template.logic.TxManager
import junit.framework.Assert
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.*
import org.tokend.wallet.xdr.utils.XdrDataOutputStream
import org.tokend.wallet.xdr.utils.toXdr
import java.math.BigDecimal

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class WithdrawTest {
    private val amount = BigDecimal.TEN
    private val destAddress = "0x6a7a43be33ba930fe58f34e07d0ad6ba7adb9b1f"

    companion object {
        @BeforeClass
        @JvmStatic
        fun setKeyValueWithdrawalTasks() {
            val key = "withdrawal_tasks:*"

            val urlConfigProvider = Util.getUrlConfigProvider()
            val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
            )

            val apiProvider =
                ApiProviderFactory().createApiProvider(urlConfigProvider, session)

            val op = ManageKeyValueOp(
                key = key,
                action = ManageKeyValueOp.ManageKeyValueOpAction.Put(
                    object : KeyValueEntryValue(KeyValueEntryType.UINT32) {
                        override fun toXdr(stream: XdrDataOutputStream) {
                            super.toXdr(stream)
                            1.toXdr(stream)
                        }
                    }
                ),
                ext = ManageKeyValueOp.ManageKeyValueOpExt.EmptyVersion()
            )

            val sourceAccount = Config.ADMIN_ACCOUNT

            val systemInfo =
                apiProvider.getApi()
                    .general
                    .getSystemInfo()
                    .execute()
                    .get()
            val netParams = systemInfo.toNetworkParams()

            val tx = TransactionBuilder(netParams, sourceAccount.accountId)
                .addOperation(Operation.OperationBody.ManageKeyValue(op))
                .build()

            tx.addSignature(sourceAccount)

            TxManager(apiProvider).submit(tx).blockingGet()
        }
    }

    @Test
    fun aCreateWithdrawal() {
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

        val asset = Util.createAsset(apiProvider, txManager)

        Util.getSomeMoney(asset, BigDecimal.ONE, repositoryProvider, session, txManager)

        val balance = repositoryProvider.balances.itemsList.first { it.assetCode == asset }

        val useCase = CreateWithdrawalRequestUseCase(
            amount,
            balance,
            destAddress,
            session,
            FeeManager(apiProvider)
        )

        val request = useCase.perform().blockingGet()

        Assert.assertEquals(
            "Withdrawal request amount must be equal to the requested one",
            0, amount.compareTo(request.amount)
        )
        Assert.assertEquals(
            "Withdrawal request asset must be equal to the requested one",
            asset, request.asset.code
        )
        Assert.assertEquals(
            "Withdrawal request destination address must be equal to the requested one",
            destAddress, request.destinationAddress
        )
        Assert.assertEquals(
            "Withdrawal request account ID must be equal to the actual one",
            session.getWalletInfo()!!.accountId, request.accountId
        )
    }

    @Test
    fun bConfirmWithdrawal() {
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

        val asset = Util.createAsset(apiProvider, txManager)

        val initialBalance = Util.getSomeMoney(
            asset, amount * BigDecimal("2"),
            repositoryProvider, session, TxManager(apiProvider)
        )

        Util.makeAccountGeneral(session, apiProvider, repositoryProvider.systemInfo, txManager)

        val balance = repositoryProvider.balances.itemsList.first { it.assetCode == asset }

        val request = CreateWithdrawalRequestUseCase(
            amount,
            balance,
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

        val expectedBalance = initialBalance - amount
        val currentBalance = repositoryProvider.balances.itemsList
            .find { it.assetCode == asset }!!.available

        Assert.assertEquals(
            "Result balance must be lower than the initial one by withdrawal amount",
            0, currentBalance.compareTo(expectedBalance)
        )

        Thread.sleep(2000)

        val historyRepository = repositoryProvider.balanceChanges(request.balanceId)
        historyRepository.updateIfNotFreshDeferred().blockingAwait()
        val transactions = historyRepository.itemsList

        Assert.assertTrue(
            "History must not be empty after withdrawal sending",
            transactions.isNotEmpty()
        )
        Assert.assertTrue(
            "First history entry must be a withdrawal after withdrawal sending",
            transactions.first().cause is BalanceChangeCause.WithdrawalRequest
        )
        Assert.assertEquals("Withdrawal history entry must have a requested destination address",
            destAddress,
            transactions
                .first()
                .cause
                .let { it as BalanceChangeCause.WithdrawalRequest }
                .destinationAddress
        )
    }

    @Test
    fun cWithdrawWithFee() {
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

        val (_, rootAccount) = Util.getVerifiedWallet(
            email, password, apiProvider, session, repositoryProvider
        )

        val txManager = TxManager(apiProvider)

        val asset = Util.createAsset(apiProvider, txManager)

        val initialBalance = Util.getSomeMoney(
            asset, amount * BigDecimal("2"),
            repositoryProvider, session, txManager
        )

        val feeType = FeeType.WITHDRAWAL_FEE

        val result = Util.addFeeForAccount(
            rootAccount.accountId,
            apiProvider,
            txManager,
            feeType,
            asset = asset
        )

        Assert.assertTrue("Withdrawal fee must be set", result)

        Util.makeAccountGeneral(session, apiProvider, repositoryProvider.systemInfo, txManager)

        val balance = repositoryProvider.balances.itemsList.first { it.assetCode == asset }

        val request = CreateWithdrawalRequestUseCase(
            amount,
            balance,
            destAddress,
            session,
            FeeManager(apiProvider)
        ).perform().blockingGet()

        Assert.assertTrue(
            "Withdrawal request fee must be greater than zero",
            request.fee.total > BigDecimal.ZERO
        )

        ConfirmWithdrawalRequestUseCase(
            request,
            session,
            repositoryProvider,
            TxManager(apiProvider)
        ).perform().blockingAwait()

        Thread.sleep(2000)

        repositoryProvider.balances.updateIfNotFreshDeferred().blockingAwait()

        val currentBalance = repositoryProvider.balances.itemsList
            .find { it.assetCode == asset }!!.available

        val expected = initialBalance - amount - request.fee.total

        Assert.assertEquals(
            "Result balance must be lower than the initial one by withdrawal amount and fee",
            0, expected.compareTo(currentBalance)
        )
    }
}