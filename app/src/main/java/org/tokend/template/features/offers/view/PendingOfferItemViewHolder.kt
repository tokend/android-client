package org.tokend.template.features.offers.view

import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.view.View
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.history.HistoryItemView
import org.tokend.template.view.history.HistoryItemViewImpl
import org.tokend.template.view.util.formatter.AmountFormatter
import org.tokend.template.view.util.formatter.DateFormatter

class PendingOfferItemViewHolder(
        view: View,
        private val amountFormatter: AmountFormatter,
        smallIcon: Boolean
) : BaseViewHolder<PendingOfferListItem>(view),
        HistoryItemView by HistoryItemViewImpl(view, smallIcon) {

    private val incomingIcon: Drawable? =
            ContextCompat.getDrawable(view.context, R.drawable.ic_tx_received)

    private val outgoingIcon: Drawable? =
            ContextCompat.getDrawable(view.context, R.drawable.ic_tx_sent)

    private val dateFormatter = DateFormatter(view.context)

    override fun bind(item: PendingOfferListItem) {
        displayIcon(item)
        displayAction(item)
        displayCounterparty(item)
        displayAmount(item)
        displayExtraInfo(item)
    }

    private fun displayIcon(item: PendingOfferListItem) {
        val icon = if (item.isInvestment || !item.isBuy) outgoingIcon else incomingIcon
        iconImageView.setImageDrawable(icon)
    }

    private fun displayAction(item: PendingOfferListItem) {
        val actionStringRes =
                when {
                    item.isInvestment -> R.string.tx_action_investment
                    item.isBuy -> R.string.buy
                    else -> R.string.sell
                }
        actionTextView.setText(actionStringRes)
    }

    private fun displayCounterparty(item: PendingOfferListItem) {
        val counterpartyString =
                if (item.isInvestment)
                    view.context.getString(R.string.template_tx_in, item.counterpartyAssetCode)
                else
                    view.context.getString(
                            R.string.template_price_one_equals,
                            item.assetCode,
                            amountFormatter.formatAssetAmount(item.price, item.counterpartyAssetCode))

        actionDetailsTextView.visibility = View.VISIBLE
        actionDetailsTextView.text = counterpartyString

    }

    private fun displayAmount(item: PendingOfferListItem) {
        var amountString = amountFormatter.formatAssetAmount(item.amount, item.assetCode)
        var amountColor = incomingColor

        if (item.isInvestment || !item.isBuy) {
            amountString = "-$amountString"
            amountColor = outgoingColor
        }

        amountTextView.text = amountString
        amountTextView.setTextColor(amountColor)
    }

    private fun displayExtraInfo(item: PendingOfferListItem) {
        extraInfoTextView.visibility = View.VISIBLE
        extraInfoTextView.text = dateFormatter.formatTimeOrDate(item.date)
    }
}