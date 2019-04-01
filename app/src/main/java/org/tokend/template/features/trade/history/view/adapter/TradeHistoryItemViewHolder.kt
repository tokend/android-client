package org.tokend.template.features.trade.history.view.adapter

import android.view.View
import kotlinx.android.synthetic.main.list_item_trade_history.view.*
import org.tokend.template.data.model.TradeHistoryRecord
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.util.formatter.AmountFormatter
import org.tokend.template.view.util.formatter.DateFormatter

class TradeHistoryItemViewHolder(view: View,
                                 private val amountFormatter: AmountFormatter
) : BaseViewHolder<TradeHistoryRecord>(view) {

    private val priceText = view.price_text_view
    private val amountText = view.amount_text_view
    private val timeText = view.time_text_view

    override fun bind(item: TradeHistoryRecord) {
        priceText.text = amountFormatter.formatAssetAmount(item.price,
                item.quoteAsset, withAssetCode = false, abbreviation = true)

        amountText.text = amountFormatter.formatAssetAmount(item.baseAmount,
                item.baseAsset, withAssetCode = false, abbreviation = true)

        timeText.text = DateFormatter(view.context).formatTimeOrDate(item.createdAt)
    }
}