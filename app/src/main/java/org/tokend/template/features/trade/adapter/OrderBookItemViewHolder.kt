package org.tokend.template.features.trade.adapter

import android.graphics.Paint
import android.graphics.Typeface
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.jetbrains.anko.find
import org.jetbrains.anko.textColor
import org.tokend.sdk.api.models.Offer
import org.tokend.template.R
import org.tokend.template.base.view.adapter.base.BaseViewHolder
import org.tokend.template.base.view.util.AmountFormatter

class OrderBookItemViewHolder(view: View) : BaseViewHolder<Offer>(view) {
    private val priceTextView = view.find<TextView>(R.id.price_text_view)
    private val volumeTextView = view.find<TextView>(R.id.volume_text_view)
    private val rootLayout = view.find<ViewGroup>(R.id.root_layout)
    private var isBuy = false

    private val textSize: Float by lazy {
        view.context.resources.getDimensionPixelSize(R.dimen.text_size_order_book).toFloat()
    }

    override fun bind(item: Offer) {
        volumeTextView.text = AmountFormatter.formatAssetAmount(item.baseAmount, item.baseAsset)
        priceTextView.text = AmountFormatter.formatAssetAmount(item.price, item.quoteAsset,
                minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)

        isBuy = item.isBuy

        if (isBuy) {
            ViewCompat.setLayoutDirection(rootLayout, ViewCompat.LAYOUT_DIRECTION_LTR);
            priceTextView.textColor =
                    ContextCompat.getColor(view.context, R.color.received)
            volumeTextView.gravity = Gravity.LEFT
            priceTextView.gravity = Gravity.RIGHT
        } else {
            ViewCompat.setLayoutDirection(rootLayout, ViewCompat.LAYOUT_DIRECTION_RTL);
            priceTextView.textColor =
                    ContextCompat.getColor(view.context, R.color.sent)
            volumeTextView.gravity = Gravity.RIGHT
            priceTextView.gravity = Gravity.LEFT
        }
    }

    fun setDecimalPointAlignment(maxBuyAmountWidth: Float, maxSellPriceWidth: Float) {
        if (isBuy) {
            val paddingDelta = maxBuyAmountWidth - measureText(volumeTextView.text?.toString(),
                    textSize,
                    Typeface.DEFAULT)
            volumeTextView.setPadding(paddingDelta.toInt(), 0, 0, 0)
            priceTextView.setPadding(0, 0, 0, 0)
        } else {
            val paddingDelta = maxSellPriceWidth - measureText(priceTextView.text?.toString(),
                    textSize,
                    Typeface.DEFAULT_BOLD)
            priceTextView.setPadding(paddingDelta.toInt(), 0, 0, 0)
            volumeTextView.setPadding(0, 0, 0, 0)
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