package org.tokend.template.test

import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.di.providers.AccountProviderFactory
import org.tokend.template.di.providers.ApiProviderFactory
import org.tokend.template.di.providers.RepositoryProviderImpl
import org.tokend.template.di.providers.WalletInfoProviderFactory
import org.tokend.template.features.send.logic.ConfirmPaymentRequestUseCase
import org.tokend.template.features.send.logic.CreatePaymentRequestUseCase
import org.tokend.template.logic.FeeManager
import org.tokend.template.logic.Session
import org.tokend.template.logic.transactions.TxManager
import org.tokend.wallet.Base32Check
import org.tokend.wallet.xdr.FeeType
import org.tokend.wallet.xdr.PaymentFeeType
import java.math.BigDecimal

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class PaymentsTest {
    private val emissionAmount = BigDecimal.TEN
    private val paymentAmount = BigDecimal.ONE

    @Test
    fun aCreatePayment() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )

        val email = Util.getEmail()
        val password = Config.DEFAULT_PASSWORD
        val recipientEmail = Util.getEmail()

        val apiProvider =
                ApiProviderFactory().createApiProvider(urlConfigProvider, session)
        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider,
                JsonApiToolsProvider.getObjectMapper())

        val (_, recipientAccount, _) = Util.getVerifiedWallet(
                recipientEmail, password, apiProvider, session, null
        )

        val recipientAccountId = recipientAccount.accountId

        Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        val txManager = TxManager(apiProvider)
        val asset = Util.createAsset(apiProvider, txManager)
        Util.getSomeMoney(asset, emissionAmount,
                repositoryProvider, session, txManager)

        val useCase = CreatePaymentRequestUseCase(
                recipientAccountId,
                paymentAmount,
                asset,
                "Test payment",
                session,
                FeeManager(apiProvider),
                repositoryProvider.balances(),
                repositoryProvider.accountDetails()
        )

        val request = useCase.perform().blockingGet()

        Assert.assertEquals("Payment request amount must be equal to the requested amount",
                paymentAmount, request.amount)
        Assert.assertTrue("Payment request recipient must be a valid account ID",
                Base32Check.isValid(Base32Check.VersionByte.ACCOUNT_ID, request.recipientAccountId.toCharArray()))
    }

    @Test
    fun bConfirmPayment() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )

        val email = Util.getEmail()
        val password = Config.DEFAULT_PASSWORD
        val recipientEmail = Util.getEmail()

        val apiProvider =
                ApiProviderFactory().createApiProvider(urlConfigProvider, session)
        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider,
                JsonApiToolsProvider.getObjectMapper())

        val (_, recipientAccount, _) = Util.getVerifiedWallet(
                recipientEmail, password, apiProvider, session, null
        )

        val recipientAccountId = recipientAccount.accountId

        Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        val txManager = TxManager(apiProvider)

        val asset = Util.createAsset(apiProvider, txManager)
        val initialBalance = Util.getSomeMoney(asset, emissionAmount,
                repositoryProvider, session, txManager)

        val request = CreatePaymentRequestUseCase(
                recipientAccountId,
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
                .find { it.assetCode == asset }!!.available

        Assert.assertNotEquals("Balance must be changed after the payment sending",
                0, initialBalance.compareTo(currentBalance))

        val historyRepository = repositoryProvider.balanceChanges(request.senderBalanceId)
        historyRepository.updateIfNotFreshDeferred().blockingAwait()
        val transactions = historyRepository.itemsList

        Assert.assertTrue("History must not be empty after the payment sending",
                transactions.isNotEmpty())
        Assert.assertTrue("First history entry must be a payment after the payment sending",
                transactions.first().cause is BalanceChangeCause.Payment)
        Assert.assertEquals("Payment history entry must have a requested destination account id",
                request.recipientAccountId,
                transactions
                        .first()
                        .cause
                        .let { it as BalanceChangeCause.Payment }
                        .destAccountId
        )
    }

    @Test
    fun cPaymentWithFee() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )

        val email = Util.getEmail()
        val password = Config.DEFAULT_PASSWORD
        val recipientEmail = Util.getEmail()

        val apiProvider =
                ApiProviderFactory().createApiProvider(urlConfigProvider, session)
        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider,
                JsonApiToolsProvider.getObjectMapper())

        val (_, recipientAccount, _) = Util.getVerifiedWallet(
                recipientEmail, password, apiProvider, session, null
        )

        val recipientAccountId = recipientAccount.accountId

        val (_, rootAccount, _) = Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        val txManager = TxManager(apiProvider)

        val asset = Util.createAsset(apiProvider, txManager)

        val feeType = FeeType.PAYMENT_FEE
        val feeSubType = PaymentFeeType.OUTGOING.value

        val result = Util.addFeeForAccount(
                rootAccount.accountId,
                apiProvider,
                txManager,
                feeType,
                feeSubType,
                asset
        )

        Assert.assertTrue("Fee must be added successfully", result)

        val initialBalance = Util.getSomeMoney(asset, emissionAmount,
                repositoryProvider, session, txManager)

        val request = CreatePaymentRequestUseCase(
                recipientAccountId,
                paymentAmount,
                asset,
                "Test payment with fee",
                session,
                FeeManager(apiProvider),
                repositoryProvider.balances(),
                repositoryProvider.accountDetails()
        ).perform().blockingGet()

        Assert.assertTrue("Payment request sender fee must greater than zero",
                request.senderFee.total > BigDecimal.ZERO)

        ConfirmPaymentRequestUseCase(
                request,
                session,
                repositoryProvider,
                txManager
        ).perform().blockingAwait()

        Thread.sleep(500)

        repositoryProvider.balances().updateIfNotFreshDeferred().blockingAwait()

        val currentBalance = repositoryProvider.balances().itemsList
                .find { it.assetCode == asset }!!.available

        val expected = initialBalance - paymentAmount - request.senderFee.total

        Assert.assertEquals("Result balance must be lower than the initial one by payment amount and fee",
                expected, currentBalance)
    }
}