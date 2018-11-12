package org.tokend.template.base.activities.tx_details

import android.app.Activity
import android.app.ProgressDialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.sdk.api.base.model.operations.OfferMatchOperation
import org.tokend.sdk.api.base.model.operations.OperationState
import org.tokend.sdk.api.base.model.operations.OperationType
import org.tokend.sdk.api.trades.model.Offer
import org.tokend.template.R
import org.tokend.template.base.logic.transactions.TxManager
import org.tokend.template.base.view.InfoCard
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.ToastManager
import kotlin.reflect.KClass

open class OfferMatchDetailsActivity(
        clazz: KClass<out OfferMatchOperation> = OfferMatchOperation::class
) : TxDetailsActivity<OfferMatchOperation>(clazz) {
    protected var isPending = false
    protected lateinit var tx: OfferMatchOperation
    protected var isPrimaryMarket = false

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_details)
        setTitle(R.string.offer_match_details_title)
    }

    // region Details
    override fun displayDetails(item: OfferMatchOperation) {
        tx = item
        isPending = item.state == OperationState.PENDING
        isPrimaryMarket = item.type == OperationType.INVESTMENT

        if (isPending) {
            setTitle(R.string.pending_offer_details_title)
        }

        invalidateOptionsMenu()

        displayStateIfNeeded(item, cards_layout)
        displayPaid(item)
        displayReceived(item)
        displayDate(item, cards_layout)
    }

    protected open fun displayPaid(tx: OfferMatchOperation) {
        val paidAsset = if (tx.isReceived) tx.matchData.quoteAsset else tx.asset
        val amount =
                if (tx.isReceived) tx.matchData.quoteAmount else tx.amount
        val paidAmount =
                if (tx.feeAsset == paidAsset) amount + tx.fee else amount

        val headingRes =
                if (!isPending)
                    R.string.paid
                else
                    R.string.to_pay

        if (tx.feeAsset != paidAsset) {
            InfoCard(cards_layout)
                    .setHeading(headingRes, "${AmountFormatter.formatAssetAmount(paidAmount)} " +
                            paidAsset)
                    .addRow(R.string.price,
                            getString(R.string.template_price_one_equals, tx.asset,
                                    AmountFormatter.formatAssetAmount(tx.matchData.price), tx.matchData.quoteAsset))
        } else {
            InfoCard(cards_layout)
                    .setHeading(headingRes, "${AmountFormatter.formatAssetAmount(paidAmount)} " +
                            paidAsset)
                    .addRow(R.string.amount,
                            "+${AmountFormatter.formatAssetAmount(amount,
                                    paidAsset, minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                            } $paidAsset")
                    .addRow(R.string.tx_fee,
                            "+${AmountFormatter.formatAssetAmount(tx.fee,
                                    paidAsset, minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                            } $paidAsset")
        }
    }

    protected open fun displayReceived(tx: OfferMatchOperation) {
        val receivedAsset = if (tx.isReceived) tx.asset else tx.matchData.quoteAsset
        val amount =
                if (tx.isReceived) tx.amount else tx.matchData.quoteAmount
        val receivedAmount =
                if (tx.feeAsset == receivedAsset) amount - tx.fee else amount

        val headingRes =
                if (!isPending)
                    R.string.received
                else
                    R.string.to_receive

        if (tx.feeAsset == receivedAsset) {
            InfoCard(cards_layout)
                    .setHeading(headingRes,
                            "${AmountFormatter.formatAssetAmount(receivedAmount, receivedAsset)
                            } $receivedAsset")
                    .addRow(R.string.amount,
                            "+${AmountFormatter.formatAssetAmount(amount,
                                    receivedAsset, minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                            } $receivedAsset")
                    .addRow(R.string.tx_fee,
                            "-${AmountFormatter.formatAssetAmount(tx.fee,
                                    receivedAsset, minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                            } $receivedAsset")
        } else {
            InfoCard(cards_layout)
                    .setHeading(headingRes, "${AmountFormatter.formatAssetAmount(receivedAmount, receivedAsset)
                    } $receivedAsset")
                    .addRow(R.string.price,
                            getString(R.string.template_price_one_equals, tx.asset,
                                    AmountFormatter.formatAssetAmount(tx.matchData.price), tx.matchData.quoteAsset))
        }
    }
    // endregion

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.offer_details, menu)
        menu?.findItem(R.id.cancel_offer)?.isVisible = isPending
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
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    protected open fun getOfferCancellationMessage(): String {
        return getString(R.string.cancel_offer_confirmation)
    }

    protected open fun getOfferCanceledMessage(): String {
        return getString(R.string.offer_canceled)
    }

    private fun cancelOffer() {
        val progress = ProgressDialog(this)
        progress.isIndeterminate = true
        progress.setMessage(getString(R.string.processing_progress))
        progress.setCancelable(false)

        val offer = getOfferToCancel()

        CancelOfferUseCase(
                offer,
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
                            ToastManager(this).short(getOfferCanceledMessage())
                            finishWithSuccess()
                        },
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )
    }

    protected open fun getOfferToCancel(): Offer {
        val balances = repositoryProvider.balances().itemsSubject.value

        return Offer(
                id = tx.id.toLongOrNull() ?: 0L,
                orderBookId = tx.matchData.orderBookId ?: 0L,
                isBuy = tx.matchData.isBuy,
                baseAsset = tx.asset,
                quoteAsset = tx.matchData.quoteAsset,
                quoteBalance = balances.find { it.asset == tx.matchData.quoteAsset }?.balanceId,
                baseBalance = balances.find { it.asset == tx.asset }?.balanceId
        )
    }
    // endregion

    private fun finishWithSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }
}