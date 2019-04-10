package org.tokend.template.features.wallet.adapter

import android.view.View
import org.tokend.template.R
import org.tokend.template.features.wallet.view.BalanceChangeIconFactory
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.history.HistoryItemView
import org.tokend.template.view.history.HistoryItemViewImpl
import org.tokend.template.view.util.LocalizedName
import org.tokend.template.view.util.formatter.AmountFormatter

class BalanceChangeItemViewHolder(view: View,
                                  private val amountFormatter: AmountFormatter,
                                  smallIcon: Boolean
) : BaseViewHolder<BalanceChangeListItem>(view),
        HistoryItemView by HistoryItemViewImpl(view, smallIcon) {

    private val iconFactory = BalanceChangeIconFactory(view.context)

    override fun bind(item: BalanceChangeListItem) {
        displayIcon(item)
        displayActionName(item)
        displayCounterpartyOrCause(item)
        displayAmount(item)
        displayExtraInfo()
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
                    item.action == BalanceChangeListItem.Action.LOCKED -> secondaryTextColor
                    item.isReceived == true -> incomingColor
                    item.isReceived == false -> outgoingColor
                    else -> secondaryTextColor
                }

        amountTextView.setTextColor(color)

        var formattedAmount = amountFormatter.formatAssetAmount(
                item.amount, item.assetCode, abbreviation = true
        )

        if (item.isReceived == false) {
            formattedAmount = "-$formattedAmount"
        }

        amountTextView.text = formattedAmount
    }

    private fun displayExtraInfo() {
        extraInfoTextView.visibility = View.GONE
    }
}