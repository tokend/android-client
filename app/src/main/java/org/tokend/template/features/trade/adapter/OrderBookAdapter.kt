package org.tokend.template.features.trade.adapter

import android.view.View
import android.view.ViewGroup
import org.jetbrains.anko.dimen
import org.jetbrains.anko.find
import org.jetbrains.anko.layoutInflater
import org.tokend.sdk.api.models.Offer
import org.tokend.template.R
import org.tokend.template.base.view.adapter.base.BaseViewHolder
import org.tokend.template.base.view.adapter.base.PaginationRecyclerAdapter

class OrderBookAdapter : PaginationRecyclerAdapter<Offer, BaseViewHolder<Offer>>() {
    class FooterViewHolder(v: View) : BaseViewHolder<Offer>(v) {
        override fun bind(item: Offer) { }
        fun bind() {
            view.find<View>(R.id.loading_footer_progress).apply {
                layoutParams = layoutParams.apply {
                    width = view.context.dimen(R.dimen.standard_padding)
                    height = view.context.dimen(R.dimen.standard_padding)
                }
            }
        }
    }

    override fun createItemViewHolder(parent: ViewGroup): BaseViewHolder<Offer> {
        val view = parent.context
                .layoutInflater.inflate(R.layout.list_item_order_book, parent, false)
        return OrderBookItemViewHolder(view)
    }

    override fun createFooterViewHolder(parent: ViewGroup): BaseViewHolder<Offer> {
        val view = parent.context
                .layoutInflater.inflate(R.layout.list_item_loading_footer, parent, false)
        return FooterViewHolder(view)
    }

    override fun bindFooterViewHolder(holder: BaseViewHolder<Offer>) {
        (holder as? FooterViewHolder)?.bind()
    }

    override fun bindItemViewHolder(holder: BaseViewHolder<Offer>, position: Int) {
        super.bindItemViewHolder(holder, position)
//        (holder as? OrderBookItemViewHolder)?.setDecimalPointAlignment()
    }
}