package org.tokend.template.features.wallet.adapter

import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatImageView
import android.view.View
import android.widget.TextView
import org.jetbrains.anko.find
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChangeAction
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.util.formatter.AmountFormatter

class BalanceChangeItemViewHolder(view: View,
                                  private val amountFormatter: AmountFormatter,
                                  private val smallIcon: Boolean
) : BaseViewHolder<BalanceChangeListItem>(view) {
    private val iconImageView: AppCompatImageView = view.find(R.id.tx_icon_image_view)
    private val actionTextView: TextView = view.find(R.id.tx_action_text_view)
    private val counterpartyTextView: TextView = view.find(R.id.tx_counterparty_text_view)
    private val amountTextView: TextView = view.find(R.id.tx_amount_text_view)
    private val extraInfoTextView: TextView = view.find(R.id.tx_below_amount_text_view)
    private val dividerView: View = view.find(R.id.divider_view)

    private val incomingIcon: Drawable? by lazy {
        ContextCompat.getDrawable(view.context, R.drawable.ic_tx_received)
    }
    private val outgoingIcon: Drawable? by lazy {
        ContextCompat.getDrawable(view.context, R.drawable.ic_tx_sent)
    }
    private val matchIcon: Drawable? by lazy {
        ContextCompat.getDrawable(view.context, R.drawable.ic_tx_match)
    }
    private val lockedIcon: Drawable? by lazy {
        ContextCompat.getDrawable(view.context, R.drawable.ic_tx_locked)
    }
    private val unlockedIcon: Drawable? by lazy {
        ContextCompat.getDrawable(view.context, R.drawable.ic_tx_unlocked)
    }
    private val receivedColor: Int by lazy {
        ContextCompat.getColor(view.context, R.color.received)
    }
    private val sentColor: Int by lazy {
        ContextCompat.getColor(view.context, R.color.sent)
    }
    private val secondaryTextColor: Int by lazy {
        ContextCompat.getColor(view.context, R.color.secondary_text)
    }
    private val iconSize: Int by lazy {
        view.context.resources.getDimensionPixelSize(R.dimen.tx_list_item_icon_size)
    }
    private val iconSizeSmall: Int by lazy {
        view.context.resources.getDimensionPixelSize(R.dimen.tx_list_item_icon_size_small)
    }

    init {
        if (smallIcon) {
            iconImageView.layoutParams.height = iconSizeSmall
            iconImageView.layoutParams.width = iconSizeSmall
        } else {
            iconImageView.layoutParams.height = iconSize
            iconImageView.layoutParams.width = iconSize
        }
    }

    var dividerIsVisible: Boolean = true
        set(value) {
            field = value
            dividerView.visibility =
                    if (field)
                        View.VISIBLE
                    else
                        View.GONE
        }

    override fun bind(item: BalanceChangeListItem) {
        displayIcon(item)
        displayActionName(item)
        displayCounterparty(item)
        displayAmount(item)
        displayExtraInfo(item)
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
            BalanceChangeAction.WITHDRAWN -> R.string.tx_action_withdrawal
            BalanceChangeAction.MATCHED -> R.string.tx_action_matched
            BalanceChangeAction.ISSUED -> R.string.tx_action_deposit
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
                    receivedColor
                else
                    sentColor

        amountTextView.setTextColor(color)

        var formattedAmount = amountFormatter.formatAssetAmount(
                item.amount, item.assetCode, abbreviation = true
        )

        if (!item.isReceived) {
            formattedAmount = "-$formattedAmount"
        }

        amountTextView.text = formattedAmount
    }

    private fun displayExtraInfo(item: BalanceChangeListItem) {
        extraInfoTextView.visibility = View.GONE
    }
}