package io.tokend.template.features.trade.orderbook.view.adapter

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.tokend.template.R
import io.tokend.template.view.adapter.base.BaseViewHolder
import io.tokend.template.view.util.formatter.AmountFormatter

class OrderBookEntryViewHolder(
    view: View,
    private val amountFormatter: AmountFormatter
) : BaseViewHolder<OrderBookEntryListItem>(view) {
    private val priceTextView = view.findViewById<TextView>(R.id.price_text_view)
    private val volumeTextView = view.findViewById<TextView>(R.id.volume_text_view)

    override fun bind(item: OrderBookEntryListItem) {
        volumeTextView.text = amountFormatter.formatAssetAmount(
            item.volume, item.baseAsset,
            withAssetCode = false
        )
        priceTextView.text = amountFormatter.formatAssetAmount(
            item.price, item.quoteAsset,
            withAssetCode = false
        )

        volumeTextView.background.level = 100 * item.volumePercentsOfMax

        if (item.isBuy) {
            priceTextView.setTextColor(
                ContextCompat.getColor(view.context, R.color.received)
            )
        } else {
            priceTextView.setTextColor(
                ContextCompat.getColor(view.context, R.color.sent)
            )
        }
    }
}