package org.tokend.template.features.wallet.adapter

import android.view.View
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChangeAction
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.history.HistoryItemView
import org.tokend.template.view.history.HistoryItemViewImpl
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
            BalanceChangeAction.LOCKED -> lockedIcon
            BalanceChangeAction.UNLOCKED -> unlockedIcon
            BalanceChangeAction.MATCHED -> matchIcon
            else -> if (item.isReceived) incomingIcon else outgoingIcon
        }

        iconImageView.setImageDrawable(icon)
    }

    private fun displayActionName(item: BalanceChangeListItem) {
        val stringRes = when (item.action) {
            BalanceChangeAction.LOCKED -> R.string.tx_action_locked
            BalanceChangeAction.CHARGED_FROM_LOCKED -> R.string.tx_action_charged
            BalanceChangeAction.UNLOCKED -> R.string.tx_action_unlocked
            BalanceChangeAction.CHARGED -> R.string.tx_action_charged
            BalanceChangeAction.WITHDRAWN -> R.string.tx_action_withdrawn
            BalanceChangeAction.MATCHED -> R.string.tx_action_matched
            BalanceChangeAction.ISSUED -> R.string.tx_action_issued
            BalanceChangeAction.FUNDED -> R.string.tx_action_received
        }

        actionTextView.setText(stringRes)
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
                if (item.action == BalanceChangeAction.LOCKED)
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