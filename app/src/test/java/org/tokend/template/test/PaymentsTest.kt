package org.tokend.template.test

import org.junit.Assert
import org.junit.Test
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.sdk.utils.extentions.encodeHexString
import org.tokend.template.di.providers.AccountProviderFactory
import org.tokend.template.di.providers.ApiProviderFactory
import org.tokend.template.di.providers.RepositoryProviderImpl
import org.tokend.template.di.providers.WalletInfoProviderFactory
import org.tokend.template.features.send.logic.ConfirmPaymentRequestUseCase
import org.tokend.template.features.send.logic.CreatePaymentRequestUseCase
import org.tokend.template.logic.FeeManager
import org.tokend.template.logic.Session
import org.tokend.template.logic.transactions.TxManager
import org.tokend.wallet.Account
import org.tokend.wallet.xdr.FeeType
import org.tokend.wallet.xdr.PaymentFeeType
import java.math.BigDecimal
import java.security.SecureRandom

class PaymentsTest {
    private val emissionAmount = BigDecimal.TEN
    private val paymentAmount = BigDecimal.ONE

    @Test
    fun createPayment() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )

        val email = "${System.currentTimeMillis()}@mail.com"
        val password = "qwe123".toCharArray()
        val recipientEmail = "${SecureRandom.getSeed(12).encodeHexString()}@mail.com"

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
        val asset = Util.createAsset(Account.fromSecretSeed(Config.ADMIN_SEED), apiProvider,
                txManager)
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
        val recipientEmail = "${SecureRandom.getSeed(12).encodeHexString()}@mail.com"

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

        val asset = Util.createAsset(Account.fromSecretSeed(Config.ADMIN_SEED), apiProvider,
                txManager)
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
        val recipientEmail = "${SecureRandom.getSeed(12).encodeHexString()}@mail.com"

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

        val asset = Util.createAsset(Account.fromSecretSeed(Config.ADMIN_SEED), apiProvider,
                txManager)

        val feeType = FeeType.PAYMENT_FEE
        val feeSubType = PaymentFeeType.OUTGOING.value

        val result = Util.addFeeForAccount(
                rootAccount,
                repositoryProvider,
                txManager,
                feeType,
                feeSubType,
                asset
        )

        Assert.assertTrue(result)

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

        Assert.assertTrue(request.senderFee.total > BigDecimal.ZERO)

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

        Assert.assertEquals(expected, currentBalance)
    }
}