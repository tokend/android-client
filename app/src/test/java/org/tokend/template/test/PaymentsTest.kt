package org.tokend.template.test

import org.junit.Assert
import org.junit.Test
import org.tokend.template.di.providers.*
import org.tokend.template.features.send.logic.ConfirmPaymentRequestUseCase
import org.tokend.template.features.send.logic.CreatePaymentRequestUseCase
import org.tokend.template.logic.FeeManager
import org.tokend.template.logic.Session
import org.tokend.template.logic.transactions.TxManager
import org.tokend.wallet.Account
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.*
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class PaymentsTest {
    private val asset = "BTC"
    private val emissionAmount = BigDecimal.TEN
    private val paymentAmount = BigDecimal.ONE

    private val fixedFee = BigDecimal("0.050000")
    private val percentFee = BigDecimal("0.001000")
    private val upperBound = emissionAmount
    private val lowerBound = paymentAmount

    @Test
    fun createPayment() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )

        val email = "${System.currentTimeMillis()}@mail.com"
        val password = "qwe123".toCharArray()
        val recipient = "alice@mail.com"

        val apiProvider =
                ApiProviderFactory().createApiProvider(urlConfigProvider, session)
        val repositoryProvider = RepositoryProviderImpl(apiProvider, session)

        Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        val useCase = CreatePaymentRequestUseCase(
                recipient,
                paymentAmount,
                asset,
                "Test payment",
                session,
                FeeManager(apiProvider),
                repositoryProvider.balances(),
                repositoryProvider.accountDetails()
        )

        val request = useCase.perform().blockingGet()

        Assert.assertEquals(paymentAmount, request.amount)
    }

    @Test
    fun confirmPayment() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )

        val email = "${System.currentTimeMillis()}@mail.com"
        val password = "qwe123".toCharArray()
        val recipient = "alice@mail.com"

        val apiProvider =
                ApiProviderFactory().createApiProvider(urlConfigProvider, session)
        val repositoryProvider = RepositoryProviderImpl(apiProvider, session)

        Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        val txManager = TxManager(apiProvider)

        val initialBalance = Util.getSomeMoney(asset, emissionAmount, repositoryProvider, txManager)

        val request = CreatePaymentRequestUseCase(
                recipient,
                paymentAmount,
                asset,
                "Test payment",
                session,
                FeeManager(apiProvider),
                repositoryProvider.balances(),
                repositoryProvider.accountDetails()
        ).perform().blockingGet()

        ConfirmPaymentRequestUseCase(
                request,
                session,
                repositoryProvider,
                txManager
        ).perform().blockingAwait()

        Thread.sleep(500)

        repositoryProvider.balances().updateIfNotFreshDeferred().blockingAwait()

        val currentBalance = repositoryProvider.balances().itemsList
                .find { it.asset == asset }!!.balance

        Assert.assertNotEquals(0, initialBalance.compareTo(currentBalance))
    }

    @Test
    fun paymentWithFee() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )

        val email = "${System.currentTimeMillis()}@mail.com"
        val password = "qwe123".toCharArray()
        val recipient = "alice@mail.com"

        val apiProvider =
                ApiProviderFactory().createApiProvider(urlConfigProvider, session)
        val repositoryProvider = RepositoryProviderImpl(apiProvider, session)

        val (_, rootAccount, _) = Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        val txManager = TxManager(apiProvider)

        val result = addFeeForAccount(rootAccount, repositoryProvider, txManager)

        Assert.assertTrue(result)

        val initialBalance = Util.getSomeMoney(asset, emissionAmount, repositoryProvider, txManager)

        val useCase = CreatePaymentRequestUseCase(
                recipient,
                paymentAmount,
                asset,
                "Test payment with fee",
                session,
                FeeManager(apiProvider),
                repositoryProvider.balances(),
                repositoryProvider.accountDetails()
        )

        val request = useCase.perform().blockingGet()
        Assert.assertEquals(fixedFee, request.senderFee.fixed)

        ConfirmPaymentRequestUseCase(
                request,
                session,
                repositoryProvider,
                txManager
        ).perform().blockingAwait()

        Thread.sleep(500)

        repositoryProvider.balances().updateIfNotFreshDeferred().blockingAwait()

        val currentBalance = repositoryProvider.balances().itemsList
                .find { it.asset == asset }!!.balance

        val expected = initialBalance - paymentAmount - request.senderFee.total

        Assert.assertEquals(expected, currentBalance)
    }

    private fun addFeeForAccount(
            rootAccount: Account,
            repositoryProvider: RepositoryProvider,
            txManager: TxManager
    ): Boolean {
        val sourceAccount = Account.fromSecretSeed(Config.ADMIN_SEED)

        val netParams = repositoryProvider.systemInfo().getNetworkParams().blockingGet()

        val fixedFee = netParams.amountToPrecised(fixedFee)
        val percentFee = netParams.amountToPrecised(percentFee)
        val upperBound = netParams.amountToPrecised(upperBound)
        val lowerBound = netParams.amountToPrecised(lowerBound)

        val sourceString = "type:0asset:${asset}subtype:1accountID:${rootAccount.accountId}"
        val sha = MessageDigest.getInstance("SHA-256").digest(sourceString.toByteArray(StandardCharsets.UTF_8))

        val hash = Hash(sha)

        val feeEntry = FeeEntry(FeeType.PAYMENT_FEE, asset,
                fixedFee, percentFee, rootAccount.xdrPublicKey, null, 1, lowerBound, upperBound, hash,
                FeeEntry.FeeEntryExt.EmptyVersion())

        val feeOp = SetFeesOp(feeEntry, false, SetFeesOp.SetFeesOpExt.EmptyVersion())

        val op = Operation.OperationBody.SetFees(feeOp)

        val tx = TransactionBuilder(netParams, sourceAccount.accountId)
                .addOperation(op)
                .build()

        tx.addSignature(sourceAccount)

        val response = txManager.submit(tx).blockingGet()

        return response.isSuccess
    }
}