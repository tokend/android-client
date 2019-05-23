package org.tokend.template.features.wallet.adapter

import android.view.View
import org.tokend.template.R
import org.tokend.template.features.wallet.view.BalanceChangeIconFactory
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.history.HistoryItemView
import org.tokend.template.view.history.HistoryItemViewImpl
import org.tokend.template.view.util.LocalizedName
import org.tokend.template.view.util.formatter.AmountFormatter
import org.tokend.template.view.util.formatter.DateFormatter

class BalanceChangeItemViewHolder(view: View,
                                  private val amountFormatter: AmountFormatter,
                                  smallIcon: Boolean
) : BaseViewHolder<BalanceChangeListItem>(view),
        HistoryItemView by HistoryItemViewImpl(view, smallIcon) {

    private val iconFactory = BalanceChangeIconFactory(view.context)
    private val dateFormatter = DateFormatter(view.context)

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
                val formatted = try {
                    "${counterparty.substring(0..3)}…" +
                            counterparty.substring(counterparty.length - 4 until counterparty.length)
                } catch (e: StringIndexOutOfBoundsException) {
                    counterparty
                }

                actionDetailsTextView.text =
                        if (item.isReceived)
                            view.context.getString(R.string.template_tx_from, formatted)
                        else
                            view.context.getString(R.string.template_tx_to, formatted)
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

    private fun displayExtraInfo(item: BalanceChangeListItem) {
        extraInfoTextView.visibility = View.VISIBLE
        extraInfoTextView.text = dateFormatter.formatTimeOrDate(item.date)
    }
}