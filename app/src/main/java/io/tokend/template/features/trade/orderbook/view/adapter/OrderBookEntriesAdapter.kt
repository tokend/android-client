package io.tokend.template.features.trade.orderbook.view.adapter

import android.view.ViewGroup
import io.tokend.template.R
import io.tokend.template.extensions.layoutInflater
import io.tokend.template.view.adapter.base.BaseRecyclerAdapter
import io.tokend.template.view.util.formatter.AmountFormatter

class OrderBookEntriesAdapter(
    private val isBuy: Boolean,
    private val amountFormatter: AmountFormatter
) : BaseRecyclerAdapter<OrderBookEntryListItem, OrderBookEntryViewHolder>() {
    override fun createItemViewHolder(parent: ViewGroup): OrderBookEntryViewHolder {
        val view = when (isBuy) {
            true -> parent.context
                .layoutInflater.inflate(R.layout.list_item_order_book_buy, parent, false)
            false -> parent.context
                .layoutInflater.inflate(R.layout.list_item_order_book_sell, parent, false)
        }

        return OrderBookEntryViewHolder(view, amountFormatter)
    }
}