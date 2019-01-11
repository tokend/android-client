package org.tokend.template.test

import junit.framework.Assert
import org.junit.Test
import org.tokend.sdk.api.trades.model.Offer
import org.tokend.template.di.providers.*
import org.tokend.template.features.offers.logic.CancelOfferUseCase
import org.tokend.template.features.offers.logic.ConfirmOfferUseCase
import org.tokend.template.features.offers.logic.PrepareOfferUseCase
import org.tokend.template.logic.FeeManager
import org.tokend.template.logic.Session
import org.tokend.template.logic.transactions.TxManager
import org.tokend.wallet.Account
import org.tokend.wallet.PublicKeyFactory
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.CreateIssuanceRequestOp
import org.tokend.wallet.xdr.Fee
import org.tokend.wallet.xdr.IssuanceRequest
import org.tokend.wallet.xdr.Operation
import java.math.BigDecimal

class OffersTest {
    private val baseAsset = "ETH"
    private val quoteAsset = "USD"
    private val price = BigDecimal("5.5")
    private val baseAmount = BigDecimal.ONE
    private val emissionAmount = BigDecimal.TEN!!

    @Test
    fun prepareOffer() {
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

        val offer = Offer(
                baseAsset,
                quoteAsset,
                isBuy = false,
                baseAmount = baseAmount,
                price = price
        )

        val useCase = PrepareOfferUseCase(
                offer,
                session,
                FeeManager(apiProvider)
        )

        val prepared = useCase.perform().blockingGet()

        Assert.assertNotNull(prepared.fee)
    }

    @Test
    fun confirmOffer() {
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

        Util.getSomeMoney(baseAsset, emissionAmount,
                repositoryProvider, TxManager(apiProvider))

        submitOffer(session, apiProvider, repositoryProvider)

        val offersRepository = repositoryProvider.offers(false)

        Assert.assertFalse(offersRepository.isFresh)
        Assert.assertFalse(repositoryProvider.balances().isFresh)

        Thread.sleep(500)

        offersRepository.updateIfNotFreshDeferred().blockingAwait()

        Assert.assertTrue(offersRepository.itemsList.isNotEmpty())
    }

    @Test
    fun confirmOfferCancelPrevious() {
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

        Util.getSomeMoney(baseAsset, emissionAmount,
                repositoryProvider, TxManager(apiProvider))

        submitOffer(session, apiProvider, repositoryProvider)

        val offersRepository = repositoryProvider.offers(false)

        Thread.sleep(500)

        offersRepository.updateIfNotFreshDeferred().blockingAwait()

        val offerToCancel = offersRepository.itemsList.first()

        submitOffer(session, apiProvider, repositoryProvider, offerToCancel)

        Thread.sleep(500)

        offersRepository.updateIfNotFreshDeferred().blockingAwait()

        Assert.assertTrue(offersRepository.itemsList.isNotEmpty())
        Assert.assertFalse(offersRepository.itemsList.any {
            it.id == offerToCancel.id
        })
    }

    @Test
    fun cancelOffer() {
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

        val initialBalance = Util.getSomeMoney(baseAsset, emissionAmount,
                repositoryProvider, TxManager(apiProvider))

        submitOffer(session, apiProvider, repositoryProvider)

        val offersRepository = repositoryProvider.offers(false)

        Thread.sleep(500)

        offersRepository.updateIfNotFreshDeferred().blockingAwait()

        val offerToCancel = offersRepository.itemsList.first()

        val useCase = CancelOfferUseCase(
                offerToCancel,
                repositoryProvider,
                session,
                TxManager(apiProvider)
        )

        useCase.perform().blockingAwait()

        Assert.assertTrue(offersRepository.itemsList.isEmpty())
        Assert.assertFalse(repositoryProvider.balances().isFresh)

        Thread.sleep(500)

        repositoryProvider.balances().updateIfNotFreshDeferred().blockingAwait()

        val currentBalance = repositoryProvider.balances().itemsList
                .find { it.assetCode == baseAsset }!!.available

        Assert.assertEquals(0, initialBalance.compareTo(currentBalance))
    }

    private fun submitOffer(session: Session, apiProvider: ApiProvider,
                            repositoryProvider: RepositoryProvider,
                            offerToCancel: Offer? = null) {
        val offer = Offer(
                baseAsset,
                quoteAsset,
                isBuy = false,
                baseAmount = baseAmount,
                price = price
        )

        val prepared = PrepareOfferUseCase(
                offer,
                session,
                FeeManager(apiProvider)
        ).perform().blockingGet()

        val useCase = ConfirmOfferUseCase(
                prepared,
                offerToCancel,
                repositoryProvider,
                session,
                TxManager(apiProvider)
        )

        useCase.perform().blockingAwait()
    }
}