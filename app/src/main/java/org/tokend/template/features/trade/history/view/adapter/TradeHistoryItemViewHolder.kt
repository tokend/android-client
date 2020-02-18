package org.tokend.template.features.trade.history.view.adapter

import android.support.v4.content.ContextCompat
import android.view.View
import kotlinx.android.synthetic.main.list_item_trade_history.view.*
import org.jetbrains.anko.textColor
import org.tokend.template.R
import org.tokend.template.features.trade.history.model.TradeHistoryRecord
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.util.formatter.AmountFormatter
import org.tokend.template.view.util.formatter.DateFormatter

class TradeHistoryItemViewHolder(view: View,
                                 private val amountFormatter: AmountFormatter
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
        priceText.text = amountFormatter.formatAssetAmount(item.price,
                item.quoteAsset, withAssetCode = false, abbreviation = false)

        amountText.text = amountFormatter.formatAssetAmount(item.baseAmount,
                item.baseAsset, withAssetCode = false, abbreviation = true)

        timeText.text = DateFormatter(view.context).formatTimeOrDate(item.createdAt)

        priceText.textColor =
                if (item.hasPositiveTrend)
                    positiveColor
                else
                    negativeColor
    }
}