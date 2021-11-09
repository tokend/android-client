package io.tokend.template.features.trade.history.view.adapter

import android.view.View
import androidx.core.content.ContextCompat
import io.tokend.template.R
import io.tokend.template.features.trade.history.model.TradeHistoryRecord
import io.tokend.template.view.adapter.base.BaseViewHolder
import io.tokend.template.view.util.formatter.AmountFormatter
import kotlinx.android.synthetic.main.list_item_trade_history.view.*
import java.text.DateFormat

class TradeHistoryItemViewHolder(
    view: View,
    private val amountFormatter: AmountFormatter,
    private val dateFormat: DateFormat
) : BaseViewHolder<TradeHistoryRecord>(view) {
    private val positiveColor: Int by lazy {
        ContextCompat.getColor(view.context, R.color.received)
    }

    private val negativeColor: Int by lazy {
        ContextCompat.getColor(view.context, R.color.sent)
    }

    private val priceText = view.price_text_view
    private val amountText = view.amount_text_view
    private val timeText = view.time_text_view

    override fun bind(item: TradeHistoryRecord) {
        priceText.text = amountFormatter.formatAssetAmount(
            item.price,
            item.quoteAsset, withAssetCode = false, abbreviation = false
        )

        amountText.text = amountFormatter.formatAssetAmount(
            item.baseAmount,
            item.baseAsset, withAssetCode = false, abbreviation = true
        )

        timeText.text = dateFormat.format(item.createdAt)

        priceText.setTextColor(
            if (item.hasPositiveTrend)
                positiveColor
            else
                negativeColor
        )
    }
}