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
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.ToastManager
import org.tokend.template.util.error_handlers.ErrorHandlerFactory
import java.math.BigDecimal

class OfferConfirmationActivity : BaseActivity() {
    private lateinit var offer: Offer

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

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_details)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        offer =
                (intent.getSerializableExtra(OFFER_EXTRA) as? Offer)
                ?: return

        displayDetails()
    }

    // region Display
    private fun displayDetails() {
        cards_layout.removeAllViews()
        displayToPay()
        displayToReceive()
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
            card.addRow(R.string.amount,
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

        getBalances(offer.baseAsset, offer.quoteAsset)
                .flatMapCompletable { (baseBalance, quoteBalance) ->
                    offer.baseBalance = baseBalance
                    offer.quoteBalance = quoteBalance

                    repositoryProvider.offers().create(
                            accountProvider,
                            repositoryProvider.systemInfo(),
                            TxManager(apiProvider),
                            offer
                    )
                }
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .doOnSubscribe { progress.show() }
                .doOnTerminate { progress.dismiss() }
                .doOnComplete {
                    repositoryProvider.orderBook(
                            offer.baseAsset,
                            offer.quoteAsset,
                            offer.isBuy
                    ).invalidate()
                    repositoryProvider.balances().invalidate()
                }
                .subscribeBy(
                        onComplete = {
                            progress.dismiss()
                            ToastManager.long(R.string.offer_created)
                            finishWithSuccess()
                        },
                        onError = {
                            ErrorHandlerFactory.getDefault().handle(it)
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
    }
}
