package org.tokend.template.features.wallet.adapter

import android.view.View
import org.tokend.template.R
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
    override fun bind(item: BalanceChangeListItem) {
        displayIcon(item)
        displayActionName(item)
        displayCounterparty(item)
        displayAmount(item)
        displayExtraInfo()
    }

    private fun displayIcon(item: BalanceChangeListItem) {
        val icon = when (item.action) {
            BalanceChangeListItem.Action.LOCKED -> lockedIcon
            BalanceChangeListItem.Action.UNLOCKED -> unlockedIcon
            BalanceChangeListItem.Action.MATCHED -> matchIcon
            else -> if (item.isReceived) incomingIcon else outgoingIcon
        }

        iconImageView.setImageDrawable(icon)
    }

    private fun displayActionName(item: BalanceChangeListItem) {
        actionTextView.text =
                LocalizedName(view.context).forBalanceChangeListItemAction(item.action)
    }

    private fun displayCounterparty(item: BalanceChangeListItem) {
        val counterparty = item.counterparty

        if (counterparty == null) {
            counterpartyTextView.visibility = View.GONE
        } else {
            counterpartyTextView.visibility = View.VISIBLE
            counterpartyTextView.text =
                    if (item.isReceived)
                        view.context.getString(R.string.template_tx_from, counterparty)
                    else
                        view.context.getString(R.string.template_tx_to, counterparty)
        }
    }

    private fun displayAmount(item: BalanceChangeListItem) {
        val color =
                if (item.action == BalanceChangeListItem.Action.LOCKED)
                    secondaryTextColor
                else if (item.isReceived)
                    incomingColor
                else
                    outgoingColor

        amountTextView.setTextColor(color)

        var formattedAmount = amountFormatter.formatAssetAmount(
                item.amount, item.assetCode, abbreviation = true
        )

        if (!item.isReceived) {
            formattedAmount = "-$formattedAmount"
        }

        amountTextView.text = formattedAmount
    }

    private fun displayExtraInfo() {
        extraInfoTextView.visibility = View.GONE
    }
}