package org.tokend.template.features.offers

import android.app.Activity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_details_list.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.features.offers.model.OfferRecord
import org.tokend.template.features.offers.logic.ConfirmOfferUseCase
import org.tokend.template.logic.transactions.TxManager
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.details.DetailsItem
import org.tokend.template.view.details.adapter.DetailsItemsAdapter
import org.tokend.template.view.util.ProgressDialogFactory
import java.math.BigDecimal

open class OfferConfirmationActivity : BaseActivity() {
    protected lateinit var offer: OfferRecord
    protected var prevOffer: OfferRecord? = null

    protected val adapter = DetailsItemsAdapter()

    protected val payAsset: String
        get() =
            if (offer.isBuy)
                offer.quoteAssetCode
            else
                offer.baseAssetCode
    protected val toPayTotal: BigDecimal
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
    protected val toReceiveTotal: BigDecimal
        get() =
            (if (!offer.isBuy)
                offer.quoteAmount - offer.fee
            else
                offer.baseAmount).takeIf { it.signum() > 0 } ?: BigDecimal.ZERO

    protected val cancellationOnly: Boolean
        get() = offer.baseAmount.signum() == 0 && prevOffer != null

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_details_list)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        details_list.layoutManager = LinearLayoutManager(this)
        details_list.adapter = adapter

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
        displayPrice()
        displayToPay()
        displayToReceive()
    }

    protected open fun displayPrice() {
        adapter.addData(
                DetailsItem(
                        text = getString(R.string.template_price_one_equals, offer.baseAssetCode,
                                amountFormatter.formatAssetAmount(offer.price, offer.quoteAssetCode)
                        ),
                        hint = getString(R.string.price),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_price)
                )
        )
    }

    protected open fun displayToPay() {
        val payAmount =
                if (offer.isBuy)
                    offer.quoteAmount
                else
                    offer.baseAmount

        adapter.addData(
                DetailsItem(
                        header = getString(R.string.to_pay),
                        text = amountFormatter.formatAssetAmount(payAmount, payAsset),
                        hint = getString(R.string.amount),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_coins)
                )
        )

        if (offer.isBuy && offer.fee.signum() > 0) {
            adapter.addData(
                    DetailsItem(
                            text = amountFormatter.formatAssetAmount(offer.fee, payAsset),
                            hint = getString(R.string.tx_fee)
                    ),
                    DetailsItem(
                            text = amountFormatter.formatAssetAmount(
                                    toPayTotal,
                                    payAsset
                            ),
                            hint = getString(R.string.total_label)
                    )
            )
        }
    }

    protected open fun displayToReceive() {
        val receiveAmount =
                if (!offer.isBuy)
                    offer.quoteAmount
                else
                    offer.baseAmount

        adapter.addData(
                DetailsItem(
                        header = getString(R.string.to_receive),
                        text = amountFormatter.formatAssetAmount(receiveAmount, receiveAsset),
                        hint = getString(R.string.amount),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_coins)
                )
        )

        if (!offer.isBuy && offer.fee.signum() > 0) {
            adapter.addData(
                    DetailsItem(
                            text = amountFormatter.formatAssetAmount(offer.fee, receiveAsset),
                            hint = getString(R.string.tx_fee)
                    ),
                    DetailsItem(
                            text = amountFormatter.formatAssetAmount(
                                    toReceiveTotal,
                                    payAsset
                            ),
                            hint = getString(R.string.total_label)
                    )
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
