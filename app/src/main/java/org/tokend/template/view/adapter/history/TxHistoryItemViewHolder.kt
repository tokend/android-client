package org.tokend.template.view.adapter.history

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatImageView
import android.view.View
import android.widget.TextView
import org.jetbrains.anko.find
import org.jetbrains.anko.textColor
import org.tokend.sdk.api.base.model.operations.OperationState
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.util.LocalizedName
import org.tokend.template.view.util.formatter.AmountFormatter

class TxHistoryItemViewHolder(view: View, private val amountFormatter: AmountFormatter) : BaseViewHolder<TxHistoryItem>(view) {
    private val iconImageView: AppCompatImageView = view.find(R.id.tx_icon_image_view)
    private val actionTextView: TextView = view.find(R.id.tx_action_text_view)
    private val counterpartyTextView: TextView = view.find(R.id.tx_counterparty_text_view)
    private val amountTextView: TextView = view.find(R.id.tx_amount_text_view)
    private val extraInfoTextView: TextView = view.find(R.id.tx_below_amount_text_view)
    private val dividerView: View = view.find(R.id.divider_view)

    private val receivedIcon: Drawable? by lazy {
        ContextCompat.getDrawable(view.context, R.drawable.ic_tx_received)
    }
    private val sentIcon: Drawable? by lazy {
        ContextCompat.getDrawable(view.context, R.drawable.ic_tx_sent)
    }
    private val receivedColor: Int by lazy {
        ContextCompat.getColor(view.context, R.color.received)
    }
    private val sentColor: Int by lazy {
        ContextCompat.getColor(view.context, R.color.sent)
    }
    private val primaryTextColor: Int by lazy {
        ContextCompat.getColor(view.context, R.color.primary_text)
    }
    private val secondaryTextColor: Int by lazy {
        ContextCompat.getColor(view.context, R.color.secondary_text)
    }
    private val errorColor: Int by lazy {
        ContextCompat.getColor(view.context, R.color.error)
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

    var iconIsSmall: Boolean = true
        set(value) {
            field = value
                    if (field) {
                        iconImageView.layoutParams.height = iconImageView.context.resources.getDimension(R.dimen.tx_list_item_icon_size_small).toInt()
                        iconImageView.layoutParams.width = iconImageView.context.resources.getDimension(R.dimen.tx_list_item_icon_size_small).toInt()
                    }

                else {
                        iconImageView.layoutParams.height = iconImageView.context.resources.getDimension(R.dimen.tx_list_item_icon_size).toInt()
                        iconImageView.layoutParams.width = iconImageView.context.resources.getDimension(R.dimen.tx_list_item_icon_size).toInt()                    }
        }


    override fun bind(item: TxHistoryItem) {
        displayIcon(item)
        displayAmount(item)
        displayAction(item)
        displayCounterpartyIfNeeded(item)
        displayStateIfNeeded(item)
    }

    private fun displayIcon(item: TxHistoryItem) {
        iconImageView.setImageDrawable(
                if (item.isReceived)
                    receivedIcon
                else
                    sentIcon
        )
    }

    private fun displayAmount(item: TxHistoryItem) {
        amountTextView.text = amountFormatter.formatAssetAmount(item.amount,
                abbreviation = true) + " ${item.asset}"
        if (!item.isReceived) {
            amountTextView.text = "-" + amountTextView.text
        }

        amountTextView.setTextColor(
                if (item.isReceived)
                    receivedColor
                else
                    sentColor
        )
    }

    private fun displayAction(item: TxHistoryItem) {
        actionTextView.text = LocalizedName(view.context).forTransactionAction(item.action)
    }

    private fun displayCounterpartyIfNeeded(item: TxHistoryItem) {
        if (item.counterparty != null) {
            counterpartyTextView.visibility = View.VISIBLE
            counterpartyTextView.text =
                    when (item.action) {
                        TxHistoryItem.Action.SENT,
                        TxHistoryItem.Action.WITHDRAWAL ->
                            view.context.getString(R.string.template_tx_to, item.counterparty)
                        TxHistoryItem.Action.RECEIVED ->
                            view.context.getString(R.string.template_tx_from, item.counterparty)
                        TxHistoryItem.Action.SOLD,
                        TxHistoryItem.Action.BOUGHT,
                        TxHistoryItem.Action.SELL,
                        TxHistoryItem.Action.BUY ->
                            view.context.getString(R.string.template_tx_for, item.counterparty)
                        TxHistoryItem.Action.INVESTMENT ->
                            view.context.getString(R.string.template_tx_in, item.counterparty)
                        else -> item.counterparty
                    }
        } else {
            counterpartyTextView.visibility = View.GONE
        }
    }

    @SuppressLint("RestrictedApi")
    private fun displayStateIfNeeded(item: TxHistoryItem) {
        val isPendingOffer =
                item.action == TxHistoryItem.Action.BUY
                        || item.action == TxHistoryItem.Action.SELL
                        || (item.action == TxHistoryItem.Action.INVESTMENT
                        && item.state == OperationState.PENDING)

        if (item.state != OperationState.SUCCESS && !isPendingOffer) {
            extraInfoTextView.visibility = View.VISIBLE
            extraInfoTextView.text = LocalizedName(view.context).forTransactionState(item.state)

            if (item.state == OperationState.REJECTED) {
                extraInfoTextView.textColor = errorColor
            } else {
                extraInfoTextView.textColor = secondaryTextColor
            }

            amountTextView.textColor = secondaryTextColor
            iconImageView.supportImageTintList = ColorStateList.valueOf(secondaryTextColor)
            actionTextView.textColor = secondaryTextColor
        } else {
            extraInfoTextView.visibility = View.GONE
            actionTextView.textColor = primaryTextColor
            iconImageView.supportImageTintList = null
        }
    }
}