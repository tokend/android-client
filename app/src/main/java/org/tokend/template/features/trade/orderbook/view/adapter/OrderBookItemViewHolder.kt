package org.tokend.template.features.trade.orderbook.view.adapter

import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.TextView
import org.jetbrains.anko.find
import org.jetbrains.anko.textColor
import org.tokend.template.R
import org.tokend.template.features.offers.model.OfferRecord
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.util.formatter.AmountFormatter

class OrderBookItemViewHolder(view: View,
                              private val amountFormatter: AmountFormatter
) : BaseViewHolder<OfferRecord>(view) {
    private val priceTextView = view.find<TextView>(R.id.price_text_view)
    private val volumeTextView = view.find<TextView>(R.id.volume_text_view)

    private var isBuy = false

    override fun bind(item: OfferRecord) {
        isBuy = item.isBuy

        volumeTextView.text = amountFormatter.formatAssetAmount(item.baseAmount, item.baseAssetCode,
                withAssetCode = false)
        priceTextView.text = amountFormatter.formatAssetAmount(item.price, item.quoteAssetCode,
                amountFormatter.getDecimalDigitsCount(item.quoteAssetCode),
                withAssetCode = false)
        if (isBuy) {
            priceTextView.textColor =
                    ContextCompat.getColor(view.context, R.color.received)
        } else {
            priceTextView.textColor =
                    ContextCompat.getColor(view.context, R.color.sent)
        }
    }
}