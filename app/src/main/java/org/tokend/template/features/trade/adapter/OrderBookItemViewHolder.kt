package org.tokend.template.features.trade.adapter

import android.graphics.Paint
import android.graphics.Typeface
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.TextView
import org.jetbrains.anko.find
import org.jetbrains.anko.textColor
import org.tokend.sdk.api.trades.model.Offer
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.util.formatter.AmountFormatter

class OrderBookItemViewHolder(view: View) : BaseViewHolder<Offer>(view) {
    private val priceTextView = view.find<TextView>(R.id.price_text_view)
    private val volumeTextView = view.find<TextView>(R.id.volume_text_view)

    private var isBuy = false

    private val textSize: Float by lazy {
        view.context.resources.getDimensionPixelSize(R.dimen.text_size_order_book).toFloat()
    }

    override fun bind(item: Offer) {
        isBuy = item.isBuy

        volumeTextView.text = AmountFormatter.formatAssetAmount(item.baseAmount, item.baseAsset)
        priceTextView.text = AmountFormatter.formatAssetAmount(item.price, item.quoteAsset,
                minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
        if (isBuy) {
            priceTextView!!.textColor =
                    ContextCompat.getColor(view.context, R.color.received)
        } else {
            priceTextView.textColor =
                    ContextCompat.getColor(view.context, R.color.sent)
        }
    }

    companion object {
        fun measureText(text: String?, textSize: Float, typeface: Typeface): Float {
            val paint = Paint()
            paint.isAntiAlias = true
            paint.textSize = textSize
            paint.typeface = typeface

            return paint.measureText(text)
        }
    }
}