package org.tokend.template.features.history.view.adapter

import android.view.View
import org.tokend.template.R
import org.tokend.template.features.history.view.BalanceChangeIconFactory
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.history.HistoryItemView
import org.tokend.template.view.history.HistoryItemViewImpl
import org.tokend.template.view.util.LocalizedName
import org.tokend.template.view.util.formatter.AmountFormatter
import java.text.DateFormat

class BalanceChangeItemViewHolder(
        view: View,
        private val amountFormatter: AmountFormatter,
        private val dateFormat: DateFormat
) : BaseViewHolder<BalanceChangeListItem>(view),
        HistoryItemView by HistoryItemViewImpl(view) {

    private val iconFactory = BalanceChangeIconFactory(view.context)

    override fun bind(item: BalanceChangeListItem) {
        displayIcon(item)
        displayActionName(item)
        displayCounterpartyOrCause(item)
        displayAmount(item)
        displayExtraInfo(item)
    }

    private fun displayIcon(item: BalanceChangeListItem) {
        iconImageView.setImageDrawable(iconFactory.get(item.action, item.isReceived))
    }

    private fun displayActionName(item: BalanceChangeListItem) {
        actionTextView.text =
                LocalizedName(view.context).forBalanceChangeListItemAction(item.action)
    }

    private fun displayCounterpartyOrCause(item: BalanceChangeListItem) {
        val counterparty = item.counterparty
        val cause = item.causeName

        if (counterparty != null || cause != null) {
            actionDetailsTextView.visibility = View.VISIBLE

            if (counterparty != null && item.isReceived != null) {
                actionDetailsTextView.text =
                        if (item.isReceived)
                            view.context.getString(R.string.template_tx_from, counterparty)
                        else
                            view.context.getString(R.string.template_tx_to, counterparty)
            } else {
                actionDetailsTextView.text = cause
            }
        } else {
            actionDetailsTextView.visibility = View.GONE
        }
    }

    private fun displayAmount(item: BalanceChangeListItem) {
        val color =
                when {
                    item.isReceived == true -> incomingColor
                    item.isReceived == false -> outgoingColor
                    else -> defaultAmountColor
                }

        amountTextView.setTextColor(color)

        var formattedAmount = amountFormatter.formatAssetAmount(
                item.amount, item.asset, abbreviation = true
        )

        if (item.isReceived == false) {
            formattedAmount = "-$formattedAmount"
        }

        amountTextView.text = formattedAmount
    }

    private fun displayExtraInfo(item: BalanceChangeListItem) {
        extraInfoTextView.visibility = View.VISIBLE
        extraInfoTextView.text = dateFormat.format(item.date)
    }
}