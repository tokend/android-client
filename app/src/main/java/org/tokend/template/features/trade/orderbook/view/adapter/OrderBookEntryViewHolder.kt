package org.tokend.template.features.trade.orderbook.view.adapter

import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.TextView
import org.jetbrains.anko.find
import org.jetbrains.anko.textColor
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.R
import org.tokend.template.features.offers.model.OfferRecord
import org.tokend.template.features.trade.orderbook.model.OrderBookEntryRecord
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.adapter.base.SimpleItemClickListener
import org.tokend.template.view.util.formatter.AmountFormatter
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.roundToInt

class OrderBookEntryViewHolder(view: View,
                               private val amountFormatter: AmountFormatter
) : BaseViewHolder<OrderBookEntryListItem>(view) {
    private val priceTextView = view.find<TextView>(R.id.price_text_view)
    private val volumeTextView = view.find<TextView>(R.id.volume_text_view)

    private var isBuy = false

    fun bind(item: OrderBookEntryListItem, clickListener: SimpleItemClickListener<OrderBookEntryListItem>?, maxVolume: BigDecimal) {
        super.bind(item, clickListener)

        val factor =
                (item.volume * BigDecimal(100)).divide(maxVolume, MathContext.DECIMAL128).toInt()

        volumeTextView.background.level = 100 * factor
    }

    override fun bind(item: OrderBookEntryListItem) {
        isBuy = item.isBuy

        volumeTextView.text = amountFormatter.formatAssetAmount(item.volume, item.baseAssetCode,
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