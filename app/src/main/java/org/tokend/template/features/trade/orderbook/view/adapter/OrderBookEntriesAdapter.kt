package org.tokend.template.features.trade.orderbook.view.adapter

import android.view.ViewGroup
import org.jetbrains.anko.layoutInflater
import org.tokend.template.R
import org.tokend.template.features.trade.orderbook.model.OrderBookEntryRecord
import org.tokend.template.view.adapter.base.BaseRecyclerAdapter
import org.tokend.template.view.util.formatter.AmountFormatter

class OrderBookEntriesAdapter(
        private val isBuy: Boolean,
        private val amountFormatter: AmountFormatter
) : BaseRecyclerAdapter<OrderBookEntryRecord, OrderBookEntryViewHolder>() {
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