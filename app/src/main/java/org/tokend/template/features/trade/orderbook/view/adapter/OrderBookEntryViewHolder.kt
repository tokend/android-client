package org.tokend.template.features.trade.orderbook.view.adapter

import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.TextView
import org.jetbrains.anko.find
import org.jetbrains.anko.textColor
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.util.formatter.AmountFormatter

class OrderBookEntryViewHolder(view: View,
                               private val amountFormatter: AmountFormatter
) : BaseViewHolder<OrderBookEntryListItem>(view) {
    private val priceTextView = view.find<TextView>(R.id.price_text_view)
    private val volumeTextView = view.find<TextView>(R.id.volume_text_view)

    override fun bind(item: OrderBookEntryListItem) {
        volumeTextView.text = amountFormatter.formatAssetAmount(item.volume, item.baseAsset,
                withAssetCode = false)
        priceTextView.text = amountFormatter.formatAssetAmount(item.price, item.quoteAsset,
                withAssetCode = false)

        volumeTextView.background.level = 100 * item.volumePercentsOfMax

        if (item.isBuy) {
            priceTextView.textColor =
                    ContextCompat.getColor(view.context, R.color.received)
        } else {
            priceTextView.textColor =
                    ContextCompat.getColor(view.context, R.color.sent)
        }
    }
}