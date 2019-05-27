package org.tokend.template.view.history

import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatImageView
import android.view.View
import android.widget.TextView
import org.jetbrains.anko.find
import org.tokend.template.R

class HistoryItemViewImpl(val view: View,
                          smallIcon: Boolean) : HistoryItemView {
    override val iconImageView: AppCompatImageView = view.find(R.id.tx_icon_image_view)
    override val actionTextView: TextView = view.find(R.id.tx_action_text_view)
    override val actionDetailsTextView: TextView = view.find(R.id.tx_counterparty_text_view)
    override val amountTextView: TextView = view.find(R.id.tx_amount_text_view)
    override val extraInfoTextView: TextView = view.find(R.id.tx_below_amount_text_view)
    override val dividerView: View = view.find(R.id.divider_view)

    override val incomingColor: Int by lazy {
        ContextCompat.getColor(view.context, R.color.received)
    }

    override val defaultAmountColor: Int by lazy {
        ContextCompat.getColor(view.context, R.color.primary_text)
    }

    override val outgoingColor: Int by lazy {
        ContextCompat.getColor(view.context, R.color.sent)
    }

    override val iconSize: Int by lazy {
        view.context.resources.getDimensionPixelSize(R.dimen.tx_list_item_icon_size)
    }

    override val iconSizeSmall: Int by lazy {
        view.context.resources.getDimensionPixelSize(R.dimen.tx_list_item_icon_size_small)
    }

    override var dividerIsVisible: Boolean = true
        set(value) {
            field = value
            dividerView.visibility =
                    if (field)
                        View.VISIBLE
                    else
                        View.GONE
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
}