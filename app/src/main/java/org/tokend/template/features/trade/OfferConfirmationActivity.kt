package org.tokend.template.features.trade

import android.app.Activity
import android.app.ProgressDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.sdk.api.models.Offer
import org.tokend.template.R
import org.tokend.template.base.activities.BaseActivity
import org.tokend.template.base.logic.transactions.TxManager
import org.tokend.template.base.view.InfoCard
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.extensions.getNullableStringExtra
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.ToastManager
import org.tokend.template.util.error_handlers.ErrorHandlerFactory
import java.math.BigDecimal

class OfferConfirmationActivity : BaseActivity() {
    private lateinit var offer: Offer
    private var prevOffer: Offer? = null
    private var assetName: String? = null

    private val payAsset: String
        get() =
            if (offer.isBuy)
                offer.quoteAsset
            else
                offer.baseAsset
    private val toPayAmount: BigDecimal
        get() =
            if (offer.isBuy)
                offer.quoteAmount + (offer.fee ?: BigDecimal.ZERO)
            else
                offer.baseAmount

    private val receiveAsset: String
        get() =
            if (!offer.isBuy)
                offer.quoteAsset
            else
                offer.baseAsset
    private val toReceiveAmount: BigDecimal
        get() =
            (if (!offer.isBuy)
                offer.quoteAmount - (offer.fee ?: BigDecimal.ZERO)
            else
                offer.baseAmount).takeIf { it.signum() > 0 } ?: BigDecimal.ZERO

    private val isPrimaryMarket: Boolean
        get() = offer.orderBookId != 0L

    private val displayToReceive: Boolean
        get() = intent.getBooleanExtra(DISPLAY_TO_RECEIVE, true)

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_details)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        offer =
                (intent.getSerializableExtra(OFFER_EXTRA) as? Offer)
                ?: return
        prevOffer = intent.getSerializableExtra(OFFER_TO_CANCEL_EXTRA) as? Offer
        assetName = intent.getNullableStringExtra(ASSET_NAME_EXTRA)

        if (isPrimaryMarket) {
            setTitle(R.string.investment_confirmation_title)
        }

        displayDetails()
    }

    // region Display
    private fun displayDetails() {
        cards_layout.removeAllViews()

        if (assetName != null) {
            displayToken()
        }

        displayToPay()

        if (displayToReceive) {
            displayToReceive()
        }
    }

    private fun displayToken() {
        InfoCard(cards_layout)
                .setHeading(R.string.sale_token, null)
                .addRow(assetName, offer.baseAsset)
    }

    private fun displayToPay() {
        val payBaseAmount =
                if (offer.isBuy)
                    offer.quoteAmount
                else
                    offer.baseAmount

        val card = InfoCard(cards_layout)
                .setHeading(R.string.to_pay,
                        "${AmountFormatter.formatAssetAmount(toPayAmount, payAsset)
                        } $payAsset")

        if (offer.isBuy) {
            val amountRes =
                    if (isPrimaryMarket)
                        R.string.investment_amount
                    else
                        R.string.amount

            card.addRow(amountRes,
                    "+${AmountFormatter.formatAssetAmount(payBaseAmount,
                            payAsset, minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                    } $payAsset")
                    .addRow(R.string.tx_fee,
                            "+${AmountFormatter.formatAssetAmount(offer.fee,
                                    payAsset, minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                            } $payAsset")
        } else {
            card.addRow(R.string.price, getString(R.string.template_price_one_for, offer.baseAsset,
                    AmountFormatter.formatAssetAmount(offer.price), offer.quoteAsset))
        }
    }

    private fun displayToReceive() {
        val receiveBaseAmount =
                if (!offer.isBuy)
                    offer.quoteAmount
                else
                    offer.baseAmount

        val card = InfoCard(cards_layout)
                .setHeading(R.string.to_receive,
                        "${AmountFormatter.formatAssetAmount(toReceiveAmount, receiveAsset)
                        } $receiveAsset")

        if (!offer.isBuy) {
            card
                    .addRow(R.string.amount,
                            "+${AmountFormatter.formatAssetAmount(receiveBaseAmount,
                                    receiveAsset, minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                            } $receiveAsset")
                    .addRow(R.string.tx_fee,
                            "-${AmountFormatter.formatAssetAmount(offer.fee,
                                    receiveAsset, minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                            } $receiveAsset")
        } else {
            card.addRow(R.string.price, getString(R.string.template_price_one_for, offer.baseAsset,
                    AmountFormatter.formatAssetAmount(offer.price), offer.quoteAsset))
        }
    }
    // endregion

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.confirmation, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.confirm -> confirm()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun confirm() {
        val progress = ProgressDialog(this)
        progress.isIndeterminate = true
        progress.setMessage(getString(R.string.processing_progress))
        progress.setCancelable(false)

        val cancellationOnly = offer.baseAmount.signum() == 0 && prevOffer != null

        getBalances(offer.baseAsset, offer.quoteAsset)
                .flatMapCompletable { (baseBalance, quoteBalance) ->
                    offer.baseBalance = baseBalance
                    offer.quoteBalance = quoteBalance
                    prevOffer?.baseBalance = baseBalance
                    prevOffer?.quoteBalance = quoteBalance

                    if (cancellationOnly)
                        repositoryProvider.offers(isPrimaryMarket)
                                .cancel(accountProvider,
                                        repositoryProvider.systemInfo(),
                                        TxManager(apiProvider),
                                        prevOffer!!)
                    else
                        repositoryProvider.offers(isPrimaryMarket)
                                .create(
                                        accountProvider,
                                        repositoryProvider.systemInfo(),
                                        TxManager(apiProvider),
                                        offer,
                                        prevOffer
                                )
                }
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .doOnSubscribe { progress.show() }
                .doOnTerminate { progress.dismiss() }
                .doOnComplete {
                    if (!isPrimaryMarket) {
                        listOf(repositoryProvider.orderBook(
                                offer.baseAsset,
                                offer.quoteAsset,
                                true
                        ), repositoryProvider.orderBook(
                                offer.baseAsset,
                                offer.quoteAsset,
                                false
                        )).forEach { it.invalidate() }
                    }
                    if (isPrimaryMarket) {
                        repositoryProvider.sales().invalidate()
                    }
                    repositoryProvider.balances().invalidate()
                }
                .subscribeBy(
                        onComplete = {
                            progress.dismiss()
                            if (isPrimaryMarket) {
                                if (cancellationOnly) {
                                    ToastManager(this).short(R.string.investment_canceled)
                                } else {
                                    ToastManager(this).short(R.string.successfully_invested)

                                }
                            } else {
                                ToastManager(this).short(R.string.offer_created)
                            }
                            finishWithSuccess()
                        },
                        onError = {
                            errorHandlerFactory.getDefault().handle(it)
                        }
                )
    }

    private fun getBalances(baseAsset: String, quoteAsset: String): Single<Pair<String, String>> {
        val balancesRepository = repositoryProvider.balances()
        val balances = balancesRepository.itemsSubject.value

        val existingBase = balances.find { it.asset == baseAsset }
        val existingQuote = balances.find { it.asset == quoteAsset }

        val toCreate = mutableListOf<String>()
        if (existingBase == null) {
            toCreate.add(baseAsset)
        }
        if (existingQuote == null) {
            toCreate.add(quoteAsset)
        }

        val createMissingBalances =
                if (toCreate.isEmpty())
                    Completable.complete()
                else
                    balancesRepository.create(accountProvider, repositoryProvider.systemInfo(),
                            TxManager(apiProvider), *toCreate.toTypedArray())

        return createMissingBalances
                .andThen(
                        Single.defer {
                            val base = balancesRepository.itemsSubject.value
                                    .find { it.asset == baseAsset }
                                    ?.balanceId
                                    ?: throw IllegalStateException(
                                            "Unable to create balance for $baseAsset"
                                    )
                            val quote = balancesRepository.itemsSubject.value
                                    .find { it.asset == quoteAsset }
                                    ?.balanceId
                                    ?: throw IllegalStateException(
                                            "Unable to create balance for $quoteAsset"
                                    )

                            Single.just(base to quote)
                        }
                )
    }

    private fun finishWithSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    companion object {
        const val OFFER_EXTRA = "offer"
        const val OFFER_TO_CANCEL_EXTRA = "offer_to_cancel"
        const val DISPLAY_TO_RECEIVE = "display_to_receive"
        const val ASSET_NAME_EXTRA = "asset_name"
    }
}
