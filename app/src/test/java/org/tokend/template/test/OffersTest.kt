package org.tokend.template.test

import junit.framework.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.template.data.model.OfferRecord
import org.tokend.template.di.providers.*
import org.tokend.template.features.offers.logic.CancelOfferUseCase
import org.tokend.template.features.offers.logic.ConfirmOfferUseCase
import org.tokend.template.features.offers.logic.PrepareOfferUseCase
import org.tokend.template.logic.FeeManager
import org.tokend.template.logic.Session
import org.tokend.template.logic.transactions.TxManager
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.AssetPairPolicy
import org.tokend.wallet.xdr.ManageAssetPairAction
import org.tokend.wallet.xdr.ManageAssetPairOp
import org.tokend.wallet.xdr.Operation
import java.math.BigDecimal

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class OffersTest {
    private val price = BigDecimal("5.5")
    private val baseAmount = BigDecimal.ONE
    private val emissionAmount = BigDecimal.TEN!!

    @Test
    fun aPrepareOffer() {
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

        val quoteAsset = Util.createAsset(apiProvider, TxManager(apiProvider))

        val offer = OfferRecord(
                "ETH",
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

        Assert.assertNotNull("Prepared offer must contain fee", prepared.fee)
    }

    @Test
    fun bConfirmOffer() {
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

        val baseAsset = Util.createAsset(apiProvider, txManager)

        val quoteAsset = Util.createAsset(apiProvider, txManager)

        createAssetPair(baseAsset, quoteAsset, apiProvider, txManager)

        Util.getSomeMoney(baseAsset, emissionAmount,
                repositoryProvider, session, txManager)

        submitOffer(baseAsset, quoteAsset, session, apiProvider, repositoryProvider)

        val offersRepository = repositoryProvider.offers(false)

        Assert.assertFalse("Offers repository must be invalidated after offer submitting",
                offersRepository.isFresh)
        Assert.assertFalse("Balances repository must be invalidated after offer submitting",
                repositoryProvider.balances().isFresh)

        Thread.sleep(500)

        offersRepository.updateIfNotFreshDeferred().blockingAwait()

        Assert.assertTrue("There must be a newly created offer in offers repository",
                offersRepository.itemsList.isNotEmpty())
    }

    @Test
    fun cCancelOffer() {
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

        val baseAsset = Util.createAsset(apiProvider, txManager)

        val quoteAsset = Util.createAsset(apiProvider, txManager)

        createAssetPair(baseAsset, quoteAsset, apiProvider, txManager)

        val initialBalance = Util.getSomeMoney(baseAsset, emissionAmount,
                repositoryProvider, session, txManager)

        submitOffer(baseAsset, quoteAsset, session, apiProvider, repositoryProvider)

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

        Assert.assertTrue("Offers repository must be empty after the only offer cancellation",
                offersRepository.itemsList.isEmpty())
        Assert.assertFalse("Balances repository must be invalidated after offer cancellation",
                repositoryProvider.balances().isFresh)

        Thread.sleep(500)

        repositoryProvider.balances().updateIfNotFreshDeferred().blockingAwait()

        val currentBalance = repositoryProvider.balances().itemsList
                .find { it.assetCode == baseAsset }!!.available

        Assert.assertEquals("Balance after offer cancellation must be equal to the initial one",
                0, initialBalance.compareTo(currentBalance))
    }

    @Test
    fun dConfirmOfferCancelPrevious() {
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

        val baseAsset = Util.createAsset(apiProvider, txManager)

        val quoteAsset = Util.createAsset(apiProvider, txManager)

        createAssetPair(baseAsset, quoteAsset, apiProvider, txManager)

        Util.getSomeMoney(baseAsset, emissionAmount,
                repositoryProvider, session, txManager)

        submitOffer(baseAsset, quoteAsset, session, apiProvider, repositoryProvider)

        val offersRepository = repositoryProvider.offers(false)

        Thread.sleep(500)

        offersRepository.updateIfNotFreshDeferred().blockingAwait()

        val offerToCancel = offersRepository.itemsList.first()

        submitOffer(baseAsset, quoteAsset, session, apiProvider, repositoryProvider, offerToCancel)

        Thread.sleep(500)

        offersRepository.updateIfNotFreshDeferred().blockingAwait()

        Assert.assertTrue("There must be a newly created offer in offers repository",
                offersRepository.itemsList.isNotEmpty())
        Assert.assertFalse("There must not be a cancelled offer in offers repository",
                offersRepository.itemsList.any {
                    it.id == offerToCancel.id
                })
    }

    private fun submitOffer(baseAsset: String, quoteAsset: String,
                            session: Session, apiProvider: ApiProvider,
                            repositoryProvider: RepositoryProvider,
                            offerToCancel: OfferRecord? = null) {
        val offer = OfferRecord(
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

    private fun createAssetPair(baseAsset: String, quoteAsset: String,
                                apiProvider: ApiProvider, txManager: TxManager) {
        val sourceAccount = Config.ADMIN_ACCOUNT

        val systemInfo =
                apiProvider.getApi()
                        .general
                        .getSystemInfo()
                        .execute()
                        .get()
        val netParams = systemInfo.toNetworkParams()

        val createOp = ManageAssetPairOp(
                base = baseAsset,
                quote = quoteAsset,
                physicalPrice = netParams.amountToPrecised(price),
                physicalPriceCorrection = 0,
                maxPriceStep = netParams.amountToPrecised(BigDecimal.TEN),
                policies = AssetPairPolicy.TRADEABLE_SECONDARY_MARKET.value,
                action = ManageAssetPairAction.CREATE,
                ext = ManageAssetPairOp.ManageAssetPairOpExt.EmptyVersion()
        )

        val updateOp = ManageAssetPairOp(
                base = baseAsset,
                quote = quoteAsset,
                physicalPrice = netParams.amountToPrecised(price),
                physicalPriceCorrection = 0,
                maxPriceStep = netParams.amountToPrecised(BigDecimal.TEN),
                policies = AssetPairPolicy.TRADEABLE_SECONDARY_MARKET.value,
                action = ManageAssetPairAction.UPDATE_POLICIES,
                ext = ManageAssetPairOp.ManageAssetPairOpExt.EmptyVersion()
        )

        val tx = TransactionBuilder(netParams, sourceAccount.accountId)
                .addOperation(Operation.OperationBody.ManageAssetPair(createOp))
                .addOperation(Operation.OperationBody.ManageAssetPair(updateOp))
                .build()

        tx.addSignature(sourceAccount)

        txManager.submit(tx).blockingGet()
    }
}