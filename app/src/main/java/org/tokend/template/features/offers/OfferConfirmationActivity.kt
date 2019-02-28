package org.tokend.template.features.offers

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.data.model.OfferRecord
import org.tokend.template.features.offers.logic.ConfirmOfferUseCase
import org.tokend.template.logic.transactions.TxManager
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.InfoCard
import org.tokend.template.view.util.ProgressDialogFactory
import java.math.BigDecimal

open class OfferConfirmationActivity : BaseActivity() {
    protected lateinit var offer: OfferRecord
    protected var prevOffer: OfferRecord? = null

    protected val payAsset: String
        get() =
            if (offer.isBuy)
                offer.quoteAssetCode
            else
                offer.baseAssetCode
    protected val toPayAmount: BigDecimal
        get() =
            if (offer.isBuy)
                offer.quoteAmount + offer.fee
            else
                offer.baseAmount

    protected val receiveAsset: String
        get() =
            if (!offer.isBuy)
                offer.quoteAssetCode
            else
                offer.baseAssetCode
    protected val toReceiveAmount: BigDecimal
        get() =
            (if (!offer.isBuy)
                offer.quoteAmount - offer.fee
            else
                offer.baseAmount).takeIf { it.signum() > 0 } ?: BigDecimal.ZERO

    protected val cancellationOnly: Boolean
        get() = offer.baseAmount.signum() == 0 && prevOffer != null

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_details)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initData()

        displayDetails()
    }

    protected open fun initData() {
        offer =
                (intent.getSerializableExtra(OFFER_EXTRA) as? OfferRecord)
                        ?: return
        prevOffer = intent.getSerializableExtra(OFFER_TO_CANCEL_EXTRA) as? OfferRecord
    }

    // region Display
    protected open fun displayDetails() {
        cards_layout.removeAllViews()

        displayToPay()
        displayToReceive()
    }

    protected open fun displayToPay() {
        val minDecimals = amountFormatter.getDecimalDigitsCount(payAsset)

        val payBaseAmount =
                if (offer.isBuy)
                    offer.quoteAmount
                else
                    offer.baseAmount

        val card = InfoCard(cards_layout)
                .setHeading(R.string.to_pay,
                        amountFormatter.formatAssetAmount(toPayAmount, payAsset, minDecimals))

        if (offer.isBuy) {
            card.addRow(R.string.amount,
                    "+${amountFormatter.formatAssetAmount(payBaseAmount, payAsset, minDecimals)}")
                    .addRow(R.string.tx_fee,
                            "+${amountFormatter.formatAssetAmount(offer.fee, payAsset, minDecimals)}")
        } else {
            card.addRow(R.string.price, getString(R.string.template_price_one_equals, offer.baseAssetCode,
                    amountFormatter.formatAssetAmount(offer.price, offer.quoteAssetCode))
            )
        }
    }

    protected open fun displayToReceive() {
        val minDecimals = amountFormatter.getDecimalDigitsCount(receiveAsset)

        val receiveBaseAmount =
                if (!offer.isBuy)
                    offer.quoteAmount
                else
                    offer.baseAmount

        val card = InfoCard(cards_layout)
                .setHeading(R.string.to_receive,
                        amountFormatter.formatAssetAmount(toReceiveAmount, receiveAsset, minDecimals))

        if (!offer.isBuy) {
            card
                    .addRow(R.string.amount,
                            "+${amountFormatter.formatAssetAmount(receiveBaseAmount, receiveAsset,
                                    minDecimals)}")
                    .addRow(R.string.tx_fee,
                            "-${amountFormatter.formatAssetAmount(offer.fee, receiveAsset,
                                    minDecimals)}")
        } else {
            card.addRow(R.string.price, getString(R.string.template_price_one_equals, offer.baseAssetCode,
                    amountFormatter.formatAssetAmount(offer.price, offer.quoteAssetCode))
            )
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
        val progress = ProgressDialogFactory.getTunedDialog(this)

        ConfirmOfferUseCase(
                offer,
                prevOffer,
                repositoryProvider,
                accountProvider,
                TxManager(apiProvider)
        )
                .perform()
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .doOnSubscribe { progress.show() }
                .doOnTerminate { progress.dismiss() }
                .subscribeBy(
                        onComplete = {
                            progress.dismiss()
                            toastManager.short(getSuccessMessage())
                            finishWithSuccess()
                        },
                        onError = {
                            errorHandlerFactory.getDefault().handle(it)
                        }
                )
    }

    protected open fun getSuccessMessage(): String {
        return getString(R.string.offer_created)
    }

    private fun finishWithSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    companion object {
        const val OFFER_EXTRA = "offer"
        const val OFFER_TO_CANCEL_EXTRA = "offer_to_cancel"
    }
}
