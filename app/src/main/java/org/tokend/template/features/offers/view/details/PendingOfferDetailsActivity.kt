package org.tokend.template.features.offers.view.details

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.data.model.OfferRecord
import org.tokend.template.features.offers.logic.CancelOfferUseCase
import org.tokend.template.logic.transactions.TxManager
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.InfoCard
import org.tokend.template.view.util.ProgressDialogFactory
import org.tokend.template.view.util.formatter.DateFormatter
import java.math.BigDecimal

open class PendingOfferDetailsActivity : BaseActivity() {
    protected lateinit var item: OfferRecord

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_details)
        title = getTitleString()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val item = intent.getSerializableExtra(OFFER_EXTRA) as? OfferRecord

        if (item == null) {
            finish()
            return
        }

        this.item = item

        displayDetails(item)
    }

    protected open fun getTitleString(): String {
        return getString(R.string.pending_offer_details_title)
    }

    protected open fun displayDetails(item: OfferRecord) {
        displayPrice(item)
        displayToPay(item)
        displayToReceive(item)
        displayDate(item)
    }

    protected open fun displayToPay(item: OfferRecord) {
        val asset = if (item.isBuy) item.quoteAssetCode else item.baseAssetCode
        val amount = if (item.isBuy) item.quoteAmount else item.baseAmount
        val fee = if (item.isBuy && item.isCancellable) item.fee else BigDecimal.ZERO
        val total = amount + fee
        val minDecimals = amountFormatter.getDecimalDigitsCount(asset)

        InfoCard(cards_layout)
                .setHeading(R.string.to_pay,
                        amountFormatter.formatAssetAmount(total, asset))
                .addRow(R.string.amount,
                        "+" + amountFormatter.formatAssetAmount(amount, asset, minDecimals))
                .addRow(R.string.tx_fee,
                        "+" + amountFormatter.formatAssetAmount(fee, asset, minDecimals))
    }

    protected open fun displayToReceive(item: OfferRecord) {
        val asset = if (item.isBuy) item.baseAssetCode else item.quoteAssetCode
        val amount = if (item.isBuy) item.baseAmount else item.quoteAmount
        val fee = if (item.isBuy || !item.isCancellable) BigDecimal.ZERO else item.fee
        val total = amount - fee
        val minDecimals = amountFormatter.getDecimalDigitsCount(asset)

        InfoCard(cards_layout)
                .setHeading(R.string.to_receive,
                        amountFormatter.formatAssetAmount(total, asset))
                .addRow(R.string.amount,
                        "+" + amountFormatter.formatAssetAmount(amount, asset, minDecimals))
                .addRow(R.string.tx_fee,
                        "-" + amountFormatter.formatAssetAmount(fee, asset, minDecimals))
    }

    protected open fun displayPrice(item: OfferRecord) {
        val formattedPrice = amountFormatter
                .formatAssetAmount(item.price, item.quoteAssetCode)

        val priceString = getString(R.string.template_price_one_equals,
                item.baseAssetCode, formattedPrice)

        InfoCard(cards_layout)
                .setHeading(R.string.price, null)
                .addRow(priceString, null)
    }

    protected open fun displayDate(item: OfferRecord) {
        InfoCard(cards_layout)
                .setHeading(R.string.date, null)
                .addRow(DateFormatter(this).formatLong(item.date), null)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.offer_details, menu)

        val cancelOption = menu?.findItem(R.id.cancel_offer)
        cancelOption?.isVisible = item.isCancellable

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.cancel_offer -> confirmOfferCancellation()
        }
        return super.onOptionsItemSelected(item)
    }

    // region Cancel
    private fun confirmOfferCancellation() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setMessage(getOfferCancellationMessage())
                .setPositiveButton(R.string.yes) { _, _ ->
                    cancelOffer()
                }
                .setNegativeButton(R.string.no, null)
                .show()
    }

    protected open fun getOfferCancellationMessage(): String {
        return getString(R.string.cancel_offer_confirmation)
    }

    protected open fun getOfferCanceledMessage(): String {
        return getString(R.string.offer_canceled)
    }

    private fun cancelOffer() {
        val progress = ProgressDialogFactory.getTunedDialog(this)

        CancelOfferUseCase(
                item,
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
                            toastManager.short(getOfferCanceledMessage())
                            finishWithSuccess()
                        },
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )
    }
    // endregion

    private fun finishWithSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    companion object {
        const val OFFER_EXTRA = "offer"
    }
}