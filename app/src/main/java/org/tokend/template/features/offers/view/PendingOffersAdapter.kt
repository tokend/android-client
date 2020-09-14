package org.tokend.template.features.offers.view

import android.view.View
import android.view.ViewGroup
import org.jetbrains.anko.layoutInflater
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.adapter.base.PaginationRecyclerAdapter
import org.tokend.template.view.history.HistoryItemView
import org.tokend.template.view.util.formatter.AmountFormatter
import org.tokend.template.view.util.formatter.DateFormatters

class PendingOffersAdapter(private val amountFormatter: AmountFormatter,
                           private val dateFormatter: DateFormatters) :
        PaginationRecyclerAdapter<PendingOfferListItem, BaseViewHolder<PendingOfferListItem>>() {
    private class FooterViewHolder(view: View) : BaseViewHolder<PendingOfferListItem>(view) {
        override fun bind(item: PendingOfferListItem) {}
    }

    override fun createFooterViewHolder(parent: ViewGroup): BaseViewHolder<PendingOfferListItem> {
        val view = parent.context
                .layoutInflater.inflate(R.layout.list_item_loading_footer, parent, false)
        return FooterViewHolder(view)
    }

    override fun createItemViewHolder(parent: ViewGroup): PendingOfferItemViewHolder {
        val view = parent.context
                .layoutInflater.inflate(R.layout.list_item_tx, parent, false)
        return PendingOfferItemViewHolder(view, amountFormatter, dateFormatter)
    }

    override fun bindItemViewHolder(holder: BaseViewHolder<PendingOfferListItem>, position: Int) {
        super.bindItemViewHolder(holder, position)
        val isLastInSection =
                position == itemCount - (if (needLoadingFooter) 2 else 1)
        (holder as? HistoryItemView)?.dividerIsVisible = !isLastInSection
    }

    override fun bindFooterViewHolder(holder: BaseViewHolder<PendingOfferListItem>) {}
}