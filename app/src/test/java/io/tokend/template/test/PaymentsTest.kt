package io.tokend.template.test

import io.tokend.template.features.fees.logic.FeeManager
import io.tokend.template.features.history.model.details.BalanceChangeCause
import io.tokend.template.features.send.amount.logic.PaymentFeeLoader
import io.tokend.template.features.send.logic.ConfirmPaymentRequestUseCase
import io.tokend.template.features.send.logic.CreatePaymentRequestUseCase
import io.tokend.template.features.send.recipient.logic.PaymentRecipientLoader
import io.tokend.template.logic.TxManager
import io.tokend.template.logic.providers.AccountProviderFactory
import io.tokend.template.logic.providers.ApiProviderFactory
import io.tokend.template.logic.providers.RepositoryProviderImpl
import io.tokend.template.logic.providers.WalletInfoProviderFactory
import io.tokend.template.logic.session.Session
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.tokend.sdk.factory.JsonApiToolsProvider
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
        val repositoryProvider = RepositoryProviderImpl(
            apiProvider, session, urlConfigProvider,
            JsonApiToolsProvider.getObjectMapper()
        )

        val (_) = Util.getVerifiedWallet(
            recipientEmail, password, apiProvider, session, null
        )

        Util.getVerifiedWallet(
            email, password, apiProvider, session, repositoryProvider
        )

        val txManager = TxManager(apiProvider)
        val asset = Util.createAsset(apiProvider, txManager)
        Util.getSomeMoney(
            asset, emissionAmount,
            repositoryProvider, session, txManager
        )

        val recipient = PaymentRecipientLoader(repositoryProvider.accountIdentities)
            .load(recipientEmail)
            .blockingGet()

        val feeLoader = PaymentFeeLoader(session, FeeManager(apiProvider))

        val fee = feeLoader.load(paymentAmount, asset, recipient.accountId).blockingGet()

        val balance = repositoryProvider.balances.itemsList.first { it.assetCode == asset }

        val useCase = CreatePaymentRequestUseCase(
            recipient,
            paymentAmount,
            balance,
            "Test payment",
            fee,
            session
        )

        val request = useCase.perform().blockingGet()

        Assert.assertEquals(
            "Payment request amount must be equal to the requested amount",
            paymentAmount, request.amount
        )
        Assert.assertTrue(
            "Payment request recipient must be a valid account ID",
            Base32Check.isValid(
                Base32Check.VersionByte.ACCOUNT_ID,
                recipient.accountId.toCharArray()
            )
        )
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
        val repositoryProvider = RepositoryProviderImpl(
            apiProvider, session, urlConfigProvider,
            JsonApiToolsProvider.getObjectMapper()
        )

        val (_) = Util.getVerifiedWallet(
            recipientEmail, password, apiProvider, session, null
        )

        Util.getVerifiedWallet(
            email, password, apiProvider, session, repositoryProvider
        )

        val txManager = TxManager(apiProvider)

        val asset = Util.createAsset(apiProvider, txManager)
        val initialBalance = Util.getSomeMoney(
            asset, emissionAmount,
            repositoryProvider, session, txManager
        )

        val recipient = PaymentRecipientLoader(repositoryProvider.accountIdentities)
            .load(recipientEmail)
            .blockingGet()

        val fee = PaymentFeeLoader(session, FeeManager(apiProvider))
            .load(paymentAmount, asset, recipient.accountId)
            .blockingGet()

        val balance = repositoryProvider.balances.itemsList.first { it.assetCode == asset }

        val request = CreatePaymentRequestUseCase(
            recipient,
            paymentAmount,
            balance,
            "Test payment",
            fee,
            session
        ).perform().blockingGet()

        ConfirmPaymentRequestUseCase(
            request,
            session,
            repositoryProvider,
            txManager
        ).perform().blockingAwait()

        Thread.sleep(2000)

        repositoryProvider.balances.updateIfNotFreshDeferred().blockingAwait()

        val currentBalance = repositoryProvider.balances.itemsList
            .find { it.assetCode == asset }!!.available

        Assert.assertNotEquals(
            "Balance must be changed after the payment sending",
            0, initialBalance.compareTo(currentBalance)
        )

        val historyRepository = repositoryProvider.balanceChanges(request.senderBalanceId)
        historyRepository.updateIfNotFreshDeferred().blockingAwait()
        val transactions = historyRepository.itemsList

        Assert.assertTrue(
            "History must not be empty after the payment sending",
            transactions.isNotEmpty()
        )
        Assert.assertTrue(
            "First history entry must be a payment after the payment sending",
            transactions.first().cause is BalanceChangeCause.Payment
        )
        Assert.assertEquals("Payment history entry must have a requested destination account id",
            request.recipient.accountId,
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
        val repositoryProvider = RepositoryProviderImpl(
            apiProvider, session, urlConfigProvider,
            JsonApiToolsProvider.getObjectMapper()
        )

        val (_, _) = Util.getVerifiedWallet(
            recipientEmail, password, apiProvider, session, null
        )

        val (_, rootAccount) = Util.getVerifiedWallet(
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

        val initialBalance = Util.getSomeMoney(
            asset, emissionAmount,
            repositoryProvider, session, txManager
        )

        val recipient = PaymentRecipientLoader(repositoryProvider.accountIdentities)
            .load(recipientEmail)
            .blockingGet()

        val fee = PaymentFeeLoader(session, FeeManager(apiProvider))
            .load(paymentAmount, asset, recipient.accountId)
            .blockingGet()

        val balance = repositoryProvider.balances.itemsList.first { it.assetCode == asset }

        val request = CreatePaymentRequestUseCase(
            recipient,
            paymentAmount,
            balance,
            "Test payment with fee",
            fee,
            session
        ).perform().blockingGet()

        Assert.assertTrue(
            "Payment request sender fee must greater than zero",
            request.fee.senderFee.total > BigDecimal.ZERO
        )

        ConfirmPaymentRequestUseCase(
            request,
            session,
            repositoryProvider,
            txManager
        ).perform().blockingAwait()

        val currentBalance = repositoryProvider.balances.itemsList
            .find { it.assetCode == asset }!!.available

        val expected = initialBalance - paymentAmount - request.fee.senderFee.total

        Assert.assertEquals(
            "Result balance must be lower than the initial one by payment amount and fee",
            0, currentBalance.compareTo(expected)
        )
    }
}