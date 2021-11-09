package io.tokend.template.test

import io.tokend.template.di.providers.*
import io.tokend.template.features.assets.model.SimpleAsset
import io.tokend.template.features.fees.logic.FeeManager
import io.tokend.template.features.history.model.details.BalanceChangeCause
import io.tokend.template.features.offers.logic.CancelOfferUseCase
import io.tokend.template.features.offers.logic.ConfirmOfferRequestUseCase
import io.tokend.template.features.offers.logic.CreateOfferRequestUseCase
import io.tokend.template.features.offers.model.OfferRecord
import io.tokend.template.logic.session.Session
import io.tokend.template.logic.TxManager
import junit.framework.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.tokend.sdk.factory.JsonApiToolsProvider
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
    fun aCreateOffer() {
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

        val quoteAsset = Util.createAsset(apiProvider, TxManager(apiProvider))

        val useCase = CreateOfferRequestUseCase(
            baseAsset = SimpleAsset("ETH"),
            quoteAsset = SimpleAsset(quoteAsset),
            isBuy = false,
            baseAmount = baseAmount,
            price = price,
            orderBookId = 0,
            offerToCancel = null,
            walletInfoProvider = session,
            feeManager = FeeManager(apiProvider)
        )

        val request = useCase.perform().blockingGet()

        Assert.assertNotNull("Prepared offer must contain fee", request.fee)
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
        val repositoryProvider = RepositoryProviderImpl(
            apiProvider, session, urlConfigProvider,
            JsonApiToolsProvider.getObjectMapper()
        )

        Util.getVerifiedWallet(
            email, password, apiProvider, session, repositoryProvider
        )

        val txManager = TxManager(apiProvider)

        val baseAsset = Util.createAsset(apiProvider, txManager)

        val quoteAsset = Util.createAsset(apiProvider, txManager)

        createAssetPair(baseAsset, quoteAsset, apiProvider, txManager)

        val initialQuoteBalance = Util.getSomeMoney(
            quoteAsset, emissionAmount,
            repositoryProvider, session, txManager
        )

        submitBuyOffer(baseAsset, quoteAsset, session, apiProvider, repositoryProvider)

        val currentQuoteBalance = repositoryProvider.balances.itemsList
            .first { it.assetCode == quoteAsset }.available
        Assert.assertEquals(
            "Quote balance must be lower that the initial one by offer quote amount",
            0, currentQuoteBalance.compareTo(initialQuoteBalance - price * baseAmount)
        )

        val offersRepository = repositoryProvider.offers(false)

        Thread.sleep(2000)

        offersRepository.updateIfNotFreshDeferred().blockingAwait()

        Assert.assertTrue(
            "There must be a newly created offer in offers repository",
            offersRepository.itemsList.isNotEmpty()
        )

        val offer = offersRepository.itemsList.first()

        val historyRepository = repositoryProvider.balanceChanges(offer.quoteBalanceId)
        historyRepository.updateIfNotFreshDeferred().blockingAwait()
        val transactions = historyRepository.itemsList

        Assert.assertTrue(
            "History must not be empty after withdrawal sending",
            transactions.isNotEmpty()
        )
        Assert.assertTrue(
            "First history entry must be offer-related after offer sending",
            transactions.first().cause is BalanceChangeCause.Offer
        )
        Assert.assertEquals("Offer-related history entry must have the same id with sent offer",
            offer.id,
            transactions
                .first()
                .cause
                .let { it as BalanceChangeCause.Offer }
                .offerId
        )
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
        val repositoryProvider = RepositoryProviderImpl(
            apiProvider, session, urlConfigProvider,
            JsonApiToolsProvider.getObjectMapper()
        )

        Util.getVerifiedWallet(
            email, password, apiProvider, session, repositoryProvider
        )

        val txManager = TxManager(apiProvider)

        val baseAsset = Util.createAsset(apiProvider, txManager)

        val quoteAsset = Util.createAsset(apiProvider, txManager)

        createAssetPair(baseAsset, quoteAsset, apiProvider, txManager)

        val initialBalance = Util.getSomeMoney(
            quoteAsset, emissionAmount,
            repositoryProvider, session, txManager
        )

        submitBuyOffer(baseAsset, quoteAsset, session, apiProvider, repositoryProvider)

        val offersRepository = repositoryProvider.offers(false)

        Thread.sleep(2000)

        offersRepository.updateIfNotFreshDeferred().blockingAwait()

        val offerToCancel = offersRepository.itemsList.first()

        val useCase = CancelOfferUseCase(
            offerToCancel,
            repositoryProvider,
            session,
            TxManager(apiProvider)
        )

        useCase.perform().blockingAwait()

        Assert.assertTrue(
            "Offers repository must be empty after the only offer cancellation",
            offersRepository.itemsList.isEmpty()
        )

        Thread.sleep(2000)

        repositoryProvider.balances.updateIfNotFreshDeferred().blockingAwait()

        val currentBalance = repositoryProvider.balances.itemsList
            .find { it.assetCode == quoteAsset }!!.available

        Assert.assertEquals(
            "Balance after offer cancellation must be equal to the initial one",
            0, initialBalance.compareTo(currentBalance)
        )
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
        val repositoryProvider = RepositoryProviderImpl(
            apiProvider, session, urlConfigProvider,
            JsonApiToolsProvider.getObjectMapper()
        )

        Util.getVerifiedWallet(
            email, password, apiProvider, session, repositoryProvider
        )

        val txManager = TxManager(apiProvider)

        val baseAsset = Util.createAsset(apiProvider, txManager)

        val quoteAsset = Util.createAsset(apiProvider, txManager)

        createAssetPair(baseAsset, quoteAsset, apiProvider, txManager)

        Util.getSomeMoney(
            quoteAsset, emissionAmount,
            repositoryProvider, session, txManager
        )

        submitBuyOffer(baseAsset, quoteAsset, session, apiProvider, repositoryProvider)

        val offersRepository = repositoryProvider.offers(false)

        Thread.sleep(2000)

        offersRepository.updateIfNotFreshDeferred().blockingAwait()

        val offerToCancel = offersRepository.itemsList.first()

        submitBuyOffer(
            baseAsset,
            quoteAsset,
            session,
            apiProvider,
            repositoryProvider,
            offerToCancel
        )

        Thread.sleep(2000)

        offersRepository.updateIfNotFreshDeferred().blockingAwait()

        Assert.assertTrue(
            "There must be a newly created offer in offers repository",
            offersRepository.itemsList.isNotEmpty()
        )
        Assert.assertFalse("There must not be a cancelled offer in offers repository",
            offersRepository.itemsList.any {
                it.id == offerToCancel.id
            })
    }

    private fun submitBuyOffer(
        baseAsset: String, quoteAsset: String,
        session: Session, apiProvider: ApiProvider,
        repositoryProvider: RepositoryProvider,
        offerToCancel: OfferRecord? = null
    ) {
        val request = CreateOfferRequestUseCase(
            baseAsset = SimpleAsset(baseAsset),
            quoteAsset = SimpleAsset(quoteAsset),
            isBuy = true,
            baseAmount = baseAmount,
            price = price,
            orderBookId = 0,
            offerToCancel = offerToCancel,
            walletInfoProvider = session,
            feeManager = FeeManager(apiProvider)
        ).perform().blockingGet()

        val useCase = ConfirmOfferRequestUseCase(
            request,
            session,
            repositoryProvider,
            TxManager(apiProvider)
        )

        useCase.perform().blockingAwait()
    }

    private fun createAssetPair(
        baseAsset: String, quoteAsset: String,
        apiProvider: ApiProvider, txManager: TxManager
    ) {
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