package org.tokend.template.test

import junit.framework.Assert
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.template.data.model.SimpleAsset
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.di.providers.AccountProviderFactory
import org.tokend.template.di.providers.ApiProviderFactory
import org.tokend.template.di.providers.RepositoryProviderImpl
import org.tokend.template.di.providers.WalletInfoProviderFactory
import org.tokend.template.features.withdraw.logic.ConfirmWithdrawalRequestUseCase
import org.tokend.template.features.withdraw.logic.CreateWithdrawalRequestUseCase
import org.tokend.template.logic.FeeManager
import org.tokend.template.logic.Session
import org.tokend.template.logic.transactions.TxManager
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.*
import org.tokend.wallet.xdr.utils.XdrDataOutputStream
import org.tokend.wallet.xdr.utils.toXdr
import java.math.BigDecimal

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class WithdrawTest {
    private val amount = BigDecimal.TEN
    private val destAddress = "0x6a7a43be33ba930fe58f34e07d0ad6ba7adb9b1f"

    @Before
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
        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider,
                JsonApiToolsProvider.getObjectMapper())

        Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        val txManager = TxManager(apiProvider)

        val asset = Util.createAsset(apiProvider, txManager)

        Util.getSomeMoney(asset, BigDecimal.ONE, repositoryProvider, session, txManager)

        val useCase = CreateWithdrawalRequestUseCase(
                amount,
                SimpleAsset(asset),
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
        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider,
                JsonApiToolsProvider.getObjectMapper())

        Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        val txManager = TxManager(apiProvider)

        val asset = Util.createAsset(apiProvider, txManager)

        Util.getSomeMoney(asset, amount * BigDecimal("2"),
                repositoryProvider, session, TxManager(apiProvider))

        Util.makeAccountGeneral(session, apiProvider, repositoryProvider.systemInfo(), txManager)

        val request = CreateWithdrawalRequestUseCase(
                amount,
                SimpleAsset(asset),
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
                transactions.first().cause is BalanceChangeCause.WithdrawalRequest)
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
        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider,
                JsonApiToolsProvider.getObjectMapper())

        val (_, rootAccount, _) = Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        val txManager = TxManager(apiProvider)

        val asset = Util.createAsset(apiProvider, txManager)

        val initialBalance = Util.getSomeMoney(asset, amount * BigDecimal("2"),
                repositoryProvider, session, txManager)

        val feeType = FeeType.WITHDRAWAL_FEE

        val result = Util.addFeeForAccount(
                rootAccount.accountId,
                apiProvider,
                txManager,
                feeType,
                asset = asset
        )

        Assert.assertTrue("Withdrawal fee must be set", result)

        Util.makeAccountGeneral(session, apiProvider, repositoryProvider.systemInfo(), txManager)

        val request = CreateWithdrawalRequestUseCase(
                amount,
                SimpleAsset(asset),
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