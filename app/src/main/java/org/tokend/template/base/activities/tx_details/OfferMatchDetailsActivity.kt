package org.tokend.template.base.activities.tx_details

import android.app.Activity
import android.app.ProgressDialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.sdk.api.models.Offer
import org.tokend.sdk.api.models.transactions.MatchTransaction
import org.tokend.sdk.api.models.transactions.TransactionState
import org.tokend.template.R
import org.tokend.template.base.logic.transactions.TxManager
import org.tokend.template.base.view.InfoCard
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.ToastManager
import org.tokend.template.util.error_handlers.ErrorHandlerFactory

class OfferMatchDetailsActivity : TxDetailsActivity<MatchTransaction>() {
    private var isPending = false
    private lateinit var tx: MatchTransaction

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_details)
        setTitle(R.string.offer_match_details_title)
    }

    // region Details
    override fun displayDetails(item: MatchTransaction) {
        tx = item
        isPending = item.state == TransactionState.PENDING

        if (isPending) {
            setTitle(R.string.pending_offer_details_title)
        }

        invalidateOptionsMenu()

        displayStateIfNeeded(item, cards_layout)
        displayPaid(item)
        displayReceived(item)
        displayDate(item, cards_layout)
    }

    private fun displayPaid(tx: MatchTransaction) {
        val paidAsset = if (tx.isReceived) tx.matchData.quoteAsset else tx.asset
        val receivedAsset = if (tx.isReceived) tx.asset else tx.matchData.quoteAsset
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
                            getString(R.string.template_price_one_for, receivedAsset,
                                    AmountFormatter.formatAssetAmount(tx.matchData.price), paidAsset))
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

    private fun displayReceived(tx: MatchTransaction) {
        val receivedAsset = if (tx.isReceived) tx.asset else tx.matchData.quoteAsset
        val paidAsset = if (tx.isReceived) tx.matchData.quoteAsset else tx.asset
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
                            getString(R.string.template_price_one_for, receivedAsset,
                                    AmountFormatter.formatAssetAmount(tx.matchData.price), paidAsset))
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
                .setMessage(R.string.cancel_offer_confirmation)
                .setPositiveButton(R.string.yes) { _, _ ->
                    cancelOffer()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun cancelOffer() {
        val progress = ProgressDialog(this)
        progress.isIndeterminate = true
        progress.setMessage(getString(R.string.processing_progress))
        progress.setCancelable(false)

        val offer = getOfferToCancel()

        repositoryProvider.offers()
                .cancel(accountProvider,
                        repositoryProvider.systemInfo(),
                        TxManager(apiProvider),
                        offer)
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .bindUntilEvent(lifecycle(), ActivityEvent.DESTROY)
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
                            ToastManager.short(R.string.offer_canceled)
                            finishWithSuccess()
                        },
                        onError = { ErrorHandlerFactory.getDefault().handle(it) }
                )
    }

    private fun getOfferToCancel(): Offer {
        return Offer(
                id = tx.id.toLongOrNull() ?: 0L,
                isBuy = tx.matchData.isBuy,
                baseAsset = tx.asset,
                quoteAsset = tx.matchData.quoteAsset
        )
    }
    // endregion

    private fun finishWithSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }
}