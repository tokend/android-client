package org.tokend.template.test

import org.junit.Assert
import org.junit.Test
import org.tokend.template.di.providers.*
import org.tokend.template.features.send.logic.ConfirmPaymentRequestUseCase
import org.tokend.template.features.send.logic.CreatePaymentRequestUseCase
import org.tokend.template.logic.FeeManager
import org.tokend.template.logic.Session
import org.tokend.template.logic.transactions.TxManager
import java.math.BigDecimal

class PaymentsTest {
    private val asset = "BTC"
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
}