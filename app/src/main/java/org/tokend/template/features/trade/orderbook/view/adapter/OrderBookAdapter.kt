package org.tokend.template.features.trade.orderbook.view.adapter

import android.view.View
import android.view.ViewGroup
import org.jetbrains.anko.dimen
import org.jetbrains.anko.find
import org.jetbrains.anko.layoutInflater
import org.tokend.template.R
import org.tokend.template.data.model.OfferRecord
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.adapter.base.PaginationRecyclerAdapter
import org.tokend.template.view.util.formatter.AmountFormatter

class OrderBookAdapter(val isBuy: Boolean) : PaginationRecyclerAdapter<OfferRecord, BaseViewHolder<OfferRecord>>() {

    lateinit var amountFormatter: AmountFormatter

    class FooterViewHolder(v: View) : BaseViewHolder<OfferRecord>(v) {
        override fun bind(item: OfferRecord) {}
        fun bind() {
            view.find<View>(R.id.loading_footer_progress).apply {
                layoutParams = layoutParams.apply {
                    width = view.context.dimen(R.dimen.standard_padding)
                    height = view.context.dimen(R.dimen.standard_padding)
                }
            }
        }
    }

    override fun createItemViewHolder(parent: ViewGroup): BaseViewHolder<OfferRecord> {
        val view = when (isBuy) {
            true -> parent.context
                    .layoutInflater.inflate(R.layout.list_item_order_book_buy, parent, false)
            false -> parent.context
                    .layoutInflater.inflate(R.layout.list_item_order_book_sell, parent, false)
        }

        return OrderBookItemViewHolder(view, amountFormatter)
    }

    override fun createFooterViewHolder(parent: ViewGroup): BaseViewHolder<OfferRecord> {
        val view = parent.context
                .layoutInflater.inflate(R.layout.list_item_loading_footer, parent, false)
        return FooterViewHolder(view)
    }

    override fun bindFooterViewHolder(holder: BaseViewHolder<OfferRecord>) {
        (holder as? FooterViewHolder)?.bind()
    }
}